package it.beng.modeler.microservice.services;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.BridgeEvent;
import it.beng.microservice.db.MongoDB;
import it.beng.modeler.config.cpd;
import it.beng.modeler.microservice.actions.IncomingAction;
import it.beng.modeler.microservice.actions.PublishAction;
import it.beng.modeler.microservice.utils.EventBusUtils;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public abstract class ActionService extends BridgeEventService {

  private static final Map<String, Class<? extends IncomingAction>> INCOMING_ACTIONS = new HashMap<>();

  static void registerIncomingAction(String type, Class<? extends IncomingAction> actionClass) {
    INCOMING_ACTIONS.put(type, actionClass);
  }

  private static IncomingAction createIncomingAction(BridgeEvent event) {
    JsonObject json = event.getRawMessage().getJsonObject("body");
    if (json == null) {
      EventBusUtils.fail(event, "illegal state: no json found in message body");
      return null;
    }
    String type = json.getString("type");
    if (type == null) {
      EventBusUtils.fail(event, "illegal state: no type found in action json");
      return null;
    }
    Class<? extends IncomingAction> actionClass = INCOMING_ACTIONS.get(type);
    if (actionClass == null) {
      if (cpd.develop()) {
        return new PublishAction(json) {
          @Override
          protected String innerType() {
            return this.json.getString("type");
          }
        };
      } else {
        EventBusUtils.fail(event, "illegal state: no incoming action registered for type " + type);
        return null;
      }
    }
    try {
      IncomingAction action =
          actionClass.getDeclaredConstructor(JsonObject.class).newInstance(json);
      if (!action.isValid()) {
        EventBusUtils.fail(event, "illegal state: action is invalid");
        return null;
      }
      return action;
    } catch (Exception e) {
      EventBusUtils.fail(event, e);
      return null;
    }
  }

  protected static final MongoDB mongodb = cpd.dataDB();

  private final String publishAddressRegex =
      "^" + address() + "::[a-z0-9]{8}-(?:[a-z0-9]{4}-){3}[a-z0-9]{12}$";
  private final Pattern publishAddressPattern = Pattern.compile(publishAddressRegex);

  ActionService(Vertx vertx) {
    super(vertx);
  }

  protected abstract String address();

  @Override
  protected void init() {
    vertx.eventBus().consumer(address(), message -> message.reply(message.body()));
  }

  @Override
  public Collection<PermittedOptions> inboundPermitted() {
    return Arrays.asList(
        new PermittedOptions().setAddress(address()),
        new PermittedOptions().setAddressRegex(publishAddressRegex));
  }

  @Override
  public Collection<PermittedOptions> outboundPermitted() {
    return inboundPermitted();
  }

  @Override
  public boolean handle(BridgeEvent event) {
    JsonObject message = event.getRawMessage();
    if (message == null) { return false; }
    String address = message.getString("address");
    if (address == null) { return false; }
    if (publishAddressPattern.matcher(address).matches()) {
      handlePublish(event);
      return true;
    } else if (address().equals(address)) {
      handleSend(event);
      return true;
    }
    return false;
  }

  private void handlePublish(BridgeEvent event) {
    switch (event.type()) {
      case SEND:
        EventBusUtils.fail(
            event, "illegal state: cannot send messages over a publish/register address");
        break;
      case PUBLISH:
        process(event);
        break;
      case RECEIVE:
      case REGISTER:
      case UNREGISTER:
      default:
        EventBusUtils.complete(event);
    }
  }

  private void handleSend(BridgeEvent event) {
    switch (event.type()) {
      case SEND:
        process(event);
        break;
      case PUBLISH:
        EventBusUtils.fail(
            event, "illegal state: cannot publish messages over a send/reply address");
        break;
      case REGISTER:
        EventBusUtils.fail(
            event, "illegal state: cannot register address over a send/reply address");
        break;
      case RECEIVE:
      case UNREGISTER:
      default:
        EventBusUtils.complete(event);
    }
  }

  private void process(BridgeEvent event) {
    IncomingAction incomingAction = createIncomingAction(event);
    if (incomingAction != null && incomingAction.isValid()) {
      incomingAction.handle(
          EventBusUtils.context(event),
          action -> {
            if (action.succeeded()) {
              event.getRawMessage().put("body", action.result());
              EventBusUtils.complete(event);
            } else {
              EventBusUtils.fail(event, action.cause());
            }
          });
    }
  }
}
