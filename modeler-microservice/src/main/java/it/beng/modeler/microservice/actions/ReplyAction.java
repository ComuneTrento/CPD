package it.beng.modeler.microservice.actions;

import io.vertx.core.json.JsonObject;

public abstract class ReplyAction extends Action {
  public ReplyAction(JsonObject action) {
    super(action);
  }
}
