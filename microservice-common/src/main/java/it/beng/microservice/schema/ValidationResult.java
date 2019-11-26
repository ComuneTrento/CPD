package it.beng.microservice.schema;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * <p>This class is a member of <strong>microservice-stack</strong> project.</p>
 *
 * @author vince
 */
public class ValidationResult {

  private final JsonObject validation;

  public ValidationResult(boolean isValid, JsonArray errors) {
    this.validation = new JsonObject()
        .put("isValid", isValid)
        .put("errors", errors != null ? errors : new JsonArray());
  }

  public boolean isValid() {
    return validation.getBoolean("isValid");
  }

  public JsonArray errors() {
    return validation.getJsonArray("errors");
  }

  public JsonObject toJson() {
    return validation.copy();
  }

}
