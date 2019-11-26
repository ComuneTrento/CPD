package it.beng.modeler.microservice.subroute.auth;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.impl.NoStackTraceThrowable;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.UserSessionHandler;
import it.beng.modeler.config.cpd;
import it.beng.modeler.microservice.auth.local.LocalAuthProvider;
import it.beng.modeler.microservice.http.JsonResponse;
import it.beng.modeler.microservice.subroute.VoidSubRoute;
import it.beng.modeler.microservice.utils.AuthUtils;
import javax.security.auth.login.AccountNotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class is a member of <strong>modeler-microservice</strong> project.
 *
 * @author vince
 */
public final class LocalAuthSubRoute extends VoidSubRoute {

  private static final Logger logger = LogManager.getLogger(LocalAuthSubRoute.class);

  public static final String PROVIDER = "local";

  private LocalAuthProvider localAuthProvider;

  public LocalAuthSubRoute(Vertx vertx, Router router) {
    super(cpd.server.auth.path + PROVIDER + "/", vertx, router, false);
  }

  public LocalAuthProvider getLocalAuthProvider() {
    return localAuthProvider;
  }

  @Override
  protected void init() {

    /** LOCAL AUTHENTICATION * */

    // configure local auth provider
    localAuthProvider = LocalAuthProvider.create(vertx);

    // create local auth user session handler
    final UserSessionHandler localUserSessionHandler = UserSessionHandler.create(localAuthProvider);
    router.route(HttpMethod.POST, path + "login/handler").handler(localUserSessionHandler);

    // create local user login handler
    router.route(HttpMethod.POST, path + "login/handler").handler(context -> {
      final JsonObject authInfo = context.getBodyAsJson();
      if (authInfo != null) {
        localAuthProvider.authenticate(authInfo, result -> {
          if (result.succeeded()) {
            User user = result.result();
            JsonObject loginState = context.get("loginState");
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
            // TODO: store user session in the database in order to do something similar to jwt
//            OAuth2SubRoute.createJwtToken(context);
            new JsonResponse(context).end(user.principal());
          } else {
            context.fail(result.cause());
          }
        });
      } else { context.fail(new NoStackTraceThrowable("no authInfo body in login post")); }
    });
  }
}
