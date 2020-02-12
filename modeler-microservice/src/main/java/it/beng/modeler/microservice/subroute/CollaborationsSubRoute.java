package it.beng.modeler.microservice.subroute;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import it.beng.microservice.common.Countdown;
import it.beng.microservice.db.DeleteResult;
import it.beng.modeler.config.cpd;
import it.beng.modeler.microservice.http.JsonResponse;
import it.beng.modeler.microservice.utils.AuthUtils;
import it.beng.modeler.microservice.utils.CommonUtils;
import it.beng.modeler.microservice.utils.DBUtils;
import it.beng.modeler.microservice.utils.ProcessEngineUtils;
import it.beng.modeler.model.Domain;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.flowable.engine.history.HistoricProcessInstance;

/**
 * This class is a member of <strong>modeler-microservice</strong> project.
 *
 * @author vince
 */
public final class CollaborationsSubRoute extends VoidSubRoute {

  private static final Logger logger = LogManager.getLogger(CollaborationsSubRoute.class);

  public static final String PATH = "collaborations/";

  private static final FindOptions COLLABORATION_FIELDS =
      new FindOptions().setFields(new JsonObject()
                                      .put("id", 1)
                                      .put("language", 1)
                                      .put("name", 1)
                                      .put("documentation", 1)
                                      .put("team", 1)
                                      .put("$domain", 1));

  public CollaborationsSubRoute(Vertx vertx, Router router) {
    super(PATH, vertx, router, false);
  }

  @Override
  protected void init() {
    router.route(HttpMethod.GET, path).handler(this::get);

    router.route(HttpMethod.POST, path + "new").handler(this::post);
    router.route(HttpMethod.POST, path + "completeTask").handler(this::completeTask);

    router.route(HttpMethod.PUT, path + ":id/svg").handler(this::writeSVG);

    router.route(HttpMethod.PUT, path + ":id/team").handler(this::putTeam);
    router.route(HttpMethod.PUT, path + ":id/new").handler(this::startNew);

    router.route(HttpMethod.DELETE, path + ":id").handler(this::delete);
  }

  private void writeSVG(RoutingContext context) {
    final JsonObject body = context.getBodyAsJson();
    final String svg = body.getString("svg");
    final String diagramId = context.pathParam("id");
    AssetsSubRoute.writeSVG(svg, diagramId);
    context.response().end();
  }

  private void completeTask(RoutingContext context) {
    try {
      final JsonObject body = context.getBodyAsJson();
      final JsonObject task = body.getJsonObject("task");
      final JsonObject variable = body.getJsonObject("variable");
      final String svg = body.getString("svg");
      ProcessEngineUtils.completeTask(task, variable, complete -> {
        if (complete.succeeded()) {
          final HistoricProcessInstance process =
              ProcessEngineUtils.getHistoricProcess(task.getString("processId"), true);
          if (process != null) {
            final String diagramId = process.getBusinessKey();
            AssetsSubRoute.writeSVG(svg, diagramId);
            double progress = (Double) process.getProcessVariables().get("progress");
            mongodb.findOneAndUpdate(Domain.ofDefinition(Domain.Definition.DIAGRAM).getCollection(),
                                     new JsonObject().put("id", diagramId),
                                     new JsonObject().put("$set", new JsonObject()
                                         .put("lastModified",
                                              DBUtils.mongoDateTime(OffsetDateTime.now()))
                                         .put("progress", progress)
                                     ), updateProgress -> {
                  if (updateProgress.succeeded()) {
                    new JsonResponse(context).end();
                  } else { new JsonResponse(context).fail(updateProgress.cause()); }
                });
          } else {
            new JsonResponse(context).fail(new NullPointerException("process instance not found"));
          }
        } else { new JsonResponse(context).fail(complete.cause()); }
      });
    } catch (Exception e) {
      context.fail(e);
    }
  }

  private static void isAdminOrOwnerOfDesign(
      User user, String designId, Handler<Future<Boolean>> handler) {
    if (isAdmin(user)) {
      handler.handle(Future.succeededFuture(true));
      return;
    }
    JsonObject query =
        new JsonObject()
            .put("id", designId)
            .put("team.owner", AuthUtils.getAccount(user).getString("id"));
    cpd.dataDB()
       .findOne(
           Domain.ofDefinition(Domain.Definition.DIAGRAM).getCollection(),
           query,
           new JsonObject(),
           findOne -> {
             if (findOne.succeeded()) {
               JsonObject result = findOne.result();
               handler.handle(Future.succeededFuture(result != null && !result.isEmpty()));
             } else {
               handler.handle(Future.failedFuture(findOne.cause()));
             }
           });
  }

