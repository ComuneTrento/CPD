package it.beng.modeler.microservice.actions;

import io.vertx.core.json.JsonObject;
import it.beng.microservice.db.MongoDB;
import it.beng.modeler.config.cpd;

public abstract class Action {
  protected static final MongoDB mongodb = cpd.dataDB();

  public final JsonObject json;

  public Action(JsonObject action) {
    this.json = action;
  }

  public String address() {
    return json.getString("address");
  }

  public String type() {
    return json.getString("type");
  }

  public boolean isValid() {
    return address() != null && type() != null;
  }
}
