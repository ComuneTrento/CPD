package it.beng.modeler.microservice.subroute;

import com.hazelcast.com.eclipsesource.json.ParseException;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.impl.NoStackTraceThrowable;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import it.beng.microservice.common.AsyncHandler;
import it.beng.microservice.db.MongoDB;
import it.beng.modeler.config.cpd;
import it.beng.modeler.microservice.http.JsonResponse;
import it.beng.modeler.microservice.utils.AuthUtils;
import it.beng.modeler.microservice.utils.DBUtils;
import it.beng.modeler.microservice.utils.JsonUtils;
import it.beng.modeler.model.Domain;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class is a member of <strong>modeler-microservice</strong> project.
 *
 * @author vince
 */
public final class ApiSubRoute extends VoidSubRoute {

  private static final Logger logger = LogManager.getLogger(ApiSubRoute.class);

  public ApiSubRoute(Vertx vertx, Router router) {
    super(cpd.server.api.path, vertx, router, false);
  }

  @Override
  protected void init() {

    // IMPORTANT: redirect "api" to "api/" because of strange behaviour with swagger.
    // It MUST be done with regex (i.e. must be exactly "api") to avoid infinite redirections
    router
        .routeWithRegex(
            HttpMethod.GET, "^" + Pattern.quote(path.substring(0, path.length() - 1)) + "$")
        .handler(context -> redirect(context, path));

    /* SIMPATICO public API */

    // stats
    // deprecated ...
    router
        .route(HttpMethod.GET, path + "stats/diagram/:procedureId/eServiceCount")
        .handler(this::getProcedureEServiceCount);
    // ... replaced by
    router
        .route(HttpMethod.GET, path + "stats/procedure/:procedureId/eServiceCount")
        .handler(this::getProcedureEServiceCount);
    // deprecated ...
    router
        .route(HttpMethod.GET, path + "stats/diagram/:procedureId/userFeedbackCount")
        .handler(this::getProcedureUserFeedbackCount);
    // ... replaced by
    router
        .route(HttpMethod.GET, path + "stats/procedure/:procedureId/userFeedbackCount")
        .handler(this::getProcedureUserFeedbackCount);

    // summary
    // deprecated ...
    router
        .route(HttpMethod.GET, path + "diagram/summary/list")
        .handler(this::getProcedureSummaryList);
    // ... replaced by
    router
        .route(HttpMethod.GET, path + "procedure/summary/list")
        .handler(this::getProcedureSummaryList);
    router
        .route(HttpMethod.GET, path + "procedure/:procedureId/summary")
        .handler(this::getProcedureSummary);
    // Deprecated ...
    router
        .route(HttpMethod.GET, path + "diagram/eService/:eServiceId/summary")
        .handler(this::getProcedureEServiceSummary);
    // ... replaced by
    router
        .route(HttpMethod.GET, path + "procedure/eService/:eServiceId/summary")
        .handler(this::getProcedureEServiceSummary);
    // new E-Service InteractionTask for Servicepedia
    router
        .route(HttpMethod.GET, path + "eService/:eServiceId/summary")
        .handler(this::getEServiceSummary);

    // feedback
    router
        .route(HttpMethod.GET, path + "user/feedback/:fromDateTime")
        .handler(this::getUserFeedback);
    router
        .route(HttpMethod.GET, path + "user/feedback/:fromDateTime/:toDateTime")
        .handler(this::getUserFeedback);
    router.route(HttpMethod.POST, path + "user/feedback").handler(this::postUserFeedback);

    /* CPD API */

    // diagram
    // TODO: this sould return an array of svg, use root ID for single svg diagram
    router.route(HttpMethod.GET, path + "diagram/:id.svg").handler(this::getModelDiagramSVG);
    router.route(HttpMethod.GET, path + "diagram/my").handler(this::getDiagramMyList);
    router.route(HttpMethod.GET, path + "diagram/search/:text").handler(this::getDiagramTextList);
    router.route(HttpMethod.GET, path + "diagram/newer/:limit").handler(this::getDiagramNewerList);

    router.route(HttpMethod.PUT, path + "diagram/:id.svg").handler(this::putModelDiagramSVG);

    // // diagram
    // router.route(HttpMethod.GET, path + "diagram/:id").handler(this::getDiagramElement);
    // router.route(HttpMethod.PUT, path + "diagram").handler(this::putDiagramElement);
    // router.route(HttpMethod.DELETE, path + "diagram/:id").handler(this::delDiagramElement);

    // // model
    // router.route(HttpMethod.GET, path + "model/:id").handler(this::getModelElement);
    // router.route(HttpMethod.PUT, path + "model").handler(this::putModelElement);
    // router.route(HttpMethod.DELETE, path + "model/:id").handler(this::delModelElement);

    // // lists
    // router.route(HttpMethod.GET, path +
    // "diagram/:rootId/elements").handler(this::getDiagramElements);
    // router.route(HttpMethod.GET, path +
    // "diagram/:rootId/models").handler(this::getDiagramModels);

    // // data
    // router.route(HttpMethod.GET, path + "data/stencilSetDefinition/:notation")
    //         .handler(this::getStencilSetDefinition);

    /* STATIC RESOURCES (swagger-ui) */

    router.route(HttpMethod.GET, path + "*").handler(StaticHandler.create("web/swagger-ui"));
  }

