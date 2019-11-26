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
public final class AppSubRoute extends VoidSubRoute {

  public AppSubRoute(Vertx vertx, Router router) {
    super("" /* config.app.path */, vertx, router, false);
  }

  @Override
  protected void init() {
    for (String locale : cpd.app.locales) {
      /* STATIC RESOURCES (CPD app) */
      final String appPath = path + locale;
      router
          .route(HttpMethod.GET, appPath + "/*")
          .handler(
              StaticHandler.create("web/ROOT/" + locale)
                           .setDirectoryListing(false)
                           .setAllowRootFileSystemAccess(false)
                           .setAlwaysAsyncFS(true)
                           .setCachingEnabled(true)
                           .setFilesReadOnly(true));
      router
          .route(HttpMethod.GET, appPath + "/*")
          .handler(
              context -> {
                if (context.normalisedPath().startsWith(appPath + "/" + cpd.ASSETS_PATH)) {
                  String resource =
                      context
                          .normalisedPath()
                          .substring((appPath + "/" + cpd.ASSETS_PATH).length());
                  context.reroute(path + cpd.ASSETS_PATH + "locale/" + locale + "/" + resource);
                } else if (!context.request().path().equals(appPath)) {
                  // let the application handle it's own dynamic route first
                  context.reroute(appPath);
                } else {
                  // if appPath has been already processed, pass it to next handler
                  context.next();
                }
                // (error handler)
              });
    }
  }
}