  private void get(RoutingContext context) {
    List<String> params;
    final JsonObject idQuery = new JsonObject();

    params = context.queryParam("id");
    if (params != null && params.size() > 0) {
      idQuery.put("id", params.get(0));
    }

    params = context.queryParam("myrole");
    if (params != null && params.size() > 0) {
      User user = context.user();
      if (user == null) {
        JsonResponse.endWithEmptyArray(context);
        return;
      }
      String userId = AuthUtils.getAccount(user).getString("id");
      idQuery.put(
          "$or",
          new JsonArray(
              Arrays.stream(params.get(0).split("\\s*,\\s*"))
                    .map(role -> new JsonObject().put("team." + role, userId))
                    .collect(Collectors.toList())));
    }

    final Domain diagramDomain = Domain.ofDefinition(Domain.Definition.DIAGRAM);
    final String collection = diagramDomain.getCollection();
    final JsonObject query = DBUtils.and(Arrays.asList(diagramDomain.getQuery(), idQuery));
    mongodb.findWithOptions(
        collection,
        query,
        COLLABORATION_FIELDS,
        find -> {
          if (find.succeeded()) {
            new JsonResponse(context).end(find.result());
          } else {
            context.fail(find.cause());
          }
        });
  }

  private void putTeam(RoutingContext context) {
    final String collaborationId = context.pathParam("id");
    if (collaborationId == null) {
      context.fail(new NullPointerException("no collaboration id"));
      return;
    }
    final JsonObject body = context.getBodyAsJson();
    if (body == null || body.isEmpty()) {
      context.fail(new NullPointerException("no body"));
      return;
    }
    final JsonObject team = body.getJsonObject("team");
    AuthUtils.isEngaged(
        context.user(),
        collaborationId,
        isEngaged -> {
          if (isEngaged.succeeded()) {
            if (!isEngaged.result()) {
              new JsonResponse(context).fail(
                  new NullPointerException(),
                  HttpResponseStatus.NOT_FOUND
              );
              return;
            }
            final JsonObject idQuery = new JsonObject().put("id", collaborationId);
            final Domain diagramDomain = Domain.ofDefinition(Domain.Definition.DIAGRAM);
            final String collection = diagramDomain.getCollection();
            final JsonObject query = DBUtils.and(Arrays.asList(diagramDomain.getQuery(), idQuery));
            final JsonObject update = new JsonObject().put("$set", new JsonObject(
                team.stream()
                    .collect(Collectors.toMap(
                        entry -> "team." + entry.getKey(),
                        Entry::getValue))
            ));
            mongodb.findOneAndUpdate(
                collection,
                query,
                update,
                findOneAndUpdate -> {
                  if (findOneAndUpdate.succeeded()) {
                    final JsonObject original = findOneAndUpdate.result();
                    final String $domain = original.getString("$domain");
                    if ($domain != null && Domain.ofDefinition(Domain.Definition.DIAGRAM)
                                                 .getDomains()
                                                 .contains($domain)) {
                      ProcessEngineUtils.update(
                          new JsonObject()
                              .put("original", original)
                              .put("changes", new JsonObject().put("team", team)),
                          updated -> {
                            if (updated.succeeded()) {
                              new JsonResponse(context).end(original);
                            } else { context.fail(updated.cause()); }
                          });
                    } else { new JsonResponse(context).end(original); }
                  } else { context.fail(findOneAndUpdate.cause()); }
                });
          } else { context.fail(isEngaged.cause()); }
        });
  }

  private void startNew(RoutingContext context) {
    final String collaborationId = context.pathParam("id");
    if (collaborationId == null) {
      context.fail(new NullPointerException("no collaboration id"));
      return;
    }
    isAdminOrOwnerOfDesign(
        context.user(),
        collaborationId,
        isAdminOrOwner -> {
          if (isAdminOrOwner.succeeded()) {
            if (isAdminOrOwner.result()) {
              mongodb.findOneAndUpdate(
                  Domain.ofDefinition(Domain.Definition.DIAGRAM).getCollection(),
                  new JsonObject().put("id", collaborationId),
                  new JsonObject().put("$set", new JsonObject().put("progress", 0d)),
                  findOneAndUpdate -> {
                    if (findOneAndUpdate.succeeded()) {
                      JsonObject collaboration = findOneAndUpdate.result();
                      ProcessEngineUtils.startCollaboration(
                          collaborationId, collaboration.getJsonObject("team"));
                      new JsonResponse(context).end(collaboration);
                    } else { context.fail(findOneAndUpdate.cause()); }
                  });
            } else { context.fail(HttpResponseStatus.UNAUTHORIZED.code()); }
          } else { context.fail(isAdminOrOwner.cause()); }
        });
  }

