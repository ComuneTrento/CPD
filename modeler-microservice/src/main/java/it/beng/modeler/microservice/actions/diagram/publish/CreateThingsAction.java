package it.beng.modeler.microservice.actions.diagram.publish;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import it.beng.microservice.common.AsyncHandler;
import it.beng.modeler.config.cpd;
import it.beng.modeler.model.Domain;

import java.util.List;
import java.util.stream.Collectors;

public class CreateThingsAction extends AuthorizedAction {
  public static final String TYPE = "[Diagram Action Publish] Create Things";

  public CreateThingsAction(JsonObject action) {
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
  protected void forEach(JsonObject thing, AsyncHandler<Void> handler) {
    final String langCode = thing.getString("language");
    if (langCode != null) {
      thing.put("language", cpd.language(langCode));
    }
    mongodb.save(
        Domain.get(thing.getString("$domain")).getCollection(),
        thing,
        create -> {
          if (create.failed()) {
            handler.handle(Future.failedFuture(create.cause()));
          } else {
            handler.handle(Future.succeededFuture());
          }
        });
  }

  public JsonArray things() {
    return json.getJsonArray("things");
  }
}
