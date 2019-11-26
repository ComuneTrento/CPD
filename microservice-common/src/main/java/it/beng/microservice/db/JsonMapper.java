package it.beng.microservice.db;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class JsonMapper {

  private BiMap<String, String> mappings;

  public JsonMapper(Map<String, String> mappings) {
    this.mappings = HashBiMap.create(mappings == null ? new HashMap<>() : mappings);
  }

  Map<String, String> mappings() {
    return mappings;
  }

  Map<String, String> inverse() {
    return mappings.inverse();
  }

  public JsonObject encode(JsonObject document) {
    if (document == null) { return null; }
    JsonObject encoded = new JsonObject();
    for (Map.Entry<String, Object> entry : document) {
      String key = entry.getKey();
      if (mappings.containsKey(key)) {
        key = mappings.get(key);
      }
      Object value = entry.getValue();
      if (value instanceof JsonObject) {
        value = encode((JsonObject) value);
      } else if (value instanceof JsonArray) { value = encode((JsonArray) value); }
      encoded.put(key, value);
    }
    return encoded;
  }

  public JsonArray encode(JsonArray document) {
    if (document == null) { return null; }
    JsonArray encoded = new JsonArray();
    for (Object value : document) {
      if (value instanceof JsonObject) {
        value = encode((JsonObject) value);
      } else if (value instanceof JsonArray) { value = encode((JsonArray) value); }
      encoded.add(value);
    }
    return encoded;
  }

  public List<JsonObject> encode(List<JsonObject> documents) {
    if (documents == null) { return null; }
    List<JsonObject> encoded = new LinkedList<>();
    for (JsonObject document : documents) {
      encoded.add(encode(document));
    }
    return encoded;
  }

  public JsonObject decode(JsonObject document) {
    Map<String, String> inverse = mappings.inverse();
    if (document == null) { return null; }
    JsonObject decoded = new JsonObject();
    for (Map.Entry<String, Object> entry : document) {
      String key = entry.getKey();
      if (inverse.containsKey(key)) {
        key = inverse.get(key);
      }
      Object value = entry.getValue();
      if (value instanceof JsonObject) {
        value = decode((JsonObject) value);
      } else if (value instanceof JsonArray) { value = decode((JsonArray) value); }
      decoded.put(key, value);
    }
    return decoded;
  }

  public JsonArray decode(JsonArray document) {
    if (document == null) { return null; }
    JsonArray decoded = new JsonArray();
    for (Object value : document) {
      if (value instanceof JsonObject) {
        value = decode((JsonObject) value);
      } else if (value instanceof JsonArray) { value = decode((JsonArray) value); }
      decoded.add(value);
    }
    return decoded;
  }

  public List<JsonObject> decode(List<JsonObject> documents) {
    if (documents == null) { return null; }
    List<JsonObject> decoded = new LinkedList<>();
    for (JsonObject document : documents) {
      decoded.add(decode(document));
    }
    return decoded;
  }

}