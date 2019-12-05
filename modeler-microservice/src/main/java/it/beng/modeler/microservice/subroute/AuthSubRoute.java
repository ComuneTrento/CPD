package it.beng.modeler.microservice.subroute;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.jwt.impl.JWTUser;
import io.vertx.ext.auth.oauth2.AccessToken;
import io.vertx.ext.mongo.UpdateOptions;
import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.UserSessionHandler;
import it.beng.microservice.common.AsyncHandler;
import it.beng.microservice.common.Countdown;
import it.beng.modeler.config.cpd;
import it.beng.modeler.config.cpd.OAuth2Config;
import it.beng.modeler.microservice.http.JsonResponse;
import it.beng.modeler.microservice.subroute.auth.LocalAuthSubRoute;
import it.beng.modeler.microservice.subroute.auth.OAuth2AuthCodeSubRoute;
import it.beng.modeler.microservice.subroute.auth.OAuth2ClientSubRoute;
import it.beng.modeler.microservice.subroute.auth.OAuth2ImplicitSubRoute;
import it.beng.modeler.microservice.subroute.auth.OAuth2SubRoute;
import it.beng.modeler.microservice.utils.AuthUtils;
import it.beng.modeler.model.Domain.Collection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class is a member of <strong>modeler-microservice</strong> project.
 *
 * @author vince
 */
public final class AuthSubRoute extends VoidSubRoute {

  private static final Logger logger = LogManager.getLogger(AuthSubRoute.class);

  static List<String> knownProviders = new LinkedList<>();

  public static class AAC {

    private static OAuth2Config config;
    private static String host;

    public static OAuth2Config getConfig() {
      if (config == null) {
        config = cpd.oauth2.configs.stream()
                                   .filter(config -> "AAC".equals(config.provider))
                                   .findAny()
                                   .orElse(null);
      }
      return config;
    }

    public static String getHost() {
      if (host == null) {
        try {
          host = new URI(getConfig().site).getHost();
        } catch (URISyntaxException ignored) { }
      }
      return host;
    }

    public static void revoke(String accessToken, AsyncHandler<Void> handler) {
      String endpoint = getConfig().revokeEndpoint.replace("{access_token}", accessToken);
      final WebClient client = AuthUtils.createWebClient(cpd.vertx());
      client.requestAbs(HttpMethod.GET, getConfig().site + endpoint)
            .putHeader("Host", getHost())
            .send(response -> {
              client.close();
              if (response.succeeded()) {
                logger.debug("TOKEN " + accessToken + " revoked!");
                handler.handle(Future.succeededFuture());
              } else {
                handler.handle(Future.failedFuture(response.cause()));
              }
            });
    }

    public static void refresh(String refreshToken, AsyncHandler<JsonObject> handler) {
      String endpoint = getConfig().tokenPath;
      final WebClient client = AuthUtils.createWebClient(cpd.vertx());
      client.requestAbs(HttpMethod.POST, getConfig().site + endpoint)
            .putHeader("Host", getHost())
            .putHeader("Accept", "application/json")
            .putHeader("Content-Type", "application/x-www-form-urlencoded")
            .sendForm(MultiMap.caseInsensitiveMultiMap()
                              .add("client_id", getConfig().clientId)
                              .add("client_secret", getConfig().clientSecret)
                              .add("refresh_token", refreshToken)
                              .add("grant_type", "refresh_token"), response -> {
              client.close();
              if (response.failed()) {
                handler.handle(Future.failedFuture(response.cause()));
              } else {
                JsonObject body = response.result().bodyAsJsonObject();
                String error = body.getString("error_description");
                if (error == null) {
                  JsonObject exception = body.getJsonObject("exception");
                  if (exception != null) {
                    error = exception.getString("error_description");
                  }
                }
                if (error != null) {
                  handler.handle(Future.failedFuture(error));
                } else {
                  handler.handle(Future.succeededFuture(response.result().bodyAsJsonObject()));
                }
              }
            });
    }
  }

  public AuthSubRoute(Vertx vertx, Router router) {
    super(cpd.server.auth.path, vertx, router, false);
  }

  public static void checkEncodedStateStateCookie(RoutingContext context, String encodedState) {
    if (encodedState == null || !encodedState.equals(context.session().get("encodedState"))) {
      logger.error(HttpResponseStatus.NOT_FOUND.toString());
      throw new IllegalStateException("invalid login transaction");
    }
    logger.debug("state check successful");
  }

