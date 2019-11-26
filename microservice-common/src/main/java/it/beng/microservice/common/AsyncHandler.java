package it.beng.microservice.common;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/**
 * <p>This interface is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public interface AsyncHandler<T> extends Handler<AsyncResult<T>> {}
