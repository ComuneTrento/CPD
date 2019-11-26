package it.beng.modeler.microservice.subroute;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.impl.NoStackTraceThrowable;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import it.beng.microservice.common.Countdown;
import it.beng.microservice.schema.ValidationResult;
import it.beng.modeler.config.cpd;
import it.beng.modeler.microservice.http.JsonResponse;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This class is a member of <strong>modeler-microservice</strong> project.
 *
 * @author vince
 */
public final class SchemaSubRoute extends VoidSubRoute {

  public SchemaSubRoute(Vertx vertx, Router router) {
    super(cpd.server.schema.path, vertx, router, false);
  }

  @Override
  protected void init() {

    router.route(HttpMethod.GET, path + "").handler(this::getList);
    router
        .routeWithRegex(HttpMethod.GET, "^" + Pattern.quote(path) + ".*/source$")
        .handler(this::getSchemaSource);
    router
        .routeWithRegex(HttpMethod.GET, "^" + Pattern.quote(path) + ".*/validate/([^\\/]+)$")
        .handler(this::validateCollection);
    router.route(HttpMethod.GET, path + "*").handler(this::getSchema);
    router.route(HttpMethod.POST, path + "*").handler(this::validate);
    router.route(HttpMethod.PUT, path + "*").handler(this::save);
    router.route(HttpMethod.DELETE, path + "*").handler(this::delete);
  }

  private void getList(RoutingContext context) {
    schemaTools.getSchemaList(
        getSchemaList -> {
          if (getSchemaList.succeeded())
            new JsonResponse(context)
                .end(
                    getSchemaList.result().stream()
                        .map(item -> item.put("$domain", schemaTools.relRef(item.getString("$id"))))
                        .collect(Collectors.toList()));
          else context.fail(getSchemaList.cause());
        });
  }

  private void getSchema(RoutingContext context) {
    final String $id = cpd.server.schema.fixUriScheme(context.request().absoluteURI());
    schemaTools.getSchema(
        $id,
        getExtendedSchema -> {
          if (getExtendedSchema.succeeded())
            new JsonResponse(context).end(getExtendedSchema.result());
          else context.fail(getExtendedSchema.cause());
        });
  }

  private void validateCollection(RoutingContext context) {
    String collection = context.pathParam("param0");
    if (collection == null) {
      new JsonResponse(context).end(null);
      return;
    }
    String $id;
    {
      String abs = cpd.server.schema.fixUriScheme(context.request().absoluteURI());
      $id = abs.substring(0, abs.length() - ("/validate/").length() - collection.length());
    }
    schemaTools.getSource(
        $id,
        getSchema -> {
          if (getSchema.succeeded()) {
            final JsonObject schema = getSchema.result();
            if (schema != null) {
              JsonObject query = new JsonObject();
              String domain = null;
              {
                JsonObject properties = schema.getJsonObject("properties");
                if (properties != null) {
                  JsonObject $domain = properties.getJsonObject("$domain");
                  if ($domain != null) {
                    domain = $domain.getString("const");
                  }
                }
              }
              if (domain != null) query.put("$domain", domain);
              mongodb.find(
                  collection,
                  query,
                  find -> {
                    if (find.succeeded()) {
                      List<JsonObject> instances = find.result();
                      context.response().putHeader("content-type", "text/plain; charset=utf-8");
                      if (instances.size() > 0) {
                        Countdown countdown = new Countdown(instances.size());
                        StringBuffer result = new StringBuffer();
                        for (JsonObject instance : instances) {
                          schemaTools.validate(
                              $id,
                              instance,
                              validate -> {
                                result
                                    .append("instance: ")
                                    .append(instance.encodePrettily())
                                    .append("\n--> ");
                                if (validate.succeeded()) {
                                  ValidationResult validation = validate.result();
                                  if (validation.isValid())
                                    result.append("successfully validated.");
                                  else {
                                    result
                                        .append("validation error: ")
                                        .append(validation.errors().encodePrettily());
                                  }
                                } else {
                                  result
                                      .append("could not be validated: ")
                                      .append(validate.cause().getMessage());
                                }
                                if (countdown.next().hasNext()) result.append("\n\n");
                                else context.response().end(result.toString());
                              });
                        }
                      } else {
                        context.response().end("nothing to validate!");
                      }
                    } else {
                      context.fail(find.cause());
                    }
                  });
            } else new JsonResponse(context).end(null);
          } else context.fail(getSchema.cause());
        });
  }

  private void getSchemaSource(RoutingContext context) {
    String $id;
    {
      String abs = cpd.server.schema.fixUriScheme(context.request().absoluteURI());
      $id = abs.substring(0, abs.length() - "/source".length());
    }
    schemaTools.getSource(
        $id,
        getSchemaSource -> {
          if (getSchemaSource.succeeded()) new JsonResponse(context).end(getSchemaSource.result());
          else context.fail(getSchemaSource.cause());
        });
  }

  private void validate(RoutingContext context) {
    final String $id = cpd.server.schema.fixUriScheme(context.request().absoluteURI());
    final JsonObject document = context.getBodyAsJson();
    boolean ok = document != null;
    if (ok) {
      final Object value = document.getValue("value");
      ok = value != null;
      if (ok) {
        schemaTools.validate(
            $id,
            value,
            validate -> {
              if (validate.succeeded()) context.response().end();
              else context.fail(validate.cause());
            });
      }
    }
    if (!ok) context.fail(new NoStackTraceThrowable("no value to validate against " + $id));
  }

  private void save(RoutingContext context) {
    //        isAuthorized(context, "schema", "editor", isAuthorized -> {
    //            if (isAuthorized.succeeded()) {
    final String $id = cpd.server.schema.fixUriScheme(context.request().absoluteURI());
    final JsonObject body = context.getBodyAsJson();
    boolean ok = body != null;
    if (ok) {
      final JsonObject schema = body.getJsonObject("schema");
      ok = schema != null;
      if (ok) {
        schema.put("$id", $id);
        schemaTools.saveSchemaSource(
            schema,
            saveSchema -> {
              if (saveSchema.succeeded())
                new JsonResponse(context).end(saveSchema.result().toJson());
              else context.fail(saveSchema.cause());
            });
      }
    }
    if (!ok) context.fail(new NoStackTraceThrowable("no schema to save for " + $id));
    //            } else throw new ResponseError(context, isAuthorized.cause());
    //        });
  }

  private void delete(RoutingContext context) {
    context.fail(new NoStackTraceThrowable("API not implemented"));
    /*
    isAuthorized(context, "schema", "editor", isAuthorized -> {
        if (isAuthorized.succeeded()) {
            schemaTools.deleteSchema(context.request().absoluteURI(), deleteSchema -> {
                if (deleteSchema.succeeded())
                    JSON_OBJECT_RESPONSE_END(context, deleteSchema.result());
                else
                    throw new ResponseError(context, deleteSchema.cause());
            });
        } else throw new ResponseError(context, isAuthorized.cause());
    });
    */
  }
}
