package it.beng.modeler.microservice.utils;

import io.vertx.core.eventbus.ReplyFailure;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.BridgeEventType;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.impl.UserHolder;
import io.vertx.ext.web.handler.sockjs.BridgeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class EventBusUtils {
  private static final Logger logger = LogManager.getLogger(EventBusUtils.class);

  public static void log(BridgeEvent event) {
    if (event.type() != BridgeEventType.SOCKET_PING) {
      JsonObject message = event.getRawMessage();
      logger.debug(
          "EVENT BUS: $1 $2"
              .replace("$1", event.type().name())
              .replace("$2", message != null ? message.encodePrettily() : "-"));
    }
  }

  public static RoutingContext context(BridgeEvent event) {
    UserHolder userHolder = event.socket().webSession().get("__vertx.userHolder");
    return userHolder != null ? userHolder.context : null;
  }

  public static void complete(BridgeEvent event) {
    event.complete(true);
  }

  public static void fail(BridgeEvent event, String message) {
    JsonObject envelope =
        new JsonObject()
            .put("type", "err")
            .put("address", event.getRawMessage().getString("replyAddress"))
            .put("failureCode", -1)
            .put("failureType", ReplyFailure.RECIPIENT_FAILURE.name().toLowerCase())
            .put("message", message)
            .put("payoad", event.getRawMessage());
    event.socket().write(envelope.encode());
    event.fail(message);
  }

  public static void fail(BridgeEvent event, Throwable exception) {
    fail(event, exception.getLocalizedMessage());
  }
}