  /* PUBLIC API */

  private void getProcedureEServiceCount(RoutingContext context) {
    String procedureId = context.pathParam("procedureId");
    MongoDB.Command command =
        mongodb.command(
            "getProcedureEServiceCount",
            new HashMap<String, String>() {
              private static final long serialVersionUID = 1L;

              {
                put("procedureId", procedureId);
              }
            });
    mongodb.runCommand(
        "aggregate",
        command,
        ar -> {
          if (ar.succeeded()) {
            JsonArray result = ar.result().getJsonArray("result");
            if (result.size() > 0) {
              new JsonResponse(context).end(result.getJsonObject(0));
            } else {
              new JsonResponse(context)
                  .end(new JsonObject().put("id", procedureId).put("count", 0));
            }
          } else {
            context.fail(ar.cause());
          }
        });
  }

  private void getProcedureUserFeedbackCount(RoutingContext context) {
    String procedureId = context.pathParam("procedureId");
    MongoDB.Command command =
        mongodb.command(
            "getProcedureUserFeedbackCount",
            new HashMap<String, String>() {
              private static final long serialVersionUID = 1L;

              {
                put("procedureId", procedureId);
              }
            });
    mongodb.runCommand(
        "aggregate",
        command,
        ar -> {
          if (ar.succeeded()) {
            JsonResponse response = new JsonResponse(context);
            JsonArray result = ar.result().getJsonArray("result");
            if (result.size() > 0) {
              if (procedureId != null) { response.end(result.getJsonObject(0)); } else {
                response.end(result);
              }
            } else { response.end(new JsonObject().put("id", procedureId).put("count", 0)); }
          } else {
            context.fail(ar.cause());
          }
        });
  }

  private void getProcedureSummaryCommand(
      String procedureId, String eServiceId, String appHref, AsyncHandler<JsonArray> handler) {
    MongoDB.Command command =
        mongodb.command(
            "getProcedureSummary",
            new HashMap<String, String>() {
              {
                put("procedureId", procedureId != null ? "\"_id\":\"" + procedureId + "\"," : "");
                put("eServiceId",
                    eServiceId != null ? "\"phases.eServiceIds\": \"" + eServiceId + "\"" : "");
                put("appDiagramUrl", appHref + cpd.app.designerPath);
                put("appDiagramSvg", cpd.server.apiHref() + "diagram/");
              }
            });
    mongodb.runCommand(
        "aggregate",
        command,
        ar -> {
          if (ar.succeeded()) {
            handler.handle(Future.succeededFuture(ar.result().getJsonArray("result")));
          } else {
            handler.handle(Future.failedFuture(ar.cause()));
          }
        });
  }

