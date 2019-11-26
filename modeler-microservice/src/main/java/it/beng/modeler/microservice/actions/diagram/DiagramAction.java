package it.beng.modeler.microservice.actions.diagram;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import it.beng.microservice.common.AsyncHandler;
import it.beng.modeler.config.cpd;
import it.beng.modeler.microservice.utils.AuthUtils;

import java.util.Collection;

public interface DiagramAction {
  String ADDRESS = cpd.server.eventBus.diagramAddress;
  String COMMAND_PATH = "actions/diagram/";

  static JsonObject diagramQuery(String id) {
    return new JsonObject().put("id", id);
  }

  static void isAuthorized(
      JsonObject account, JsonObject authority, AsyncHandler<Boolean> handler) {
    if (account == null) {
      handler.handle(Future.succeededFuture(false));
    }
    AuthUtils.isAuthorized(authority, account.getJsonObject("roles"), handler);
  }

  static void isPermitted(
      JsonObject account,
      JsonObject collaboration,
      Collection<String> roles,
      AsyncHandler<Boolean> handler) {
    if (account == null) {
      handler.handle(Future.succeededFuture(false));
      return;
    }
    final String userId = account.getString("id");
    final JsonObject team = collaboration.getJsonObject("team");
    for (String role : roles) {
      JsonArray userIds = team.getJsonArray(role);
      if (userIds != null && userIds.contains(userId)) {
        handler.handle(Future.succeededFuture(true));
        return;
      }
    }
    handler.handle(Future.succeededFuture(false));
  }

  static void isEngaged(
      JsonObject account, JsonObject collaboration, AsyncHandler<Boolean> handler) {
    final String userId = account.getString("id");
    for (Object userIds : collaboration.getJsonObject("team").getMap().values()) {
      if (((JsonArray) userIds).contains(userId)) {
        handler.handle(Future.succeededFuture(true));
        return;
      }
    }
    handler.handle(Future.succeededFuture(false));
  }
}
