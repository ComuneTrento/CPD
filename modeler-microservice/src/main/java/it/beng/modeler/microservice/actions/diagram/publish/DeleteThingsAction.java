package it.beng.modeler.microservice.actions.diagram.publish;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import it.beng.microservice.common.AsyncHandler;
import it.beng.modeler.model.Domain;

import java.util.List;
import java.util.stream.Collectors;

public class DeleteThingsAction extends AuthorizedAction {
  public static final String TYPE = "[Diagram Action Publish] Delete Things";

  public DeleteThingsAction(JsonObject action) {
    super(action);
  }

  @Override
  protected String innerType() {
    return TYPE;
  }

  @Override
  public boolean isValid() {
    return super.isValid() && things() != null;
  }

  @Override
  protected List<JsonObject> items() {
    return this.things().stream()
        .filter(item -> item instanceof JsonObject)
        .map(item -> (JsonObject) item)
        .collect(Collectors.toList());
  }

  @Override
  protected void forEach(JsonObject item, AsyncHandler<Void> handler) {
    mongodb.removeDocument(
        Domain.get(item.getString("$domain")).getCollection(),
        new JsonObject().put("id", item.getString("id")),
        delete -> {
          if (delete.failed()) {
            handler.handle(Future.failedFuture(delete.cause()));
          } else {
            handler.handle(Future.succeededFuture());
          }
        });
  }

  public JsonArray things() {
    return json.getJsonArray("things");
  }
}
