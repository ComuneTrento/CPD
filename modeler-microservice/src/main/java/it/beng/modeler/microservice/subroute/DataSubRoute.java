package it.beng.modeler.microservice.subroute;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import it.beng.modeler.microservice.http.JsonResponse;
import it.beng.modeler.microservice.utils.JsonUtils;
import java.util.Map;

/**
 * This class is a member of <strong>modeler-microservice</strong> project.
 *
 * @author vince
 * @author vince
 */
public final class DataSubRoute extends VoidSubRoute {

  public DataSubRoute(Vertx vertx, Router router) {
    super("data/", vertx, router, false);
  }

  @Override
  protected void init() {

    // GET
    // returns a list of items
    router.route(HttpMethod.GET, path + ":collection").handler(this::find);
    // returns the specified item
    router.route(HttpMethod.GET, path + ":collection/:id").handler(this::findOne);
    // returns the specified field of the specified item
    router.route(HttpMethod.GET, path + ":collection/:id/:field").handler(this::findField);
    // POST
    // creates a new item
    router.route(HttpMethod.POST, path + ":collection").handler(this::postDocument);
    // not allowed
    router.route(HttpMethod.POST, path + ":collection/:id").handler(SubRoute::failMethodNotAllowed);
    // PUT
    // updates all items
    router.route(HttpMethod.PUT, path + ":collection").handler(this::putDocument);
    // updates the specified item
    // router.route(HttpMethod.PUT, path + ":collection/:id").handler(this::putItem);
    // // DELETE
    // // deletes all items
    // router.route(HttpMethod.DELETE, path + ":collection").handler(this::deleteCollection);
    // // deletes the specified item
    // router.route(HttpMethod.DELETE, path + ":collection/:id").handler(this::deleteItem);

  }

  // TODO: queryParams values are all strings, check if it is necessary to cast each string to
  // proper type as given by
  // schema/collection
  private static JsonObject query(RoutingContext context) {
    JsonObject query = new JsonObject();
    for (Map.Entry<String, String> entry : context.queryParams()) {
      String key = entry.getKey();
      String value = entry.getValue();
      if (value.contains("|")) {
        JsonArray or = new JsonArray();
        for (String orValue : value.split("\\|")) {
          or.add(new JsonObject().put(key, orValue));
        }
        query.put("$or", or);
      } else { query.put(key, value); }
    }
    return query;
  }

  /* DATA API */

  private void find(RoutingContext context) {
    if (isLoggedInOtherwiseFail(context)) {
      String collection = context.pathParam("collection");
      JsonResponse response = new JsonResponse(context).chunked();
      mongodb
          .findBatch(collection, query(context))
          .exceptionHandler(context::fail)
          .handler(response::write)
          .endHandler(h -> response.end());
    }
  }

  private void findOne(RoutingContext context) {
    if (isLoggedInOtherwiseFail(context)) {
      String collection = context.pathParam("collection");
      JsonObject query = new JsonObject().put("id", context.pathParam("id"));
      mongodb.findOne(
          collection,
          query,
          new JsonObject(),
          findOne -> {
            JsonResponse response = new JsonResponse(context);
            if (findOne.succeeded() && findOne.result().size() > 0) {
              response.end(findOne.result());
            } else { response.end(null); }
          });
    }
  }

  private void findField(RoutingContext context) {
    if (isLoggedInOtherwiseFail(context)) {
      String collection = context.pathParam("collection");
      JsonObject query = new JsonObject().put("id", context.pathParam("id"));
      FindOptions options = new FindOptions();
      options.setFields(new JsonObject().put(context.pathParam("field"), 1));
      mongodb.findWithOptions(
          collection,
          query,
          options,
          getItem -> {
            if (getItem.succeeded()) {
              new JsonResponse(context).end(JsonUtils.firstOrNull(getItem.result()));
            } else { new JsonResponse(context).end(null); }
          });
    }
  }

  private void postDocument(RoutingContext context) {
    if (isAdminOtherwiseFail(context)) {
      String collection = context.pathParam("collection");
      JsonObject document = context.getBodyAsJson();
      schemaTools.validate(
          schemaTools.absRef(document.getString("$domain")),
          document,
          validate -> {
            if (validate.succeeded()) {
              if (validate.result().isValid()) {
                // save to db
                mongodb.insert(
                    collection,
                    document,
                    save -> {
                      JsonResponse response = new JsonResponse(context);
                      if (save.succeeded()) {
                        response.status(HttpResponseStatus.CREATED).end(save.result());
                      } else {
                        context.fail(save.cause());
                      }
                    });
              } else {
                context.fail(HttpResponseStatus.UNPROCESSABLE_ENTITY.code());
              }
            } else {
              context.fail(validate.cause());
            }
          });
    }
  }

  private void putDocument(RoutingContext context) {
    if (isAdminOtherwiseFail(context)) {
      String collection = context.pathParam("collection");
      JsonObject document = context.getBodyAsJson();
      mongodb.save(
          collection,
          document,
          save -> {
            if (save.succeeded()) {
              new JsonResponse(context).status(HttpResponseStatus.CREATED).end(save.result());
            } else {
              context.fail(save.cause());
            }
          });
    }
  }
}
