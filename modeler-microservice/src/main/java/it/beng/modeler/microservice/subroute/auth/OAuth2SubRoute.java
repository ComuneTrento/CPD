package it.beng.modeler.microservice.subroute.auth;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2ClientOptions;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.jwt.JWTOptions;
import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.UserSessionHandler;
import it.beng.microservice.common.AsyncHandler;
import it.beng.modeler.config.cpd;
import it.beng.modeler.microservice.subroute.SubRoute;
import it.beng.modeler.microservice.utils.AuthUtils;
import it.beng.modeler.microservice.utils.CommonUtils;
import it.beng.modeler.model.Domain;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is a member of <strong>modeler-microservice</strong> project.
 *
 * @author vince
 */
public abstract class OAuth2SubRoute extends SubRoute<OAuth2SubRoute.Config> {

  protected static final String ID = "id";
  private static final String PROVIDER = "provider";
  private static final String FIRST_NAME = "firstName";
  private static final String LAST_NAME = "lastName";
  private static final String DISPLAY_NAME = "displayName";
  private static final String ROLES = "roles";
  private static final Map<String, Map<String, String>> ACCOUNT_PROVIDERS = new HashMap<>();

  static {
    Map<String, String> provider;
    /* Google */
    provider = new HashMap<>();
    provider.put(ID, "id");
    provider.put(FIRST_NAME, "given_name");
    provider.put(LAST_NAME, "family_name");
    ACCOUNT_PROVIDERS.put("Google", provider);
    /* AAC */
    provider = new HashMap<>();
    provider.put(ID, "userId");
    provider.put(FIRST_NAME, "name");
    provider.put(LAST_NAME, "surname");
    ACCOUNT_PROVIDERS.put("AAC", provider);
  }

  public static final String AUTHORIZATION_CODE_GRANT = "AUTHORIZATION_CODE_GRANT";
  public static final String CLIENT_CREDENTIALS_GRANT = "CLIENT_CREDENTIALS_GRANT";
  public static final String IMPLICIT_GRANT = "IMPLICIT_GRANT";

  protected cpd.OAuth2Config oauth2Config;
  protected cpd.OAuth2Config.Flow oauth2Flow;
  protected OAuth2ClientOptions oauth2ClientOptions;
  protected OAuth2Auth oauth2Provider;

  static class Config {

    cpd.OAuth2Config oauth2Config;
    String oauth2FlowType;

    Config(cpd.OAuth2Config oauth2Config, String oauth2FlowType) {
      this.oauth2Config = oauth2Config;
      this.oauth2FlowType = oauth2FlowType;
    }
  }

  public OAuth2SubRoute(
      Vertx vertx, Router router, cpd.OAuth2Config oauth2Config, String flowType) {
    super(
        cpd.server.auth.path + oauth2Config.provider + "/",
        vertx,
        router,
        false,
        new OAuth2SubRoute.Config(oauth2Config, flowType));
  }

  protected abstract void init();

  private static OAuth2FlowType flowType(String flowType) {
    if (flowType != null) {
      try {
        return OAuth2FlowType.valueOf(flowType);
      } catch (IllegalArgumentException ignored) {
      }
    }
    return OAuth2FlowType.AUTH_CODE;
  }

  @Override
  protected final void init(Config oauth2SubRouteConfig) {

    this.oauth2Config = oauth2SubRouteConfig.oauth2Config;
    this.oauth2Flow =
        oauth2SubRouteConfig.oauth2Config.flows.get(oauth2SubRouteConfig.oauth2FlowType);

    this.oauth2ClientOptions =
        new OAuth2ClientOptions(new HttpClientOptions().setSsl(true))
            .setSite(oauth2Config.site)
            .setClientID(oauth2Config.clientId)
            .setClientSecret(oauth2Config.clientSecret)
            .setScopeSeparator(" ");
    if (this.oauth2Config.authPath != null) {
      oauth2ClientOptions.setAuthorizationPath(oauth2Config.authPath);
    }
    if (this.oauth2Config.tokenPath != null) {
      oauth2ClientOptions.setTokenPath(oauth2Config.tokenPath);
    }
    if (this.oauth2Config.introspectionPath != null) {
      oauth2ClientOptions.setIntrospectionPath(oauth2Config.introspectionPath);
    }

    JsonObject headers = oauth2ClientOptions.getHeaders();
    if (headers == null) { headers = new JsonObject(); }

    oauth2ClientOptions.setHeaders(headers.put("Accept", "application/json"));

    // create OAuth2 Provider
    oauth2Provider =
        OAuth2Auth.create(
            vertx, oauth2ClientOptions.setFlow(flowType(oauth2SubRouteConfig.oauth2FlowType)));

    // create OAuth2 user session handler
    final UserSessionHandler oauth2UserSessionHandler = UserSessionHandler.create(oauth2Provider);
    router.route().handler(oauth2UserSessionHandler);

    init();
  }

  void getOrCreateAccount(
      final JsonObject userInfo, final String provider, AsyncHandler<JsonObject> handler) {

    if (userInfo == null || provider == null) {
      handler.handle(Future.failedFuture("cannot determine user info"));
      return;
    }

    final Map<String, String> accountProvider = ACCOUNT_PROVIDERS.get(provider);
    if (accountProvider == null) {
      handler.handle(Future.failedFuture("cannot determine account provider"));
      return;
    }

    final String id = userInfo.getString(accountProvider.get(ID), null);
    if (id == null) {
      handler.handle(Future.failedFuture("cannot determine user id"));
      return;
    }

    mongodb.findOne(
        Domain.Collection.USERS,
        new JsonObject().put(ID, id).put(PROVIDER, provider),
        new JsonObject(),
        find -> {
          if (find.succeeded()) {
            final JsonObject account = CommonUtils.coalesce(find.result(), new JsonObject());
            if (id.equals(account.getString("id"))) {
              handler.handle(Future.succeededFuture(account));
            } else {
              final String firstName =
                  userInfo.getString(accountProvider.get(FIRST_NAME), "Guest").trim();
              final String lastName = userInfo.getString(accountProvider.get(LAST_NAME), "").trim();
              final String displayName = (firstName + " " + lastName).trim();
              account
                  .put(ID, id)
                  .put(PROVIDER, provider)
                  .put(FIRST_NAME, firstName)
                  .put(LAST_NAME, lastName)
                  .put(DISPLAY_NAME, displayName)
                  .put(ROLES, AuthUtils.LOGGED_IN_CITIZEN_ROLES);
              mongodb.save(
                  Domain.Collection.USERS,
                  account,
                  save -> {
                    if (save.succeeded()) {
                      handler.handle(Future.succeededFuture(account));
                    } else {
                      handler.handle(Future.failedFuture(save.cause()));
                      return;
                    }
                  });
            }
          } else {
            handler.handle(Future.failedFuture(find.cause()));
          }
        });
  }

  public static void createJwtToken(RoutingContext context) {
    if (cpd.JWT() == null || context.user() == null) {
      return;
    }
    User user = context.user();
    // create the JWT token
    String token = cpd.JWT().generateToken(
        user.principal(),
        new JWTOptions().setExpiresInSeconds(
            user.principal().getInteger("expires_in", 3600)
        )
    );

    Cookie cookie = Cookie.cookie(cpd.JWT_TOKEN, token)
                          .setPath("/")
                          .setHttpOnly(true)
                          .setSecure(cpd.server.isSecure())
                          .setMaxAge(3600L * 24 * 14); // 14 days
    context.addCookie(cookie);
  }

}