  private void getProcedureSummaryList(RoutingContext context) {
    getProcedureSummaryCommand(
        null,
        null,
        cpd.server.appHref(context),
        command -> {
          if (command.succeeded()) {
            new JsonResponse(context).end(command.result());
          } else {
            context.fail(command.cause());
          }
        });
  }

  private void getProcedureSummary(RoutingContext context) {
    String procedureId = context.pathParam("procedureId");
    if (procedureId == null) {
      context.fail(new NullPointerException());
      return;
    }
    getProcedureSummaryCommand(
        procedureId,
        null,
        cpd.server.appHref(context),
        command -> {
          if (command.succeeded()) {
            new JsonResponse(context).end(JsonUtils.firstOrNull(command.result()));
          } else {
            context.fail(command.cause());
          }
        });
  }

  private void getProcedureEServiceSummary(RoutingContext context) {
    String eServiceId = context.pathParam("eServiceId");
    if (eServiceId == null) {
      context.fail(new NullPointerException());
      return;
    }
    getProcedureSummaryCommand(
        null,
        eServiceId,
        cpd.server.appHref(context),
        command -> {
          if (command.succeeded()) {
            new JsonResponse(context).end(JsonUtils.firstOrNull(command.result()));
          } else {
            context.fail(command.cause());
          }
        });
  }

  private void getEServiceSummary(RoutingContext context) {
    String eServiceId = context.pathParam("eServiceId");
    if (eServiceId == null) {
      context.fail(new NullPointerException());
      return;
    }
    mongodb.findOne(
        "models",
        new JsonObject().put("eServiceId", eServiceId),
        new JsonObject(),
        findOne -> {
          if (findOne.succeeded()) {
            new JsonResponse(context).end(findOne.result());
          } else {
            JsonResponse.endWithEmptyObject(context);
          }
        });
  }

  private static JsonObject mongoDateTimeRange(OffsetDateTime from, OffsetDateTime to) {
    JsonObject range = new JsonObject();
    if (from != null) { range.put("$gte", DBUtils.mongoDateTime(from)); }
    if (to != null) { range.put("$lt", DBUtils.mongoDateTime(to)); }
    return range;
  }

  private void getUserFeedback(RoutingContext context) {
    OffsetDateTime fromDateTime = DBUtils.parseDateTime(context.pathParam("fromDateTime"));
    if (fromDateTime == null) {
      context.fail(
          new NoStackTraceThrowable(
              "cannot parse '" + context.pathParam("fromDateTime") + "' as date-time"));
      return;
    }
    OffsetDateTime toDateTime = DBUtils.parseDateTime(context.pathParam("toDateTime"));
    JsonObject dateTimeRange = mongoDateTimeRange(fromDateTime, toDateTime);
    MongoDB.Command command =
        mongodb.command(
            "getUserFeedback",
            new HashMap<String, String>() {
              {
                put("dateTimeRange", dateTimeRange.encode());
                put("appDiagramUrl", cpd.server.appHref(context) + cpd.app.designerPath);
                put("appDiagramSvg", cpd.server.apiHref() + "model/diagram/");
              }
            });
    mongodb.runCommand(
        "aggregate",
        command,
        ar -> {
          if (ar.succeeded()) {
            new JsonResponse(context)
                .end(
                    new JsonObject()
                        .put("dateTimeRange", dateTimeRange)
                        .put("feedbackList", ar.result().getJsonArray("result")));
          } else {
            context.fail(ar.cause());
          }
        });
  }

