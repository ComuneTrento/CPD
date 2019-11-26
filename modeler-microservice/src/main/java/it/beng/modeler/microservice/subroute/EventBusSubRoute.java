package it.beng.modeler.microservice.subroute;

import io.vertx.core.Vertx;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions;
import it.beng.modeler.config.cpd;
import it.beng.modeler.microservice.services.BridgeEventService;
import it.beng.modeler.microservice.services.DiagramActionService;
import it.beng.modeler.microservice.utils.EventBusUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class is a member of <strong>modeler-microservice</strong> project.
 *
 * @author vince
 */
public final class EventBusSubRoute extends VoidSubRoute {
  private static final Logger logger = LogManager.getLogger(EventBusSubRoute.class);

  static {
    // register the bridge event services here.
    // NOTE: insertion order is used for handler evaluation
    BridgeEventService.registerService(DiagramActionService.class);
  }

  public EventBusSubRoute(Vertx vertx, Router router) {
    super(cpd.server.eventBus.path, vertx, router, false);
  }

  @Override
  protected void init() {

    // start the bridge event registered services
    BridgeEventService.start(vertx);

    BridgeOptions bridgeOptions = new BridgeOptions();
    for (BridgeEventService service : BridgeEventService.services()) {
      for (PermittedOptions permitted : service.inboundPermitted())
        bridgeOptions.addInboundPermitted(permitted);
      for (PermittedOptions permitted : service.outboundPermitted())
        bridgeOptions.addOutboundPermitted(permitted);
    }

    SockJSHandler sockJSHandler =
        SockJSHandler.create(vertx, new SockJSHandlerOptions().setInsertJSESSIONID(true))
            .bridge(
                bridgeOptions,
                event -> {
                  EventBusUtils.log(event);
                  boolean handled = false;
                  for (BridgeEventService service : BridgeEventService.services()) {
                    if (service.handle(event)) {
                      handled = true;
                      break;
                    }
                  }
                  if (!handled) {
                    event.complete(true);
                  }
                });

    router.route(path + "*").handler(sockJSHandler);

    logger.info("Event Bus correctly created");
  }
}
