package it.beng.microservice.schema;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import it.beng.microservice.common.AsyncHandler;
import it.beng.microservice.common.Countdown;
import it.beng.microservice.db.DeleteResult;
import it.beng.microservice.db.JsonMapper;
import it.beng.microservice.db.MongoDB;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.script.ScriptException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public class SchemaTools {

  public static final Map<String, String> DEFAULT_MAPPINGS = new HashMap<String, String>() {
    {
      put("$id", "_id");
      put("$schema", "\uFF04schema");
      put("$ref", "\uFF04ref");
      put("$extends", "\uFF04extends");
    }
  };

    /*
        static final Pattern URI_PATTERN = Pattern.compile(
            "^(?:(?:https?|ftp)://)(?:\\S+(?::\\S*)?@)?(?:(?!10(?:\\.\\d{1,3}){3})(?!127(?:\\.\\d{1,3}){3})(?!169\\.254(?:\\.\\d{1,3}){2})(?!192\\.168(?:\\.\\d{1,3}){2})(?!172\\.(?:1[6-9]|2\\d|3[0-1])(?:\\.\\d{1,3}){2})(?:[1-9]\\d?|1\\d\\d|2[01]\\d|22[0-3])(?:\\.(?:1?\\d{1,2}|2[0-4]\\d|25[0-5])){2}(?:\\.(?:[1-9]\\d?|1\\d\\d|2[0-4]\\d|25[0-4]))|(?:(?:[a-z\\x{00a1}-\\x{ffff}0-9]+-?)*[a-z\\x{00a1}-\\x{ffff}0-9]+)(?:\\.(?:[a-z\\x{00a1}-\\x{ffff}0-9]+-?)*[a-z\\x{00a1}-\\x{ffff}0-9]+)*(?:\\.(?:[a-z\\x{00a1}-\\x{ffff}]{2,})))(?::\\d{2,5})?(?:/[^\\s]*)?$",
            Pattern.CASE_INSENSITIVE
        );
    */
  private static final Logger logger = LogManager.getLogger(SchemaTools.class);
  // COMMAND_PATH = "it/beng/microservice/schema/commands/"
  public final String commandPath =
      SchemaTools.class.getPackage().getName().replace('.', '/') + "/commands/";
  public final Map<String, String> mappings = new HashMap<String, String>(DEFAULT_MAPPINGS);
  public final String collection;
  public final String schemaUriBase;
  public final String schemaPublicScheme;
  public final String schemaLocalScheme;
  private final Ajv ajv = new Ajv();
  // private final Vertx vertx;
  private final MongoDB mongo;
  private final boolean schemaSchemeDiffer;

  public SchemaTools(Vertx vertx, MongoClient mongoClient, String collection, String schemaUriBase,
      String schemaLocalScheme, Map<String, String> mappingsExtension,
      AsyncHandler<Void> completeHandler) {
    // this.vertx = vertx;
    if (mappingsExtension != null) {
      this.mappings.putAll(mappingsExtension);
    }
    this.mongo = MongoDB.create(vertx, mongoClient, commandPath, this.mappings);
    this.collection = collection;
    this.schemaUriBase = schemaUriBase + (schemaUriBase.endsWith("/") ? "" : "/");
    this.schemaPublicScheme = this.schemaUriBase.substring(0, this.schemaUriBase.indexOf("://"));
    this.schemaLocalScheme = schemaLocalScheme;

    this.schemaSchemeDiffer = !this.schemaPublicScheme.equals(this.schemaLocalScheme);

    addAllSchemas(complete -> {
        if (complete.succeeded()) { completeHandler.handle(Future.succeededFuture()); } else {
            completeHandler.handle(Future.failedFuture(complete.cause()));
        }
    });
  }

  public SchemaTools(Vertx vertx, MongoClient mongoClient, String collection, String schemaUriBase,
      String schemaLocalScheme, AsyncHandler<Void> completeHandler) {
    this(vertx, mongoClient, collection, schemaUriBase, schemaLocalScheme, null, completeHandler);
  }

  public void close() {
    mongo.close();
  }

  String fixUriScheme(String uri) {
    return schemaSchemeDiffer ? uri.replaceFirst("^" + schemaLocalScheme, schemaPublicScheme) : uri;
  }

  private boolean $idIsAbsolute(String $id) {
    Objects.requireNonNull($id);
    return $id.startsWith(schemaUriBase);
  }

  private boolean $idIsRelative(String $id) {
    Objects.requireNonNull($id);
    return !$id.startsWith(schemaUriBase);
  }

  public <T> void checkAbsoluteUri(String $id, AsyncHandler<T> handler) {
      if (!$idIsAbsolute($id)) {
          handler
              .handle(Future.failedFuture("expected an absolute uri but received '" + $id + "'"));
      } else { handler.handle(Future.succeededFuture()); }
  }

  public JsonMapper getMapper() {
    return mongo.getMapper();
  }

  public String absRef(String ref) {
    return ref.startsWith(schemaUriBase) ? ref : schemaUriBase + ref;
  }

  public List<String> absRef(List<String> refs) {
    return refs.stream().map(this::absRef).collect(Collectors.toList());
  }

  public String relRef(String ref) {
    return ref.startsWith(schemaUriBase) ? ref.substring(schemaUriBase.length()) : ref;
  }

  public List<String> relRef(List<String> refs) {
    return refs.stream().map(this::relRef).collect(Collectors.toList());
  }

  public void getSchemaList(AsyncHandler<List<JsonObject>> handler) {
    JsonObject query = new JsonObject();
    FindOptions options = new FindOptions()
        .setFields(new JsonObject().put("$id", 1).put("title", 1).put("description", 1));
    mongo.findWithOptions(collection, query, options, findWithOptions -> {
      if (findWithOptions.succeeded()) {
        List<JsonObject> list = findWithOptions.result();
        for (JsonObject item : list) {
          item.put("$id", absRef(item.getString("$id")));
        }
        handler.handle(Future.succeededFuture(list));
      } else {
        handler.handle(Future.failedFuture(findWithOptions.cause()));
      }
    });
  }

  public void validateSchema(JsonObject schema, AsyncHandler<ValidationResult> handler) {
    checkAbsoluteUri(schema.getString("$id"), checkAbsoluteUri -> {
      if (checkAbsoluteUri.succeeded()) {
        try {
          ValidationResult result = ajv.validateSchema(schema);
          handler.handle(Future.succeededFuture(result));
        } catch (ScriptException e) {
          handler.handle(Future.failedFuture(e));
        }
      } else {
        handler.handle(Future.failedFuture(checkAbsoluteUri.cause()));
      }
    });
  }

  public void validate(String $id, Object value, AsyncHandler<ValidationResult> handler) {
    checkAbsoluteUri($id, checkAbsoluteUri -> {
      if (checkAbsoluteUri.succeeded()) {
        try {
          ValidationResult result = ajv.validate($id, value);
          handler.handle(Future.succeededFuture(result));
        } catch (ScriptException e) {
          handler.handle(Future.failedFuture(e));
        }
      } else {
        handler.handle(Future.failedFuture(checkAbsoluteUri.cause()));
      }
    });
  }

  public void getSource(String $id, AsyncHandler<JsonObject> handler) {
    checkAbsoluteUri($id, checkAbsoluteUri -> {
      if (checkAbsoluteUri.succeeded()) {
        mongo.findOne(collection, new JsonObject().put("$id", relRef($id)), new JsonObject(),
            findOne -> {
                if (findOne.succeeded()) {
                    JsonObject result = findOne.result();
                    handler
                        .handle(
                            Future.succeededFuture(result != null ? result.put("$id", $id) : null));
                } else { handler.handle(Future.failedFuture(findOne.cause())); }
            });
      } else {
        handler.handle(Future.failedFuture(checkAbsoluteUri.cause()));
      }
    });
  }

  public void getSchema(String $id, AsyncHandler<JsonObject> handler) {
    checkAbsoluteUri($id, checkAbsoluteUri -> {
      if (checkAbsoluteUri.succeeded()) {
        if (ajv.schemas.containsKey($id)) {
          handler.handle(Future.succeededFuture(ajv.schemas.get($id)));
        } else {
          getSource($id, getSchema -> {
            if (getSchema.succeeded()) {
              JsonObject schema = getSchema.result();
              if (schema != null) {
                extendSources(Collections.singletonList(schema), null, extendSources -> {
                  if (extendSources.succeeded()) {
                    JsonObject extended = extendSources.result()
                        .getJsonArray("schemas")
                        .stream()
                        .filter(item -> item instanceof JsonObject)
                        .map(item -> (JsonObject) item)
                        .filter(item -> $id
                            .equals(item.getString("$id")))
                        .collect(Collectors.toList())
                        .get(0);
                    handler.handle(Future.succeededFuture(extended));
                  } else {
                    handler.handle(Future.failedFuture(extendSources.cause()));
                  }
                });
              } else {
                handler.handle(Future.succeededFuture(null));
              }

            } else {
              handler.handle(Future.failedFuture(getSchema.cause()));
            }
          });
        }
      } else {
        handler.handle(Future.failedFuture(checkAbsoluteUri.cause()));
      }
    });
  }

  public void saveSchemaSource(JsonObject source, AsyncHandler<ValidationResult> handler) {
    final String $id = source.getString("$id");
    checkAbsoluteUri($id, checkAbsoluteUri -> {
      if (checkAbsoluteUri.succeeded()) {
        extendSources(Collections.singletonList(source), null, extendSources -> {
          if (extendSources.succeeded()) {
            JsonObject schema = extendSources.result()
                .getJsonArray("schemas")
                .stream()
                .filter(item -> item instanceof JsonObject)
                .map(item -> (JsonObject) item)
                .filter(item -> $id.equals(item.getString("$id")))
                .collect(Collectors.toList())
                .get(0);
            final ValidationResult validation;
            try {
              validation = ajv.addSchema(schema);
            } catch (ScriptException e) {
              logger.error(
                  "could not add schema '" + schema.getString("$id") + "': " + e.getMessage());
              handler.handle(Future.failedFuture(e));
              return;
            }
            if (validation.isValid()) {
              logger.debug("schema: " + schema.encodePrettily());
              logger.info("saving source " + source.encodePrettily());
              mongo.save(collection, source.put("$id", relRef($id)), save -> {
                  if (save.succeeded()) {
                      handler.handle(Future.succeededFuture(validation));
                  } else { handler.handle(Future.failedFuture(save.cause())); }
              });
            } else {
              logger.warn("skipping schema '" + schema.getString("$id") + "': "
                  + validation.errors().encodePrettily());
              // TODO: re-add downstream
              handler.handle(Future.succeededFuture(validation));
            }
          } else {
            handler.handle(Future.failedFuture(extendSources.cause()));
          }
        });
      } else {
        handler.handle(Future.failedFuture(checkAbsoluteUri.cause()));
      }
    });
  }

  public void deleteSchema(String $id, AsyncHandler<DeleteResult> handler) {
    checkAbsoluteUri($id, checkAbsoluteUri -> {
      if (checkAbsoluteUri.succeeded()) {
        // TODO: check the presence of instances for the schema, in that case fail
        mongo.removeDocument(collection, new JsonObject().put("$id", relRef($id)),
            removeDocument -> {
                if (removeDocument.succeeded()) {
                    try {
                        ajv.removeSchema($id);
                        // TODO: invalidate downstream
                        handler.handle(Future.succeededFuture(removeDocument.result()));
                    } catch (ScriptException e) {
                        handler.handle(Future.failedFuture(e));
                    }
                } else { handler.handle(Future.failedFuture(removeDocument.cause())); }
            });
      } else {
        handler.handle(Future.failedFuture(checkAbsoluteUri.cause()));
      }
    });
  }

  private List<JsonObject> sortStream(List<JsonObject> stream) {
    return stream.stream().sorted((o1, o2) -> {
      final JsonArray $extends1 = o1.getJsonArray("$extends", new JsonArray());
      final JsonArray $extends2 = o2.getJsonArray("$extends", new JsonArray());
      final boolean o1lto2 = $extends1.contains(o2.getString("$id"));
      final boolean o2lto1 = $extends2.contains(o1.getString("$id"));
        if (o1lto2 && o2lto1) { throw new IllegalStateException("cannot stream cyclic $extends"); }
        if (o1lto2) { return -1; }
        if (o2lto1) { return 1; }
      return 0;
    }).collect(Collectors.toList());
  }

  public void getSourceExtensionStreams(List<String> schema$ids, boolean haveUpstream,
      boolean haveDownstream,
      final AsyncHandler<List<JsonObject>> handler) {
    HashMap<String, String> params = new HashMap<String, String>() {
      private static final long serialVersionUID = 1L;

      {
        put("collection", collection);
        put("match$id",
            (schema$ids != null) ? "{\"$in\":" + new JsonArray(schema$ids).encode() + "}"
                : "{\"$ne\":null}");
        put("match$haveUpstream", (haveUpstream) ? "{\"$ne\":null}" : "{\"$size\":0}");
        put("match$haveDownstream", (haveDownstream) ? "{\"$ne\":null}" : "{\"$size\":0}");
      }
    };
    mongo.runCommand("aggregate", mongo.command("getSourceExtensionStreams", params),
        getSourceExtensionStreams -> {
          if (getSourceExtensionStreams.succeeded()) {
            List<JsonObject> schemas = getSourceExtensionStreams.result()
                .getJsonArray("result")
                .stream()
                .filter(item -> item instanceof JsonObject)
                .map(item -> (JsonObject) item)
                .map(item -> item
                    .put("$id", absRef(item.getString("$id")))
                    .put("upstream",
                        new JsonArray(sortStream(item
                            .getJsonArray("upstream")
                            .stream()
                            .filter(u -> u instanceof JsonObject)
                            .map(u -> (JsonObject) u)
                            .map(u -> u.put("$id",
                                absRef(u.getString("$id"))))
                            .collect(Collectors
                                .toList()))))
                    .put("downstream",
                        new JsonArray(sortStream(item
                            .getJsonArray("downstream")
                            .stream()
                            .filter(u -> u instanceof JsonObject)
                            .map(u -> (JsonObject) u)
                            .map(u -> u.put("$id",
                                absRef(u.getString("$id"))))
                            .collect(Collectors
                                .toList())))))
                .collect(Collectors.toList());
            logger.debug("schemas: " + new JsonArray(schemas).encodePrettily());
            handler.handle(Future.succeededFuture(schemas));
          } else {
            handler.handle(Future.failedFuture(getSourceExtensionStreams.cause()));
          }
        });
  }

  private void addAllSchemas(final AsyncHandler<Void> completeHandler) {
    // retrieve all schema leaves: all schemas that have only extension upstreams
    getSourceExtensionStreams(null, true, false, getSourceExtensionStreams -> {
      if (getSourceExtensionStreams.succeeded()) {
        final Map<String, JsonObject> lookup = new LinkedHashMap<>();
        List<JsonObject> sources = getSourceExtensionStreams.result().stream().map(source -> {
          ((JsonArray) source.remove("upstream")).stream()
              .filter(item -> item instanceof JsonObject)
              .map(item -> (JsonObject) item)
              .forEach(item -> lookup.put(item.getString("$id"), item));
          source.remove("downstream");
          return source;
        }).collect(Collectors.toList());
        logger.debug("sources: " + new JsonArray(sources).encodePrettily());
        logger.debug("lookup: " + new JsonArray(new ArrayList<>(lookup.values())).encodePrettily());
        // extend all sources
        extendSources(sources, lookup, extendSources -> {
          if (extendSources.succeeded()) {
            extendSources.result()
                .getJsonArray("schemas")
                .stream()
                .filter(item -> item instanceof JsonObject)
                .map(item -> (JsonObject) item)
                .forEach(schema -> {
                  try {
                    final ValidationResult validation = ajv.addSchema(schema);
                      if (!validation.isValid()) {
                          logger.warn("skipping schema '" + schema.getString("$id") + "': "
                              + validation.errors().encodePrettily());
                      } else { logger.debug("schema: " + schema.encodePrettily()); }
                  } catch (ScriptException e) {
                    logger.warn("skipping schema '" + schema.getString("$id") + "' ("
                        + e.getMessage() + ")");
                  }
                });
            completeHandler.handle(Future.succeededFuture());
          } else {
            completeHandler.handle(Future.failedFuture(extendSources.cause()));
          }
        });
      } else {
        completeHandler.handle(Future.failedFuture(getSourceExtensionStreams.cause()));
      }
    });
  }

  public void extendSources(List<JsonObject> sources, Map<String, JsonObject> lookup,
      AsyncHandler<JsonObject> handler) {
    Objects.requireNonNull(sources);
    final LinkedHashMap<String, JsonObject> schemas = new LinkedHashMap<>();
      if (lookup == null) { lookup = new LinkedHashMap<>(); }
    final Countdown countdown = new Countdown(sources).onComplete(zero -> {
      handler.handle(Future.succeededFuture(new JsonObject().put("sources", sources)
          .put("schemas", new ArrayList<>(schemas.values()))));
    });
    final SchemaProcessor processor = new SchemaProcessor(this);
    for (JsonObject source : sources) {
      processor.extendSource(source.copy(), schemas, lookup, extendSource -> {
        if (extendSource.succeeded()) {
          countdown.next();
        } else {
          handler.handle(Future.failedFuture(extendSource.cause()));
        }
      });
    }
  }

}
