package it.beng.modeler.microservice.auth.local.impl;

import io.vertx.ext.auth.AuthProvider;
import it.beng.modeler.microservice.auth.local.LocalAuthHandler;

/**
 * This class is a member of <strong>modeler-microservice</strong> project.
 *
 * @author vince
 */
public class LocalAuthHandlerImpl /*extends AuthHandlerImpl*/ implements LocalAuthHandler {

  public LocalAuthHandlerImpl(AuthProvider authProvider) {
    //        super(authProvider);
  }

  //    @Override
  //    public void handle(RoutingContext context) {
  //
  //        User user = context.user();
  //        if (user != null) {
  //            this.authorise(user, context);
  //        } else {
  //            JsonObject state = AuthSubRoute.getState(context);
  //            authProvider.authenticate(state.getJsonObject("authInfo"), rh -> {
  //                if (rh.succeeded()) {
  //                    LocalUser authenticated = (LocalUser) rh.result();
  //                    logger.finest(Json.encodePrettily(authenticated.principal()));
  //                    context.setUser(authenticated);
  //                    this.authorise(authenticated, context);
  //                } else {
  //                    state.put("errorMsg", rh.cause().getMessage());
  //                    String encodedState = AuthSubRoute.getBase64EncodedState(context);
  //                    context.response()
  //                      .setStatusCode(302)
  //                      .putHeader("Location", config.server.baseHref + config.app.path +
  //                          config.locale(context) + "/login/" + encodedState)
  //                      .end();
  //                }
  //            });
  //        }
  //    }

}
