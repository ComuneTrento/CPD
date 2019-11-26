package it.beng.modeler.microservice.auth.local.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AbstractUser;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import it.beng.microservice.common.AsyncHandler;
import it.beng.modeler.microservice.auth.local.LocalAuthProvider;
import it.beng.modeler.microservice.utils.AuthUtils;

/**
 * This class is a member of <strong>modeler-microservice</strong> project.
 *
 * @author vince
 */
public class LocalUser extends AbstractUser implements User {

  private JsonObject principal;
  private LocalAuthProvider authProvider;

  public LocalUser(JsonObject principal, LocalAuthProvider authProvider) {
    this.principal = principal;
    this.authProvider = authProvider;
  }

  public JsonObject getAccount() {
    return this.principal.getJsonObject("account");
  }

  public JsonObject getRoles() {
    return this.getAccount().getJsonObject("roles");
  }

  public void isAuthorized(JsonObject authority, AsyncHandler<Boolean> resultHandler) {
    AuthUtils.isAuthorized(authority, this.getRoles(), resultHandler);
  }

  @Override
  protected void doIsPermitted(String authority, Handler<AsyncResult<Boolean>> resultHandler) {
    isAuthorized(new JsonObject(authority), (AsyncHandler<Boolean>) resultHandler);
  }

  @Override
  public JsonObject principal() {
    return principal;
  }

  @Override
  public void setAuthProvider(AuthProvider authProvider) {
    if (authProvider instanceof LocalAuthProvider) {
      this.authProvider = (LocalAuthProvider) authProvider;
    } else {
      throw new IllegalArgumentException(authProvider.getClass() + " is not a LocalAuthProvider");
    }
  }
}