  @Override
  protected void init() {

    // local/login
    if (cpd.app.useLocalAuth) {
      knownProviders.add(LocalAuthSubRoute.PROVIDER);
      new LocalAuthSubRoute(vertx, router);
    }

    if (cpd.JWT() != null) {
      // create JWT user session handler
      final UserSessionHandler jwtUserSessionHandler = UserSessionHandler.create(cpd.JWT());
      router.route().handler(jwtUserSessionHandler);
    }

    // oauth2provider/login
    for (cpd.OAuth2Config oAuth2Config : cpd.oauth2.configs) {
      knownProviders.add(oAuth2Config.provider);
      for (final String flowType : oAuth2Config.flows.keySet()) {
        switch (flowType) {
          case OAuth2AuthCodeSubRoute.FLOW_TYPE:
            new OAuth2AuthCodeSubRoute(vertx, router, oAuth2Config);
            break;
          case OAuth2ClientSubRoute.FLOW_TYPE:
            new OAuth2ClientSubRoute(vertx, router, oAuth2Config);
            break;
          case OAuth2ImplicitSubRoute.FLOW_TYPE:
            new OAuth2ImplicitSubRoute(vertx, router, oAuth2Config);
            break;
          case "PASSWORD":
            logger.warn("PASSWORD oauth2 flow type not yet implemented");
            continue;
          case "AUTH_JWT":
            logger.warn("AUTH_JWT oauth2 flow type not yet implemented");
            continue;
          default:
            logger.warn(
                "Provider '" + oAuth2Config.provider
                    + "' is unknown and will not be available.");
            continue;
        }
        logger.info(
            "Provider '" + oAuth2Config.provider + "' will follow the '" + flowType
                + "' flow.");
      }
    }

    logger.debug("known providers are " + knownProviders);

    router.route(path + "login/:provider").handler(this::login);

    router.route(HttpMethod.DELETE, path + "user/deleteData").handler(this::deleteUserData);

    router.route(HttpMethod.GET, path + "logout").handler(this::logout);

    /* API */

    // getOAuth2Providers
    router.route(HttpMethod.GET, path + "oauth2/providers").handler(this::getOAuth2Providers);
    // getUser
    router.route(HttpMethod.GET, path + "user").handler(this::getUser);
    // getUserIsAuthenticated
    router.route(HttpMethod.GET, path + "user/isAuthenticated")
          .handler(this::getUserIsAuthenticated);
    // getUserHasAccess
    // router.route(HttpMethod.GET, path +
    // "user/hasAccess/:accessRole").handler(this::getUserHasAccess);
    // getUserIsAuthorized
    // router.route(HttpMethod.GET, path +
    // "user/isAuthorized/:contextName/:contextId/:contextRole").handler(this::getUserIsAuthorized);

    // getAccounts
    router.route(HttpMethod.GET, path + "accounts").handler(this::getAccounts);
    router.route(HttpMethod.PUT, path + "accounts").handler(this::putAccounts);
  }

