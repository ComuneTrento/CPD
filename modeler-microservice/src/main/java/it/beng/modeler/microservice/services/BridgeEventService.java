package it.beng.modeler.microservice.services;

import io.vertx.core.Vertx;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.BridgeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class BridgeEventService {
  private static final Logger logger = LogManager.getLogger(BridgeEventService.class);

  private static final Map<Class<? extends BridgeEventService>, BridgeEventService> SERVICES =
      new LinkedHashMap<>();

  protected final Vertx vertx;

  BridgeEventService(Vertx vertx) {
    if (SERVICES.get(this.getClass()) != null) {
      throw new IllegalStateException(
          "only one instance of " + this.getClass().getName() + " is allowed");
    }
    this.vertx = vertx;
    SERVICES.put(this.getClass(), this);
    init();
  }

  public static void registerService(Class<DiagramActionService> bridgeEventServiceClass) {
    SERVICES.put(bridgeEventServiceClass, null);
  }

  public static void start(Vertx vertx) {
    SERVICES
        .keySet()
        .forEach(
            serviceClass -> {
              try {
                serviceClass.getDeclaredConstructor(Vertx.class).newInstance(vertx);
              } catch (Exception e) {
                logger.error(
                    "could not instantiate "
                        + serviceClass.getName()
                        + " because of "
                        + e.getLocalizedMessage());
              }
            });
  }

  public static Collection<BridgeEventService> services() {
    return SERVICES.values();
  }

  protected abstract void init();

  public abstract Collection<PermittedOptions> inboundPermitted();

  public abstract Collection<PermittedOptions> outboundPermitted();

  public abstract boolean handle(BridgeEvent event);
}
