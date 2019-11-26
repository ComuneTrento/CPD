package it.beng.modeler.microservice.subroute.auth;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.AccessToken;
import io.vertx.ext.web.Router;
import it.beng.modeler.config.cpd;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class is a member of <strong>modeler-microservice</strong> project.
 *
 * @author vince
 */
public final class OAuth2ClientSubRoute extends OAuth2SubRoute {
  private static final Logger logger = LogManager.getLogger(OAuth2ClientSubRoute.class);

  public static final String FLOW_TYPE = "CLIENT";

  public OAuth2ClientSubRoute(Vertx vertx, Router router, cpd.OAuth2Config oAuth2Config) {
    super(vertx, router, oAuth2Config, FLOW_TYPE);
  }

  public AccessToken getToken() {
    return vertx.getOrCreateContext().get(FLOW_TYPE + "_ACCESS_TOKEN");
  }

  public void setToken(User accessToken) {
    vertx.getOrCreateContext().put(FLOW_TYPE + "_ACCESS_TOKEN", accessToken);
  }

  @Override
  protected void init() {
    JsonObject tokenConfig =
        new JsonObject()
            .put("client_id", oauth2Config.clientId)
            .put("client_secret", oauth2Config.clientSecret);
    oauth2Provider.authenticate(
        tokenConfig,
        ar -> {
          if (ar.succeeded()) {
            User accessToken = ar.result();
            setToken(accessToken);
            logger.info(
                "client access token correctly created: "
                    + accessToken.principal().encodePrettily());
          } else {
            logger.warn("COULD NOT GET CLIENT TOKEN");
            ar.cause().printStackTrace();
          }
        });
  }
}
