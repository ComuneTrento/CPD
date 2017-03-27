package it.beng.modeler.microservice.subroute.auth;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.UserSessionHandler;
import it.beng.modeler.config;
import it.beng.modeler.microservice.ResponseError;
import it.beng.modeler.microservice.auth.local.LocalAuthHandler;
import it.beng.modeler.microservice.auth.local.LocalAuthProvider;
import it.beng.modeler.microservice.subroute.AuthSubRoute;
import it.beng.modeler.microservice.subroute.SubRoute;

import java.util.Base64;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class LocalAuthSubRoute extends SubRoute {

    public LocalAuthSubRoute(Vertx vertx, Router router, MongoClient mongodb) {
        super(config.server.auth.path + "local/", vertx, router, mongodb);
    }

    @Override
    protected void init(Object userData) {

        /** LOCAL AUTHENTICATION **/

        // configure local auth provider
        final LocalAuthProvider localAuthProvider = LocalAuthProvider.create(vertx);

        // create local auth user session handler
        router.route().handler(UserSessionHandler.create(localAuthProvider));

        // create local user login handler
        router.route(HttpMethod.GET, path + "login/handler").handler(rc -> {
            JsonObject state = AuthSubRoute.getState(rc);
            if (state.getJsonObject("authInfo") == null) {
                rc.fail(new ResponseError(rc, "no authInfo supplied to login state"));
            } else rc.next();
        });
        router.route(HttpMethod.GET, path + "login/handler").handler(LocalAuthHandler.create(localAuthProvider));
    }

}