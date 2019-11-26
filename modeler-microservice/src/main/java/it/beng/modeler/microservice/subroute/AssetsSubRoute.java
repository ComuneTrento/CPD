package it.beng.modeler.microservice.subroute;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import it.beng.modeler.config.cpd;

/**
 * This class is a member of <strong>modeler-microservice</strong> project.
 *
 * @author vince
 */
public final class AssetsSubRoute extends VoidSubRoute {

  public AssetsSubRoute(Vertx vertx, Router router) {
    super(/* config.app.path + */ cpd.ASSETS_PATH, vertx, router, false);
  }

  @Override
  protected void init() {
    router
        .route(HttpMethod.GET, path + "*")
        .handler(
            StaticHandler.create("web/assets").setDirectoryListing(cpd.server.assets.allowListing));
  }
}
