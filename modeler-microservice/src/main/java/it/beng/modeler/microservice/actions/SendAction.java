package it.beng.modeler.microservice.actions;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import it.beng.microservice.common.AsyncHandler;

public abstract class SendAction extends IncomingAction {
  public SendAction(JsonObject action) {
    super(action);
  }

  protected void reply(ReplyAction action, AsyncHandler<JsonObject> handler) {
    if (action.isValid()) handler.handle(Future.succeededFuture(action.json));
    else
      handler.handle(Future.failedFuture("invalid ReplyAction: " + action.json.encodePrettily()));
  }
}