  private void postUserFeedback(RoutingContext context) {
    if (isLoggedInOrFail(context)) {
      User user = context.user();
      if (user == null) {
        context.fail(new NoStackTraceThrowable("user is not authenticated"));
        return;
      }
      JsonObject feedback = context.getBodyAsJson();
      String message = feedback.getString("message");
      if (message == null || message.trim().length() == 0) {
        context.fail(new NoStackTraceThrowable("no message in feedback"));
        return;
      }
      String diagramId = feedback.getString("diagramId");
      if (diagramId == null || diagramId.trim().length() == 0) {
        context.fail(new NoStackTraceThrowable("no diagram ID in feedback"));
        return;
      }
      String elementId = feedback.getString("elementId");
      if (elementId == null || elementId.trim().length() == 0) {
        context.fail(new NoStackTraceThrowable("no element ID in feedback"));
        return;
      }
      feedback
          .put("id", UUID.randomUUID().toString())
          .put("userId", AuthUtils.getAccount(user).getString("id"))
          .put("dateTime", DBUtils.mongoDateTime(OffsetDateTime.now()));
      //        schemaTools.validate();

      mongodb.save(
          "user.feedbacks",
          feedback,
          save -> {
            if (save.succeeded()) {
              new JsonResponse(context).status(HttpResponseStatus.CREATED).end(save.result());
            } else {
              context.fail(save.cause());
            }
          });
    }
  }

  /* PRIVATE API */

  // Diagram

  private void getModelDiagramSVG(RoutingContext context) {
    String id = context.pathParam("id");
    vertx
        .fileSystem()
        .readFile(
            "web/assets/svg/" + id + ".svg",
            file -> {
              if (file.succeeded()) {
                context.response().putHeader("Content-Type", "image/svg+xml").end(file.result());
              } else {
                vertx
                    .fileSystem()
                    .readFile(
                        "web/assets/svg/diagram-not-found.svg",
                        notFound -> {
                          if (notFound.succeeded()) {
                            context
                                .response()
                                .putHeader("Content-Type", "image/svg+xml")
                                .end(notFound.result());
                          } else {
                            context.fail(notFound.cause());
                          }
                        });
              }
            });
  }

  private void putModelDiagramSVG(RoutingContext context) {
    String id = context.pathParam("id");
    AuthUtils.isEngaged(
        context.user(),
        id,
        isEngaged -> {
          if (passOrFail(
              context,
              isEngaged.succeeded() && isEngaged.result(),
              HttpResponseStatus.UNAUTHORIZED)) {}
        });
    String svg = context.getBodyAsString();
    vertx
        .fileSystem()
        .writeFile(
            "web/assets/svg/" + id + ".svg",
            Buffer.buffer(svg),
            file -> {
              if (file.succeeded()) {
                context.response().end();
              } else {
                context.fail(file.cause());
              }
            });
  }

  private void getModelDiagramList(
      List<String> diagramIds,
      List<String> userIds,
      String searchText,
      String searchLanguageCode,
      Integer limit,
      Handler<Future<List<JsonObject>>> handler) {
    if (diagramIds != null && diagramIds.isEmpty()) {
      handler.handle(Future.succeededFuture(Collections.emptyList()));
      return;
    }
    final JsonArray andArray = new JsonArray();
    if (diagramIds != null) {
      andArray.add(DBUtils.or("id", diagramIds));
    }
    if (userIds != null) {
      andArray.add(
          DBUtils.or(
              userIds.stream()
                     .map(userId ->
                              DBUtils.or(
                                  Arrays.asList(
                                      "team.owner", "team.reviewer", "team.editor",
                                      "team.observer"),
                                  userId))
                     .collect(Collectors.toList())));
    }
    if (searchText != null) {
      andArray.add(DBUtils.text(searchText, searchLanguageCode));
    }
    final Domain diagramDomain = Domain.ofDefinition(Domain.Definition.DIAGRAM);
    final String collection = diagramDomain.getCollection();
    final JsonObject query =
        DBUtils.and(
            Arrays.asList(
                diagramDomain.getQuery(),
                andArray.isEmpty() ? null : new JsonObject().put("$and", andArray)));
    logger.info("query: " + query.encodePrettily());
    final FindOptions findOptions =
        new FindOptions().setSort(new JsonObject().put("lastModified", -1));
    if (limit != null) {
      findOptions.setLimit(limit);
    }
    mongodb.findWithOptions(
        collection,
        query,
        findOptions,
        find -> {
          if (find.succeeded()) {
            handler.handle(Future.succeededFuture(find.result()));
          } else {
            handler.handle(Future.failedFuture(find.cause()));
          }
        });
  }

