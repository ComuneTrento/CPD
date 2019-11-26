package it.beng.modeler.microservice.actions.diagram.publish;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import it.beng.microservice.common.AsyncHandler;
import it.beng.microservice.common.Countdown;
import it.beng.modeler.microservice.actions.diagram.DiagramAction;
import it.beng.modeler.microservice.actions.diagram.DiagramPublishAction;
import it.beng.modeler.microservice.utils.AuthUtils;
import it.beng.modeler.microservice.utils.DBUtils;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public abstract class AuthorizedAction extends DiagramPublishAction implements DiagramAction {

  private static final Collection<String> roles = Arrays.asList("owner", "reviewer", "editor");

  AuthorizedAction(JsonObject action) {
    super(action);
  }

  @Override
  public boolean isValid() {
    return super.isValid() && items() != null;
  }

  @Override
  public void handle(RoutingContext context, AsyncHandler<JsonObject> handler) {
    DBUtils.team(diagramId(), team -> {
      if (team.succeeded()) {
        if (AuthUtils.isAdmin(context.user())) {
          afterAuthorizationIsGranted(handler);
        } else {
          DiagramAction.isPermitted(
              AuthUtils.getAccount(context),
              team.result(),
              roles,
              isPermitted -> {
                if (isPermitted.succeeded()) {
                  if (isPermitted.result()) {
                    afterAuthorizationIsGranted(handler);
                  } else {
                    handler.handle(Future.failedFuture("unauthorized"));
                  }
                } else {
                  handler.handle(Future.failedFuture(isPermitted.cause()));
                }
              });
        }
      } else { handler.handle(Future.failedFuture(team.cause())); }
    });
  }

  private void afterAuthorizationIsGranted(AsyncHandler<JsonObject> handler) {
    final Countdown countdown =
        new Countdown(this.items())
            .onComplete(
                zero -> {
                  handler.handle(Future.succeededFuture(json));
                });
    this.items()
        .forEach(
            item ->
                this.forEach(
                    item,
                    done -> {
                      if (done.failed()) {
                        handler.handle(Future.failedFuture(done.cause()));
                        throw new RuntimeException(done.cause());
                      }
                      countdown.next();
                    }));
  }

  protected abstract List<JsonObject> items();

  protected abstract void forEach(JsonObject item, AsyncHandler<Void> handler);
}
