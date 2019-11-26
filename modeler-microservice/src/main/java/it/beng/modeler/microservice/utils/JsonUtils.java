package it.beng.modeler.microservice.utils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Arrays;
import java.util.List;

/**
 * This class is a member of <strong>modeler-microservice</strong> project.
 *
 * @author vince
 */
public final class JsonUtils {

  public static JsonObject deepClone(JsonObject original) {
    return new JsonObject(original.encode());
  }

  public static JsonObject deepMerge(JsonObject... items) {
    return Arrays.stream(items)
                 .reduce(new JsonObject(), (result, item) -> result.mergeIn(deepClone(item), true));
  }

  public static <T> T firstOrNull(JsonArray jsonArray) {
    return jsonArray != null && jsonArray.size() > 0 ? (T) jsonArray.getValue(0) : null;
  }

  public static <T> T firstOrNull(List<T> list) {
    return list != null && list.size() > 0 ? list.get(0) : null;
  }
}