  private void post(RoutingContext context) {
    if (isCivilServantFailOtherwise(context)) {
      final String ownerId = AuthUtils.getAccount(context).getString("id");
      final JsonObject body = context.getBodyAsJson();
      final String notation = body.getString("notation");
      final String $domain = body.getString("$domain");
      final JsonObject team = body.getJsonObject("team");
      if (notation == null || $domain == null || team == null) {
        context.fail(new IllegalStateException("no collaboration team provided"));
        return;
      } else {
        final String sentOwnerId = team.getJsonArray("owner").getString(0);
        if (!ownerId.equals(sentOwnerId)) {
          context.fail(
              new IllegalStateException(
                  "expected owner id was <"
                      + ownerId
                      + "> but "
                      + "<"
                      + sentOwnerId
                      + "> was received"));
          return;
        }
      }
      final JsonArray ownerIdArray = new JsonArray().add(ownerId);

      final String language = cpd.language(context);
      final JsonObject now = DBUtils.mongoDateTime(OffsetDateTime.now());

      final JsonObject newDiagram = new JsonObject()
          .put("id", CommonUtils.coalesce(body.getString("id"), UUID.randomUUID().toString()))
          .put("notation", notation)
          .put("version", CommonUtils.coalesce(body.getInteger("version"), 1))
          .put("created", CommonUtils.coalesce(body.getJsonObject("created"), now))
          .put("lastModified", CommonUtils.coalesce(body.getJsonObject("lastModified"), now))
          .put("language", CommonUtils.coalesce(body.getString("language"), language))
          .put("name", CommonUtils.coalesce(body.getString("name"), "New Diagram"))
          .put("documentation", body.getString("documentation"))
          .put("team", new JsonObject()
              .put("owner", ownerIdArray)
              .put("reviewer", CommonUtils.coalesce(team.getJsonArray("reviewer"), ownerIdArray))
              .put("editor", CommonUtils.coalesce(team.getJsonArray("editor"), ownerIdArray))
              .put("observer",
                   CommonUtils.coalesce(team.getJsonArray("observer"), new JsonArray())))
          .put("progress", 0d)
          .put("$domain", $domain);

      final JsonObject newPlane = new JsonObject()
          .put("id", UUID.randomUUID().toString())
          .put("modelId", newDiagram.getString("id"))
          .put("unit", "px")
          .put("bounds",
               new JsonObject().put("x", 0.0).put("y", 0.0)
                               .put("width", 300.0).put("height", 240.0))
          .put("$domain", "Di.Plane");

      final JsonObject newRoot = new JsonObject()
          .put("id", UUID.randomUUID().toString())
          .put("designId", newDiagram.getString("id"))
          .put("language", language)
          .put("name", "New Procedure")
          .put("$domain", "Model.FPMN.Procedure");

      final JsonObject newRootShape = new JsonObject()
          .put("id", UUID.randomUUID().toString())
          .put("modelId", newRoot.getString("id"))
          .put("planeId", newPlane.getString("id"))
          .put("label", new JsonObject()
              .put("bounds", new JsonObject()
                  .put("x", 40.0)
                  .put("y", 40.0)
                  .put("width", 220.0)
                  .put("height", 40.0)))
          .put("bounds", new JsonObject()
              .put("x", 40.0)
              .put("y", 40.0)
              .put("width", 220.0)
              .put("height", 160.0))
          .put("$domain", "Di.Shape");

      final JsonObject newChild = new JsonObject()
          .put("id", UUID.randomUUID().toString())
          .put("designId", newDiagram.getString("id"))
          .put("parentId", newRoot.getString("id"))
          .put("language", language)
          .put("name", "New Phase")
          .put("$domain", "Model.FPMN.Phase");

      final JsonObject newChildShape = new JsonObject()
          .put("id", UUID.randomUUID().toString())
          .put("modelId", newChild.getString("id"))
          .put("planeId", newPlane.getString("id"))
          .put("label", new JsonObject()
              .put("bounds", new JsonObject()
                  .put("x", 40.0)
                  .put("y", 80.0)
                  .put("width", 220.0)
                  .put("height", 120.0)))
          .put("bounds", new JsonObject()
              .put("x", 40.0)
              .put("y", 80.0)
              .put("width", 220.0)
              .put("height", 120.0))
          .put("$domain", "Di.Shape");

      mongodb.save(Domain.Collection.MODELS, newDiagram, diagramSaved -> {
        if (diagramSaved.succeeded()) {
          mongodb.save(Domain.Collection.DIS, newPlane, planeSaved -> {
            if (planeSaved.succeeded()) {
              mongodb.save(Domain.Collection.MODELS, newRoot, rootSaved -> {
                if (rootSaved.succeeded()) {
                  mongodb.save(Domain.Collection.DIS, newRootShape, rootShapeSaved -> {
                    if (rootShapeSaved.succeeded()) {
                      mongodb.save(Domain.Collection.MODELS, newChild, childSaved -> {
                        if (childSaved.succeeded()) {
                          mongodb.save(Domain.Collection.DIS, newChildShape, childShapeSaved -> {
                            if (childShapeSaved.succeeded()) {
                              final String diagramId = newDiagram.getString("id");
                              ProcessEngineUtils.startCollaboration(diagramId, team);
                              new JsonResponse(context).end(diagramId);
                            } else { context.fail(childShapeSaved.cause()); }
                          });
                        } else { context.fail(childSaved.cause()); }
                      });
                    } else { context.fail(rootShapeSaved.cause()); }
                  });
                } else { context.fail(rootSaved.cause()); }
              });
            } else { context.fail(planeSaved.cause()); }
          });
        } else { context.fail(diagramSaved.cause()); }
      });
    }
  }

