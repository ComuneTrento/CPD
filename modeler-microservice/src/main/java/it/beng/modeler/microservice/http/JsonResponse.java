package it.beng.modeler.microservice.http;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import it.beng.modeler.config.cpd;
import it.beng.modeler.microservice.utils.CommonUtils;
import java.time.Instant;
import java.util.Arrays;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class JsonResponse {

  private static final Logger logger = LogManager.getLogger(JsonResponse.class);

  private static Exception DEFAULT_EXCEPTION = new NullPointerException();

  public static HttpServerResponse setHeader(RoutingContext context) {
    return context.response().putHeader("Content-Type", "application/json; charset=utf-8");
  }

  private final RoutingContext context;
  private final HttpServerResponse response;

  public JsonResponse(RoutingContext context) {
    this.context = context;
    this.response = setHeader(context);
  }

  public static void endWithEmptyObject(RoutingContext context) {
    setHeader(context).end("{}");
  }

  public static void endWithEmptyArray(RoutingContext context) {
    setHeader(context).end("[]");
  }

  private String toJson(Object value) {
    return cpd.develop() ? Json.encodePrettily(value) : Json.encode(value);
  }

  public JsonResponse chunked() {
    this.response.setChunked(true);
    return this;
  }

  public JsonResponse status(HttpResponseStatus status) {
    this.response.setStatusCode(status.code());
    return this;
  }

  public JsonResponse write(Object value) {
    this.response.write(toJson(value));
    return this;
  }

  public void end(Object value) {
    this.response.end(toJson(value));
  }

  public void end() {
    this.response.end();
  }

  public void fail(Throwable failure, HttpResponseStatus status) {
    if (this.response.ended()) {
      return;
    }
    failure = CommonUtils.coalesce(failure, context.failure(), DEFAULT_EXCEPTION);
    status = CommonUtils.coalesce(status, context.failed() && this.context.statusCode() != -1
                                          ? HttpResponseStatus.valueOf(this.context.statusCode())
                                          : HttpResponseStatus.INTERNAL_SERVER_ERROR);
    String message = CommonUtils.coalesce(failure.getLocalizedMessage(), status.reasonPhrase());
    JsonObject error =
        new JsonObject()
            .put("timestamp", Instant.now().toEpochMilli())
            .put("code", status.code())
            .put("message", message);
    if (failure.getCause() != null) {
      error.put("stackTrace", new JsonArray(Arrays.asList(failure.getStackTrace())));
    }
    logger.error("ERROR (" + status + "): " + error.encodePrettily());
    if (!context.failed()) { context.fail(status.code(), failure); }
    this.response.setStatusCode(status.code()).end(error.encode());
  }

  public void fail(Throwable failure) {
    fail(failure, null);
  }

  public void fail() {
    fail(null, null);
  }
}