  private void getDiagramMyList(RoutingContext context) {
    String userId;
    try {
      userId = AuthUtils.getAccount(context).getString("id");
    } catch (Exception e) {
      JsonResponse.endWithEmptyArray(context);
      return;
    }
    getModelDiagramList(
        null,
        Collections.singletonList(userId),
        null,
        null,
        null,
        list -> {
          if (list.succeeded()) {
            new JsonResponse(context).end(list.result());
          } else {
            context.fail(list.cause());
          }
        });
  }

  private void getDiagramTextList(RoutingContext context) {
    String searchText = context.pathParam("text");
    getModelDiagramList(
        null,
        null,
        searchText,
        "italian", // cpd.languageCode(context),
        null,
        list -> {
          if (list.succeeded()) {
            new JsonResponse(context).end(list.result());
          } else {
            context.fail(list.cause());
          }
        });
  }

  private void getDiagramNewerList(RoutingContext context) {
    String limitStr = context.pathParam("limit");
    if (limitStr == null) {
      JsonResponse.endWithEmptyArray(context);
      return;
    }
    int limit;
    try {
      limit = Integer.parseInt(limitStr);
    } catch (ParseException e) {
      JsonResponse.endWithEmptyArray(context);
      return;
    }
    getModelDiagramList(
        null,
        null,
        null,
        null,
        limit,
        list -> {
          if (list.succeeded()) {
            new JsonResponse(context).end(list.result());
          } else {
            context.fail(list.cause());
          }
        });
  }

  // private void getDiagramElement(RoutingContext context) {
  //     String id = context.pathParam("id");
  //     modelTools.getDiagramEntity(id, getDiagram -> {
  //         if (getDiagram.succeeded()) {
  //             JSON_OBJECT_RESPONSE_END(context, getDiagram.result());
  //         } else {
  //             context.fail(getDiagram.cause());
  //         }
  //     });
  // }

  // private void putDiagramElement(RoutingContext context) {
  //     if (isLoggedIn(context)) {
  //         JsonObject entity = context.getBodyAsJson();
  //         // isDiagramEditor(context, entity, isDiagramEditor -> {
  //         //     if (isDiagramEditor.succeeded()) {
  //         //         modelTools.saveDiagramEntity(entity, saveDiagramEntity -> {
  //         //             if (saveDiagramEntity.succeeded()) {
  //         //                 JSON_OBJECT_RESPONSE_END(context,
  // saveDiagramEntity.result().toJson());
  //         //             } else {
  //         //                 throw new ResponseError(context, saveDiagramEntity.cause());
  //         //             }
  //         //         });
  //         //     } else {
  //         //         throw new ResponseError(context, isDiagramEditor.cause());
  //         //     }
  //         // });
  //     }
  // }

  // private void delDiagramElement(RoutingContext context) {
  //     if (isLoggedIn(context)) {
  //         String id = context.pathParam("id");
  //         // isDiagramEditor(context, id, isDiagramEditor -> {
  //         //     if (isDiagramEditor.succeeded()) {
  //         //         modelTools.deleteDiagramEntity(id, deleteDiagramEntity -> {
  //         //             if (deleteDiagramEntity.succeeded())
  //         //                 JSON_OBJECT_RESPONSE_END(context,
  // deleteDiagramEntity.result().toJson());
  //         //             else
  //         //                 throw new ResponseError(context, deleteDiagramEntity.cause());
  //         //         });
  //         //     } else {
  //         //         throw new ResponseError(context, isDiagramEditor.cause());
  //         //     }
  //         // });
  //     }
  // }