  private void delete(RoutingContext context) {
    if (isAdminOtherwiseFail(context)) {
      final String id = context.pathParam("id");
      if (id == null) {
        context.fail(new NullPointerException("no id"));
        return;
      }
      final JsonObject idQuery = new JsonObject().put("id", id);
      final Domain diagramDomain = Domain.ofDefinition(Domain.Definition.DIAGRAM);
      final String diagramCollection = diagramDomain.getCollection();
      final JsonObject diagramQuery = DBUtils.and(Arrays.asList(diagramDomain.getQuery(), idQuery));
      mongodb.findOneAndDelete(
          diagramCollection,
          diagramQuery,
          deleteDiagram -> {
            if (deleteDiagram.succeeded()) {
              if (deleteDiagram.result() == null) {
                context.fail(HttpResponseStatus.NOT_FOUND.code());
                return;
              }
              final JsonArray removed =
                  new JsonArray().add(new DeleteResult(diagramCollection, 1).toJson());
              final Countdown countdown =
                  new Countdown(3)
                      .onComplete(
                          launch -> {
                            if (launch.succeeded()) {
                              // todo: remove process
                              new JsonResponse(context).end(removed);
                            }
                          });
              mongodb.removeDocuments(
                  Domain.Collection.MODELS,
                  new JsonObject().put("designId", id),
                  removeModels -> {
                    if (removeModels.succeeded()) {
                      removed.add(removeModels.result().toJson());
                      countdown.next();
                    } else { context.fail(removeModels.cause()); }
                  });
              mongodb.findOneAndDelete(
                  Domain.Collection.DIS,
                  new JsonObject().put("modelId", id),
                  deletePlane -> {
                    if (deletePlane.succeeded()) {
                      removed.add(new DeleteResult(Domain.Collection.DIS, 1).toJson());
                      JsonObject plane = deletePlane.result();
                      mongodb.removeDocuments(
                          Domain.Collection.DIS,
                          new JsonObject().put("planeId", plane.getString("id")),
                          removeDIs -> {
                            if (removeDIs.succeeded()) {
                              removed.add(removeDIs.result().toJson());
                              countdown.next();
                            } else { context.fail(removeDIs.cause()); }
                          });
                    } else { context.fail(deletePlane.cause()); }
                  });
              ProcessEngineUtils.deleteCollaboration(id);
              countdown.next();
            } else { context.fail(deleteDiagram.cause()); }
          });
    }
  }
}
