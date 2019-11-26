package it.beng.modeler.microservice.actions.diagram.publish;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import it.beng.microservice.common.AsyncHandler;
import it.beng.microservice.common.Countdown;
import it.beng.modeler.microservice.utils.DBUtils;
import it.beng.modeler.microservice.utils.JsonUtils;
import it.beng.modeler.microservice.utils.ProcessEngineUtils;
import it.beng.modeler.model.Domain;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UpdateThingsAction extends AuthorizedAction {

  private static final Logger logger = LogManager.getLogger(UpdateThingsAction.class);

  public static final String TYPE = "[Diagram Action Publish] Update Things";

  public UpdateThingsAction(JsonObject action) {
    super(action);
  }

  @Override
  protected String innerType() {
    return TYPE;
  }

  @Override
  public boolean isValid() {
    return super.isValid() && updates() != null;
  }

  @Override
  protected List<JsonObject> items() {
    return this.updates().stream()
               .filter(item -> item instanceof JsonObject)
               .map(item -> (JsonObject) item)
               .collect(Collectors.toList());
  }

  @Override
  protected void forEach(JsonObject update, AsyncHandler<Void> handler) {
    final JsonObject changes = update.getJsonObject("changes");
    final JsonObject original = update.getJsonObject("original");
    final JsonObject replace = JsonUtils.deepMerge(original, changes);
    final String $domain = original.getString("$domain");
    final boolean isDiagram = Domain.ofDefinition(Domain.Definition.DIAGRAM)
                                    .getDomains()
                                    .contains($domain);
    if (isDiagram) { // TODO: always update diagram lastModified, whatever element is being updated
      replace.put("lastModified", DBUtils.mongoDateTime(OffsetDateTime.now()));
    }
    Domain domain = Domain.get($domain);
    Countdown counter = new Countdown(2).onComplete(complete -> {
      if (complete.succeeded()) {
        handler.handle(Future.succeededFuture());
      } else { handler.handle(Future.failedFuture(complete.cause())); }
    });
    mongodb.findOneAndReplace(
        domain.getCollection(),
        new JsonObject().put("id", original.getString("id")),
        replace,
        findOneAndReplace -> {
          if (findOneAndReplace.succeeded()) {
            counter.next();
          } else {
            counter.fail(findOneAndReplace.cause());
          }
        });

    JsonObject team = changes.getJsonObject("team");
    if (team == null || $domain == null || !isDiagram) {
      counter.next();
    } else {
      ProcessEngineUtils.update(
          new JsonObject()
              .put("original", original)
              .put("changes", new JsonObject().put("team", team)),
          updated -> {
            if (updated.succeeded()) {
              counter.next();
            } else { counter.fail(updated.cause()); }
          });
    }
  }

  public JsonArray updates() {
    return json.getJsonArray("updates");
  }
}
