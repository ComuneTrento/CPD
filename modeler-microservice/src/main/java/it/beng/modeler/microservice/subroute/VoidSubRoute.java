package it.beng.modeler.microservice.subroute;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

/**
 * This class is a member of <strong>modeler-microservice</strong> project.
 *
 * @author vince
 */
public abstract class VoidSubRoute extends SubRoute<Void> {

  public VoidSubRoute(String path, Vertx vertx, Router router, boolean isPrivate) {
    super(path, vertx, router, isPrivate, null);
  }

  protected abstract void init();

  @Override
  protected void init(Void userData) {
    init();
  }
}