  // private void getModelElement(RoutingContext context) {
  //     String id = context.pathParam("id");
  //     modelTools.getModelEntity(id, getModelEntity -> {
  //         if (getModelEntity.succeeded()) {
  //             JSON_OBJECT_RESPONSE_END(context, getModelEntity.result());
  //         } else {
  //             JSON_NULL_RESPONSE_END(context);
  //         }
  //     });
  // }

  // private void putModelElement(RoutingContext context) {
  //     if (isLoggedIn(context)) {
  //         JsonObject entity = context.getBodyAsJson();
  //         // isModelEditor(context, entity, isModelEditor -> {
  //         //     if (isModelEditor.succeeded()) {
  //         //         modelTools.saveModelEntity(entity, saveModelEntity -> {
  //         //             if (saveModelEntity.succeeded()) {
  //         //                 JSON_OBJECT_RESPONSE_END(context,
  // saveModelEntity.result().toJson());
  //         //             } else {
  //         //                 throw new ResponseError(context, saveModelEntity.cause());
  //         //             }
  //         //         });
  //         //     } else {
  //         //         throw new ResponseError(context, isModelEditor.cause());
  //         //     }
  //         // });
  //     }
  // }

  // private void delModelElement(RoutingContext context) {
  //     if (isLoggedIn(context)) {
  //         String id = context.pathParam("id");
  //         // isDiagramEditor(context, id, isDiagramEditor -> {
  //         //     if (isDiagramEditor.succeeded()) {
  //         //         modelTools.deleteModelEntity(id, deleteModelEntity -> {
  //         //             if (deleteModelEntity.succeeded())
  //         //                 JSON_OBJECT_RESPONSE_END(context,
  // deleteModelEntity.result().toJson());
  //         //             else
  //         //                 throw new ResponseError(context, deleteModelEntity.cause());
  //         //         });
  //         //     } else {
  //         //         throw new ResponseError(context, isDiagramEditor.cause());
  //         //     }
  //         // });
  //     }
  // }

  // private void getDiagramElements(RoutingContext context) {
  //     String rootId = context.pathParam("rootId");
  //     modelTools.getDiagramElements(rootId, getDiagramElements -> {
  //         if (getDiagramElements.succeeded()) {
  //             JSON_ARRAY_RESPONSE_END(context, getDiagramElements.result());
  //         } else {
  //             context.fail(getDiagramElements.cause());
  //         }
  //     });
  // }

  // private void getDiagramModels(RoutingContext context) {
  //     String rootId = context.pathParam("rootId");
  //     modelTools.getDiagramModels(rootId, getDiagramModels -> {
  //         if (getDiagramModels.succeeded()) {
  //             JSON_ARRAY_RESPONSE_END(context, getDiagramModels.result());
  //         } else {
  //             context.fail(getDiagramModels.cause());
  //         }
  //     });
  // }

  // private void getSemanticList(RoutingContext context) {
  //     mongodb.find("semantic.entity", new JsonObject(), ar -> {
  //         if (ar.succeeded()) {
  //             JSON_ARRAY_RESPONSE_END(context, ar.result());
  //         } else {
  //             context.fail(ar.cause());
  //         }
  //     });
  // }

  // private void getSemanticListByType(RoutingContext context) {
  //     String clazz = context.pathParam("class");
  //     mongodb.find("semantic.entity", new JsonObject().put("class", clazz), ar -> {
  //         if (ar.succeeded()) {
  //             JSON_ARRAY_RESPONSE_END(context, ar.result());
  //         } else {
  //             context.fail(ar.cause());
  //         }
  //     });
  // }

  // private void getStencilSetDefinition(RoutingContext context) {
  //     final String notation = context.pathParam("notation");
  //     vertx.fileSystem().readFile(config.DATA_PATH + notation.replace(".", "/") + ".json", ar ->
  // {
  //         if (ar.succeeded()) {
  //             JSON_OBJECT_RESPONSE_END(context, ar.result().toJsonObject());
  //         } else {
  //             context.fail(ar.cause());
  //         }
  //     });
  // }

}
