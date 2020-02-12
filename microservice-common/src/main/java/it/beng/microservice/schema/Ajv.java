package it.beng.microservice.schema;

import com.coveo.nashorn_modules.Require;
import com.coveo.nashorn_modules.ResourceFolder;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.HashMap;
import java.util.Map;
import javax.script.ScriptException;
import jdk.nashorn.api.scripting.NashornScriptEngine;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
final class Ajv {

  private static final Logger logger = LogManager.getLogger(Ajv.class);

  private static final String $SCHEMA = "http://json-schema.org/draft-06/schema#";
  private static final ResourceFolder ROOT_FOLDER = ResourceFolder
      .create(Ajv.class.getClassLoader(),
          "it/beng/microservice/schema",
          "UTF-8");

  final Map<String, JsonObject> schemas = new HashMap<>();
  private final NashornScriptEngine engine;

  Ajv() {
    logger.info("initializing Ajv... ");
    this.engine = (NashornScriptEngine) new NashornScriptEngineFactory().getScriptEngine("-strict",
        "--no-java",
        "--no-syntax-extensions");
    try {
      Require.enable(engine, ROOT_FOLDER);
      engine.eval(ROOT_FOLDER.getFile("main.js"));
    } catch (ScriptException e) {
      throw new IllegalStateException(e);
    }
    logger.info("Ajv initialized.");
  }

  public ValidationResult validate(String $id, Object value) throws ScriptException {
      if ($id == null) { throw new ScriptException("$id cannot be null"); }
    boolean encoded = false;
    // encoded = true only if value is a "container instance", in this case a json representation is passed
    if (value instanceof JsonObject) {
      value = ((JsonObject) value).encode();
      encoded = true;
    } else if (value instanceof JsonArray) {
      value = ((JsonArray) value).encode();
      encoded = true;
    }
    try {
      final ScriptObjectMirror result = (ScriptObjectMirror) engine.invokeFunction("validate",
          $id,
          value,
          encoded);
      boolean isValid = (Boolean) result.getMember("isValid");
      JsonArray errors = null;
      if (!isValid) {
        errors = new JsonArray();
        ScriptObjectMirror resultErrors = (ScriptObjectMirror) result.getMember("errors");
        int len = resultErrors.size();
        for (int i = 0; i < len; i++) {
          errors.add(resultErrors.get("" + i));
        }
      }
      return new ValidationResult(isValid, errors);
    } catch (NoSuchMethodException e) {
      throw new ScriptException(e);
    }
  }

  public ValidationResult validateSchema(JsonObject schema) throws ScriptException {
    return validate($SCHEMA, schema);
  }

  public ValidationResult addSchema(JsonObject schema) throws ScriptException {
    final String $id = schema.getString("$id");
      if ($id == null) { throw new ScriptException("$id cannot be null"); }
    final ValidationResult validation = validate($SCHEMA, schema);
    if (validation.isValid()) {
      removeSchema($id);
      try {
        engine.invokeFunction("addSchema", schema.encode());
        schemas.put($id, schema);
        logger.info("schema '" + schema.getString("$id") + "' added.");
      } catch (NoSuchMethodException e) {
        logger.warn("schema '" + schema.getString("$id") + "' could not be added!");
        throw new ScriptException(e);
      }
    }
    return validation;
  }

  public void removeSchema(String $id) throws ScriptException {
      if ($id == null) { throw new ScriptException("$id cannot be null"); }
    try {
      engine.invokeFunction("removeSchema", $id);
      schemas.remove($id);
    } catch (NoSuchMethodException e) {
      throw new ScriptException(e);
    }
  }

  public boolean containsSchema(String $id) throws ScriptException {
      if ($id == null) { throw new ScriptException("$id cannot be null"); }
    try {
      return (boolean) engine.invokeFunction("containsSchema", $id);
    } catch (NoSuchMethodException e) {
      throw new ScriptException(e);
    }
  }

}
