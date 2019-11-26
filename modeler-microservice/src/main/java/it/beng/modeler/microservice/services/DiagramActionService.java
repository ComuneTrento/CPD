package it.beng.modeler.microservice.services;

import io.vertx.core.Vertx;
import it.beng.modeler.microservice.actions.diagram.DiagramAction;
import it.beng.modeler.microservice.actions.diagram.publish.CreateThingsAction;
import it.beng.modeler.microservice.actions.diagram.publish.DeleteThingsAction;
import it.beng.modeler.microservice.actions.diagram.publish.UpdateThingsAction;
import it.beng.modeler.microservice.actions.diagram.send.LoadDefinitionAction;

public class DiagramActionService extends ActionService {

  // TODO: creare un modello "Initial FPMN" da usare come template nella creazione di un nuovo
  // diagramma

  // register here all handled IncomingActions (Publish/Send)
  static {
    /* PUBLISH */
    registerIncomingAction(CreateThingsAction.TYPE, CreateThingsAction.class);
    registerIncomingAction(UpdateThingsAction.TYPE, UpdateThingsAction.class);
    registerIncomingAction(DeleteThingsAction.TYPE, DeleteThingsAction.class);
    /* SEND */
    registerIncomingAction(LoadDefinitionAction.TYPE, LoadDefinitionAction.class);
  }

  DiagramActionService(Vertx vertx) {
    super(vertx);
  }

  @Override
  protected String address() {
    return DiagramAction.ADDRESS;
  }
}
