package it.beng.modeler.microservice.utils;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import it.beng.microservice.common.AsyncHandler;
import it.beng.microservice.db.MongoDB;
import it.beng.modeler.config.cpd;
import it.beng.modeler.model.Domain;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class is a member of <strong>modeler-microservice</strong> project.
 *
 * @author vince
 */
public final class DBUtils {

  private static final MongoDB mongoDB = cpd.dataDB();
  private static final Domain COLLABORATION = Domain.ofDefinition(Domain.Definition.DIAGRAM);

  /* COLLECTION */
  public static void loadCollection(
      String collection, JsonObject query, AsyncHandler<List<JsonObject>> handler) {
    mongoDB.find(collection, query, handler);
  }

  public static void loadCollection(String collection, AsyncHandler<List<JsonObject>> handler) {
    loadCollection(collection, new JsonObject(), handler);
  }

  public static class Properties {
    private static Map<String, Object> P = new HashMap<>();

    protected static JsonObject value(Object value) {
      return new JsonObject().put("value", value);
    }

    public static void get(String property, AsyncHandler<Object> handler) {
      if (P.containsKey(property)) {
        handler.handle(Future.succeededFuture(P.get(property)));
        return;
      }

      loadCollection(
          Domain.Collection.PROPERTIES,
          new JsonObject(),
          properties -> {
            if (properties.succeeded()) {
              Object value =
                  properties.result().stream()
                      .filter(item -> property.equals(item.getString("id")))
                      .map(item -> item.getValue("value"))
                      .findFirst()
                      .orElse(null);
              P.put(property, value);
              handler.handle(Future.succeededFuture(value));
            } else handler.handle(Future.failedFuture(properties.cause()));
          });
    }

    public static void set(String property, Object value, AsyncHandler<Boolean> handler) {
      Object oldValue = P.get(property);
      if (value == oldValue || value != null && value.equals(oldValue)) {
        handler.handle(Future.succeededFuture(false));
        return;
      }

      mongoDB.findOneAndUpdate(
          Domain.Collection.PROPERTIES,
          ID(property),
          new JsonObject().put("$set", value(value)),
          update -> {
            if (update.succeeded()) {
              P.put(property, value);
              handler.handle(Future.succeededFuture(true));
            } else handler.handle(Future.failedFuture(update.cause()));
          });
    }
  }

  /* EXTENSIONS */

  public static String langOrEN(JsonObject translations, String lang) {
    return translations.getString(lang, translations.getString("en"));
  }

  /* ID */

  public static JsonObject ID(String id) {
    return new JsonObject().put("id", id);
  }

  /* DATETIME */

  public static OffsetDateTime parseDateTime(String value) {
    if (value == null) return null;
    OffsetDateTime dateTime = null;
    try {
      dateTime = OffsetDateTime.parse(value);
    } catch (DateTimeParseException ignored) {
    }
    if (dateTime == null) {
      try {
        dateTime = OffsetDateTime.parse(value + "+00:00");
      } catch (DateTimeParseException ignored) {
      }
    }
    if (dateTime == null) {
      try {
        dateTime = OffsetDateTime.parse(value + "T00:00:00+00:00");
      } catch (DateTimeParseException ignored) {
      }
    }
    return dateTime;
  }

  public static JsonObject mongoDateTime(OffsetDateTime dateTime) {
    return new JsonObject().put("$date", dateTime != null ? dateTime.toString() : null);
  }

  /* FIELDS */

  public static JsonObject fields(Collection<String> fields, Object value) {
    if (value == null) value = 1;
    JsonObject result = new JsonObject();
    for (String field : fields) {
      result.put(field, value);
    }
    return result;
  }

  /* AND */

  public static JsonObject and(Collection<JsonObject> members) {
    List<JsonObject> a =
        members.stream().filter(CommonUtils.NON_EMPTY_JSON_OBJECT).collect(Collectors.toList());
    return a.isEmpty()
        ? new JsonObject()
        : a.size() == 1 ? a.get(0) : new JsonObject().put("$and", new JsonArray(a));
  }

  /* OR */

  public static JsonObject or(Collection<JsonObject> members) {
    List<JsonObject> a =
        members.stream().filter(CommonUtils.NON_EMPTY_JSON_OBJECT).collect(Collectors.toList());
    return a.isEmpty()
        ? new JsonObject()
        : a.size() == 1 ? a.get(0) : new JsonObject().put("$or", new JsonArray(a));
  }

  public static <T> JsonObject or(String field, Collection<T> values) {
    return or(
        values.stream()
            .map(value -> new JsonObject().put(field, value))
            .collect(Collectors.toList()));
  }

  public static <T> JsonObject or(Collection<String> fields, Object value) {
    return or(
        fields.stream()
            .map(field -> new JsonObject().put(field, value))
            .collect(Collectors.toList()));
  }

  /* TEXT */

  public static JsonObject text(String searchText, String languageCode) {
    return new JsonObject()
        .put("$text", new JsonObject().put("$search", searchText).put("$language", languageCode));
  }

  /* TEAM */

  public static void team(String collaborationId, AsyncHandler<JsonObject> handler) {
    mongoDB.findOne(
        COLLABORATION.getCollection(),
        and(Arrays.asList(COLLABORATION.getQuery(), ID(collaborationId))),
        fields(Arrays.asList("team"), 1),
        find -> {
          if (find.succeeded()) {
            handler.handle(Future.succeededFuture(find.result()));
          } else handler.handle(Future.failedFuture(find.cause()));
        });
  }
}