  private void login(RoutingContext context) {
    String provider = context.pathParam("provider");
    if (!knownProviders.contains(provider)) {
      context.fail(HttpResponseStatus.UNAUTHORIZED.code());
      return;
    }
    JsonObject loginState =
        new JsonObject().put("loginId", UUID.randomUUID().toString()).put("provider", provider);
    List<String> redirect = context.queryParam("redirect");
    if (redirect != null && redirect.size() > 0) {
      try {
        loginState.put("redirect", base64.decode(redirect.get(0)));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    if (loginState.getValue("redirect") == null) {
      loginState.put("redirect", "/");
    }
    logger.debug("login state: " + loginState.encodePrettily());

    if ("local".equals(provider)) {
      context.put("loginState", loginState);
    } else {
      JsonObject state = new JsonObject().put("loginState", loginState);
      context.session().put("encodedState", base64.encode(state.encode()));
    }
    context.reroute(path + provider + "/login/handler");
  }

  private void deleteUserData(RoutingContext context) {
    @Nullable User user = context.user();
    if (user == null) {
      context.response().end();
      return;
    }
    final String userId = AuthUtils.getAccount(user).getString("id");
    final Countdown counter = new Countdown(2).onComplete(complete -> {
      context.response().end();
    });
    mongodb.updateCollectionWithOptions(Collection.USER_FEEDBACKS,
                                        new JsonObject().put("userId", userId),
                                        new JsonObject().put("$set", new JsonObject()
                                            .put("userId", "anonymous")),
                                        new UpdateOptions().setMulti(true), update -> {
          if (update.failed()) {
            counter.fail(update.cause());
          } else {
            counter.next();
            mongodb.findOneAndDelete(Collection.USERS,
                                     new JsonObject().put("id", userId), delete -> {
                  if (delete.failed()) {
                    counter.fail(delete.cause());
                  } else {
                    counter.next();
                  }
                });
          }
        });
  }

  public void logout(RoutingContext context) {
    logger.debug("logging out " + (context.user() == null
                                   ? "<NULL>"
                                   : context.user().getClass().getSimpleName()
                                       + ": " + context.user().principal().encodePrettily()));
    if (context.user() instanceof JWTUser || context.user() instanceof AccessToken) {
      // revoke token (https://<aac server>/aac/eauth/revoke/{access_token})
      AAC.revoke(context.user().principal().getString("access_token"), revoke -> {
        if (revoke.failed()) {
          logger.error(revoke.cause());
        }
        destroyUser(context);
        context.response().end();
      });
    } else {
      destroyUser(context);
      context.response().end();
    }
  }

  private static void destroyUser(RoutingContext context) {
    context.session().destroy();
    context.clearUser();
    context.removeCookie(cpd.JWT_TOKEN);
  }

  private void getOAuth2Providers(RoutingContext context) {
    JsonArray providers = new JsonArray();
    for (cpd.OAuth2Config providerConfig : cpd.oauth2.configs) {
      providers.add(
          new JsonObject()
              .put("provider", providerConfig.provider)
              .put("logoUrl", providerConfig.logoUrl));
    }
    new JsonResponse(context).end(providers);
  }

  private void getUserIsAuthenticated(RoutingContext context) {
    new JsonResponse(context).end(context.user() != null);
  }

  private void getUser(RoutingContext context) {
    User user = context.user();
    if (user != null) {
      JsonObject principal = null;
      if (AuthUtils.getAccount(user) != null) {
        principal = context.user().principal();
      } else {
        context.clearUser();
      }
      new JsonResponse(context).end(principal);
    } else {
      // try to login from jwt
      jwtLogin(context, login -> {
        JsonObject principal = null;
        if (login.succeeded()) {
          // ask aac to validate the token
          principal = login.result();
        }
        new JsonResponse(context).end(principal);
      });
    }
  }

  private void jwtLogin(RoutingContext context, AsyncHandler<JsonObject> handler) {
    Optional<Cookie> token = context.cookies().stream()
                                    .filter(cookie -> cpd.JWT_TOKEN.equals(cookie.getName()))
                                    .findFirst();
    if (token.isPresent()) {
      String value = token.get().getValue();
      cpd.JWT().authenticate(new JsonObject().put("jwt", value), jwtAuth -> {
        if (jwtAuth.succeeded()) {
          JWTUser user = (JWTUser) jwtAuth.result();
          AAC.refresh(user.principal().getString("refresh_token"), refresh -> {
            if (refresh.succeeded()) {
              logger.debug("REFRESH: " + refresh.result().encodePrettily());
              user.principal().mergeIn(refresh.result());
              context.setUser(user);
              Session session = context.session();
              if (session != null) {
                // the user has upgraded from unauthenticated to authenticated
                // session should be upgraded as recommended by owasp
                session.regenerateId();
              }
              handler.handle(Future.succeededFuture(user.principal()));
            } else {
              logger.debug("REFRESH ERROR: " + refresh.cause().getMessage());
              destroyUser(context);
              handler.handle(Future.succeededFuture(null));
            }
          });
        } else {
          handler.handle(Future.failedFuture(jwtAuth.cause()));
        }
      });
    } else {
      handler.handle(Future.succeededFuture(null));
    }
  }

  private void getAccounts(RoutingContext context) {
    if (isAdminOtherwiseFail(context)) {
      mongodb.find(
          "users",
          new JsonObject(),
          users -> {
            if (users.succeeded()) {
              new JsonResponse(context).end(users.result());
            } else {
              context.fail(users.cause());
            }
          });
    }
  }

  private void putAccounts(RoutingContext context) {
    if (isAdminOtherwiseFail(context)) {
      JsonObject account = context.getBodyAsJson();
      String id = (String) account.remove("id");
      if (id == null) {
        context.fail(new NullPointerException());
        return;
      }
      JsonObject query = new JsonObject().put("id", id);
      mongodb.findOneAndReplace(
          "users",
          query,
          account,
          update -> {
            if (update.succeeded()) {
              JsonObject userAccount = AuthUtils.getAccount(context.user());
              if (userAccount.getString("id").equals(account.getString("id"))) {
                userAccount.mergeIn(account);
                OAuth2SubRoute.createJwtToken(context);
              }
              new JsonResponse(context).end(update.result());
              logger.debug("Account UPDATED: " + account.encodePrettily());
            } else {
              context.fail(update.cause());
            }
          });
    }
  }

}
