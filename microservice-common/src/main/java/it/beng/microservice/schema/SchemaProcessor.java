package it.beng.microservice.schema;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import it.beng.microservice.common.AsyncHandler;
import it.beng.microservice.common.Countdown;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>This class is a member of <strong>microservice-stack</strong> project.</p>
 *
 * @author vince
 */
class SchemaProcessor {

  private static List<String> IGNORED_KEYS = new ArrayList<String>() {
    private static final long serialVersionUID = 1L;

    {
      add("$extends");
    }
  };

  final SchemaTools schemaTools;

  SchemaProcessor(SchemaTools schemaTools) {
    this.schemaTools = schemaTools;
  }

  private JsonArray extendArray(JsonArray source, JsonArray extension) {
    for (Object extend : extension) {
      if (!source.contains(extend)) { source.add(extend); }
    }
    return source;
  }

  private JsonObject extendObject(JsonObject source, JsonObject extension) {
    for (Map.Entry<String, Object> entry : extension) {
      final String key = entry.getKey();
      if (IGNORED_KEYS.contains(key)) { continue; }
      final Object sourceValue = source.getValue(key);
      final Object extendValue = entry.getValue();
      if (sourceValue instanceof JsonObject) {
        if (extendValue instanceof JsonObject) {
          source.put(key, extendObject((JsonObject) sourceValue, (JsonObject) extendValue));
        }
      } else if (sourceValue instanceof JsonArray) {
        if (extendValue instanceof JsonArray) {
          source.put(key, extendArray((JsonArray) sourceValue, (JsonArray) extendValue));
        }
      } else if (sourceValue == null) {
        source.put(key, extendValue);
      }
    }
    return source;
  }

  private void getOrFindSource(String $id, Map<String, JsonObject> lookup,
      AsyncHandler<JsonObject> handler) {
    if (lookup.containsKey($id)) {
      handler.handle(Future.succeededFuture(lookup.get($id)));
    } else {
      schemaTools.getSource($id, getSource -> {
        if (getSource.succeeded()) {
          final JsonObject source = getSource.result();
          lookup.put($id, source);
          handler.handle(Future.succeededFuture(source));
        } else {
          handler.handle(Future.failedFuture(getSource.cause()));
        }
      });
    }
  }

  void extendSource(JsonObject source, LinkedHashMap<String, JsonObject> schemas,
      Map<String, JsonObject> lookup, AsyncHandler<Void> handler) {
    final String source$id = source.getString("$id");
    if (schemas
        .containsKey(source$id)) // if source has already been processed return the processed one
    { handler.handle(Future.succeededFuture()); } else { // if source hasn't already been processed
      JsonArray $extends = source.getJsonArray("$extends");
      if ($extends != null && $extends.size() > 0) { //if source extends something
        final Countdown countdown = new Countdown($extends.size()).onComplete(zero -> {
          handler.handle(Future.succeededFuture());
        });
        for (Object $extend : $extends) {
          if ($extend instanceof String) { // $extend must be a string!
            String $extend$id = schemaTools.absRef((String) $extend);
            if (schemas
                .containsKey($extend$id)) { // if the extended schema has already been processed
              final JsonObject schema = schemas.get($extend$id);
              // let's extend the source
              schemas.put(source$id, extendObject(source, schema));
              countdown.next();
            } else { // if the extended schema has not yet been processed
              getOrFindSource($extend$id, lookup, getOrFindSource -> {
                if (getOrFindSource.succeeded()) { // if we've the extended schema source
                  final JsonObject extended = getOrFindSource.result();
                  extendSource(extended, schemas, lookup,
                      extendSource -> { // let's process the extended schema source
                        if (extendSource.succeeded()) { // the extension has been processed
                          // let's extend the source
                          schemas.put(source$id, extendObject(source, extended));
                          countdown.next();
                        } else { // something wrong in extension processing
                          handler.handle(Future.failedFuture(extendSource.cause()));
                        }
                      });
                } else { // we could not find the schema source
                  handler.handle(Future.failedFuture(getOrFindSource.cause()));
                }
              });
            }
          } else { // $extendsItem is not a string!!
            handler.handle(Future.failedFuture(
                new IllegalStateException("$extends must be an array of string: "
                    + $extends
                    .encode())));
          }
        }
      } else { // if (source hasn't already been processed and) source extends nothing
        schemas.put(source$id, source);
        handler.handle(Future.succeededFuture());
      }
    }
  }

}
