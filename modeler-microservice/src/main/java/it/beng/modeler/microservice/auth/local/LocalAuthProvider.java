package it.beng.modeler.microservice.auth.local;

import io.vertx.core.Vertx;
import io.vertx.ext.auth.AuthProvider;
import it.beng.modeler.microservice.auth.local.impl.LocalAuthProviderImpl;

/**
 * This class is a member of <strong>modeler-microservice</strong> project.
 *
 * @author vince
 */
public interface LocalAuthProvider extends AuthProvider {

  static LocalAuthProvider create(Vertx vertx) {
    return LocalAuthProviderImpl.get(vertx);
  }
}
