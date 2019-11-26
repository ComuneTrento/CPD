package it.beng.modeler.microservice.actions.diagram.reply;

import io.vertx.core.json.JsonObject;
import it.beng.modeler.microservice.actions.ReplyAction;
import it.beng.modeler.microservice.actions.diagram.DiagramAction;

public class DefinitionLoadedAction extends ReplyAction implements DiagramAction {

  public static final String TYPE = "[Diagram Action Reply] Definition Loaded";

  public DefinitionLoadedAction(JsonObject definition) {
    super(new JsonObject().put("address", ADDRESS).put("type", TYPE).put("definition", definition));
  }

  @Override
  public boolean isValid() {
    return super.isValid() && definition() != null;
  }

  public JsonObject definition() {
    return json.getJsonObject("definition");
  }
}
