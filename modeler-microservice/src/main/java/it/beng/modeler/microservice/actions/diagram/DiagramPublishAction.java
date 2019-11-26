package it.beng.modeler.microservice.actions.diagram;

import io.vertx.core.json.JsonObject;
import it.beng.modeler.microservice.actions.PublishAction;

/**
 * This class is a member of <strong>modeler-microservice</strong> project.
 *
 * @author vince
 */
public abstract class DiagramPublishAction extends PublishAction implements DiagramAction {

  public DiagramPublishAction(JsonObject action) {
    super(action);
  }

  public String diagramId() {
    return json.getString("diagramId");
  }

  @Override
  public boolean isValid() {
    return super.isValid() && diagramId() != null;
  }
}
