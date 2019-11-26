package it.beng.modeler.microservice.actions;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import it.beng.microservice.common.AsyncHandler;

public abstract class IncomingAction extends Action {
  public IncomingAction(JsonObject action) {
    super(action);
  }

  protected abstract String innerType();

  @Override
  public boolean isValid() {
    return super.isValid() && type().equals(innerType());
  }

  public abstract void handle(RoutingContext context, AsyncHandler<JsonObject> handler);
}
