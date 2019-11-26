package it.beng.modeler.microservice.actions;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import it.beng.microservice.common.AsyncHandler;

public abstract class PublishAction extends IncomingAction {
  public PublishAction(JsonObject action) {
    super(action);
  }

  @Override
  public void handle(RoutingContext context, AsyncHandler<JsonObject> handler) {
    handler.handle(Future.succeededFuture(json));
  }
}
