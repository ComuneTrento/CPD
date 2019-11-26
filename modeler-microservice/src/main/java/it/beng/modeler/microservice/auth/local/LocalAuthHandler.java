package it.beng.modeler.microservice.auth.local;

import it.beng.modeler.microservice.auth.local.impl.LocalAuthHandlerImpl;

/**
 * This class is a member of <strong>modeler-microservice</strong> project.
 *
 * @author vince
 */
public interface LocalAuthHandler /*extends AuthHandler*/ {
  static LocalAuthHandler create(LocalAuthProvider authProvider) {
    return new LocalAuthHandlerImpl(authProvider) {};
  }
}
