package it.beng.modeler.microservice.subroute.auth;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.impl.NoStackTraceThrowable;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.AccessToken;
import io.vertx.ext.auth.oauth2.impl.OAuth2TokenImpl;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import it.beng.modeler.config.cpd;
import it.beng.modeler.microservice.http.JsonResponse;
import it.beng.modeler.microservice.subroute.AuthSubRoute;
import it.beng.modeler.microservice.utils.AuthUtils;
import javax.security.auth.login.AccountNotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class is a member of <strong>modeler-microservice</strong> project.
 *
 * @author vince
 */
public final class OAuth2ImplicitSubRoute extends OAuth2SubRoute {

  private static final Logger logger = LogManager.getLogger(OAuth2ImplicitSubRoute.class);

  public static final String FLOW_TYPE = "IMPLICIT";

  public OAuth2ImplicitSubRoute(Vertx vertx, Router router, cpd.OAuth2Config oAuth2Config) {
    super(vertx, router, oAuth2Config, FLOW_TYPE);
  }

  /*
      https://simpatico.business-engineering.it/cpd/oauth2/server/callback,
      https://localhost:8901/cpd/oauth2/client/callback,
      https://simpatico.business-engineering.it/cpd/oauth2/client/callback,
      https://localhost:8901/cpd/oauth2/server/callback

  */

  @Override
  protected void init() {
    router.route(HttpMethod.GET, path + "login/handler").handler(this::loginHandler);
    router.route(HttpMethod.GET, baseHref + "oauth2/client/callback")
          .handler(context -> {
            // TODO: check why reroute keeps the hash segment in safari
            context.reroute(cpd.server.appPath(context) + "login/oauth2/client/callback");
            /* this doesn't work in safari as it cleans the hash fragment on redirects... */
            // redirect(context, cpd.server.appPath(context) + "login/oauth2/client/callback");
          });
    router.route(HttpMethod.POST, path + "hash").handler(this::loginWithHash);
  }

  private void loginHandler(RoutingContext context) {
    StringBuilder url =
        new StringBuilder(
            oauth2ClientOptions.getSite() + oauth2ClientOptions.getAuthorizationPath())
            .append("?")
            .append("response_type=token")
            .append("&")
            .append("grant_type=implicit")
            .append("&")
            .append("redirect_uri=")
            .append(cpd.server.origin())
            .append(baseHref)
            .append("oauth2/client/callback")
            .append("&")
            .append("client_id=")
            .append(oauth2ClientOptions.getClientID())
            .append("&")
            .append("scope=")
            .append(oauth2Flow.scopeString(" "))
            .append("&")
            .append("state=")
            .append((String) context.session().get("encodedState"));
    redirect(context, url.toString());
  }

  private void loginWithHash(final RoutingContext context) {
    final JsonObject hash = context.getBodyAsJson();
    logger.debug("receiverd hash: " + hash.encodePrettily());
    AuthSubRoute.checkEncodedStateStateCookie(context, hash.getString("state"));
    hash.remove("state");
    final WebClient client = AuthUtils.createWebClient(vertx);
    client.requestAbs(HttpMethod.GET, oauth2Flow.getUserProfile)
          .putHeader("Accept", "application/json")
          .putHeader("Authorization", "Bearer " + hash.getString("access_token"))
          .putHeader("scope", oauth2Flow.scopeString(","))
          .as(BodyCodec.jsonObject())
          .send(
              cr -> {
                client.close();
                if (cr.succeeded()) {
                  HttpResponse<JsonObject> response = cr.result();
                  if (response.statusCode() == HttpResponseStatus.OK.code()) {
                    logger.debug("body: " + response.body().encodePrettily());

                    final JsonObject state =
                        new JsonObject(base64.decode(context.session().remove("encodedState")));
                    final JsonObject loginState = state.getJsonObject("loginState");
                    final String provider = loginState.getString("provider");

                    getOrCreateAccount(
                        response.body(),
                        provider,
                        getOrCreateAccount -> {
                          if (getOrCreateAccount.succeeded()) {
                            final AccessToken user = new OAuth2TokenImpl(oauth2Provider, hash);
                            // add user to the session
                            user.principal().put("account", getOrCreateAccount.result());
                            user.principal().put("loginState", loginState);
                            context.setUser(user);
                            Session session = context.session();
                            if (session != null) {
                              // the user has upgraded from unauthenticated to authenticated
                              // session should be upgraded as recommended by owasp
                              session.regenerateId();
                            }
                            try {
                              AuthUtils.checkUserAccount(user);
                            } catch (AccountNotFoundException e) {
                              context.fail(e);
                              return;
                            }
                            logger.debug(
                                "implicit flow user principal: " + user.principal()
                                                                       .encodePrettily());
                            createJwtToken(context);
                            new JsonResponse(context).end(user.principal());
                          } else {
                            context.fail(getOrCreateAccount.cause());
                          }
                        });
                  } else {
                    context.clearUser();
                    context.fail(new NoStackTraceThrowable("error while fetching user account"));
                  }
                } else {
                  context.clearUser();
                  context.fail(cr.cause());
                }
              });
  }
}
