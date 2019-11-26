package it.beng.microservice.common;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Collection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * <p>This class is a member of <strong>microservice-stack</strong> project.</p>
 *
 * @author vince
 */
public final class Countdown {

  private static Logger logger = LogManager.getLogger(Countdown.class);

  private int steps;
  private int i;
  private JsonObject data;
  private AsyncHandler<Integer> stepHandler;
  private Future<Void> completeFuture = Future.future();

  public Countdown(int steps) {
    this.steps = steps;
    this.i = steps;
  }

  public Countdown(Collection<?> collection) {
    this(collection.size());
  }

  public Countdown(JsonArray collection) {
    this(collection.size());
  }

  public Countdown onStep(AsyncHandler<Integer> handler) {
    this.stepHandler = handler;
    return this;
  }

  public Countdown onComplete(AsyncHandler<Void> handler) {
    this.completeFuture.setHandler(handler);
    if (!this.hasNext()) { this.completeFuture.complete(); }
    return this;
  }

  public int steps() {
    return steps;
  }

  public int step() {
    return steps - i;
  }

  public boolean hasNext() {
    return i > 0;
  }

  public Countdown next() {
    i--;
    if (!hasNext()) { completeFuture.complete(); } else if (stepHandler != null) {
      stepHandler.handle(Future.succeededFuture(step()));
    }
    return this;
  }

  public boolean isComplete() {
    return completeFuture.isComplete();
  }

  public boolean failed() {
    return completeFuture.failed();
  }

  public void fail(Throwable error) {
    if (failed()) {
      logger.warn("Countdown is already complete: failed");
    } else {
      i = 0;
      completeFuture.fail(error);
    }
  }

  public JsonObject data() {
    if (data == null) { data = new JsonObject(); }
    return data;
  }

  @Override
  public String toString() {
    return super.toString() + " (i=" + i + ")";
  }

}
