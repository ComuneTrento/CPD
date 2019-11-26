package it.beng.modeler.microservice.utils;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import it.beng.microservice.common.AsyncHandler;
import it.beng.modeler.config.cpd;
import it.beng.modeler.model.Domain;
import java.util.Arrays;
import javax.security.auth.login.AccountNotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class AuthUtils {

  private static final Logger logger = LogManager.getLogger(AuthUtils.class);

  public static final JsonObject LOGGED_OUT_CITIZEN_ROLES =
      new JsonObject()
          .put("system", "guest")
          .put("interaction", "citizen")
          .put("things", new JsonObject());

  public static final JsonObject LOGGED_IN_CITIZEN_ROLES =
      new JsonObject()
          .put("system", "user")
          .put("interaction", "citizen")
          .put("things", new JsonObject());

  public static boolean isAdmin(User user) {
    return user != null
        && "admin".equals(getAccount(user).getJsonObject("roles").getString("system"));
  }

  public static void isAuthorized(
      JsonObject authority, JsonObject userRoles, AsyncHandler<Boolean> handler) {
    logger.debug(
        "checking authority " + authority.encodePrettily() + " vs. " + userRoles.encodePrettily());
    final JsonArray authoritySystem = authority.getJsonArray("system");
    final JsonArray authorityInteraction = authority.getJsonArray("interaction");
    try {
      handler.handle(
          Future.succeededFuture(
              (authoritySystem == null || authoritySystem.contains(userRoles.getString("system")))
                  && (authorityInteraction == null
                  || authorityInteraction.contains(userRoles.getString("interaction")))));
    } catch (Exception e) {
      handler.handle(Future.failedFuture(e));
    }
  }

  public static void isEngaged(User user, String collaborationId,
                               Handler<Future<Boolean>> handler) {
    if (user == null || collaborationId == null) {
      handler.handle(Future.failedFuture(new NullPointerException()));
      return;
    }
    if (isAdmin(user)) {
      handler.handle(Future.succeededFuture(true));
      return;
    }
    final JsonObject query =
        DBUtils.or(
            Arrays.asList("team.owner", "team.reviewer", "team.editor", "team.observer"),
            getAccount(user).getString("id"))
               .put("id", collaborationId);
    cpd.dataDB()
       .findOne(
           Domain.ofDefinition(Domain.Definition.DIAGRAM).getCollection(),
           query,
           new JsonObject(),
           findOne -> {
             if (findOne.succeeded()) {
               handler.handle(Future.succeededFuture(findOne.result() != null));
             } else {
               handler.handle(Future.failedFuture(findOne.cause()));
             }
           });
  }

  public static JsonObject getAccount(User user) {
    return user != null && user.principal() != null
           ? user.principal().getJsonObject("account")
           : null;
  }

  public static JsonObject getAccount(RoutingContext context) {
    return context != null ? getAccount(context.user()) : null;
  }

  public static void checkUserAccount(User user) throws AccountNotFoundException {
    JsonObject account = getAccount(user);
    if (account == null) {
      throw new AccountNotFoundException("illegal user after login");
    }

    // check if user is the default administrator and sets his role accordingly
    if (cpd.server.adminId != null && cpd.server.adminId.equals(account.getString("id"))) {
      JsonObject roles = account.getJsonObject("roles");
      if (roles == null) {
        roles = new JsonObject();
        account.put("roles", roles);
      }
      roles.put("system", "admin");
    }

    // IdentityService id = config.processEngine().getIdentityService();

    logger.info("user " + account.getString("displayName") + " has logged in");
  }

  public static WebClient createWebClient(Vertx vertx, boolean followRedirects) {
    return WebClient.create(
        vertx,
        new WebClientOptions().setUserAgent("CPD-WebClient/1.0").setFollowRedirects(followRedirects)
    );
  }

  public static WebClient createWebClient(Vertx vertx) {
    return createWebClient(vertx, false);
  }

}
