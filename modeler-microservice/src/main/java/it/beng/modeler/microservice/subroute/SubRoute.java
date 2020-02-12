package it.beng.modeler.microservice.subroute;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import it.beng.microservice.db.MongoDB;
import it.beng.microservice.schema.SchemaTools;
import it.beng.modeler.config.cpd;
import it.beng.modeler.microservice.utils.AuthUtils;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class is a member of <strong>modeler-microservice</strong> project.
 *
 * @author vince
 */
public abstract class SubRoute<T> {

  private static final Logger logger = LogManager.getLogger(SubRoute.class);

  protected static ThreadLocal<Vertx> _vertx = new ThreadLocal<>();

  static {
    Json.mapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
    // Json.mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, true);
    Json.mapper.setDefaultPropertyInclusion(
        JsonInclude.Value.construct(Include.ALWAYS, Include.NON_NULL));
    Json.prettyMapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
    // Json.prettyMapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, true);
    Json.prettyMapper.setDefaultPropertyInclusion(
        JsonInclude.Value.construct(Include.ALWAYS, Include.NON_NULL));
  }

  protected final String baseHref;
  protected final String path;
  protected final Vertx vertx;
  protected final Router router;
  protected final MongoDB mongodb;
  protected final SchemaTools schemaTools;
  // protected final ModelTools modelTools;
  protected final boolean isPrivate;

  public SubRoute(String path, Vertx vertx, Router router, boolean isPrivate, T userData) {
    _vertx.set(vertx);
    this.baseHref = cpd.server.baseHref;
    this.path = baseHref + path;
    logger.info("sub-route registered: " + this.path);
    this.vertx = vertx;
    this.router = router;
    this.mongodb = cpd.dataDB();
    this.schemaTools = cpd.schemaTools();
    // this.modelTools = modelTools;
    this.isPrivate = isPrivate;
    if (isPrivate) {
      router.route(path + "*").handler(SubRoute::privateRoute);
    }
    this.init(userData);
  }

  protected abstract void init(T userData);

  protected static void privateRoute(RoutingContext context) {
    if (context.user() == null) {
      failUnauthorized(context);
    } else {
      context.next();
    }
  }

  protected static void failUnauthorized(RoutingContext context) {
    context.fail(HttpResponseStatus.UNAUTHORIZED.code());
  }

  protected static void failMethodNotAllowed(RoutingContext context) {
    context.fail(HttpResponseStatus.METHOD_NOT_ALLOWED.code());
  }

  public static final class base64 {

    public static String decode(String encoded) {
      return new String(
          Base64.getDecoder().decode(encoded.replace('_', '/').replace('-', '+')),
          StandardCharsets.ISO_8859_1);
    }

    public static String encode(String decoded) {
      return Base64.getEncoder()
                   .encodeToString(decoded.getBytes(StandardCharsets.ISO_8859_1))
                   .replace('/', '_')
                   .replace('+', '-');
    }

    public static String encode(JsonObject jsonObject) {
      return encode(jsonObject.encode());
    }
  }

  protected static boolean passOrFail(
      RoutingContext context, boolean passCondition, HttpResponseStatus status) {
    if (!passCondition) {
      context.fail(status.code());
    }
    return passCondition;
  }

  protected static boolean isLoggedInOtherwiseFail(RoutingContext context) {
    return passOrFail(context, context.user() != null, HttpResponseStatus.UNAUTHORIZED);
  }

  protected static boolean isAdmin(User user) {
    return AuthUtils.isAdmin(user);
  }

  protected static boolean isAdminOtherwiseFail(RoutingContext context) {
    return passOrFail(context, isAdmin(context.user()), HttpResponseStatus.UNAUTHORIZED);
  }

  protected static boolean isCivilServant(User user) {
    return user != null
        && (isAdmin(user)
        || "civil-servant"
        .equals(AuthUtils.getAccount(user).getJsonObject("roles").getString("interaction"))
    );
  }

  protected static boolean isCivilServantFailOtherwise(RoutingContext context) {
    return passOrFail(context, isCivilServant(context.user()), HttpResponseStatus.UNAUTHORIZED);
  }

  public static void redirect(RoutingContext context, final String location) {
    final String _location = location.replaceAll("(?<!:)/{2,}", "/");
    context
        .response()
        // disable all caching
        .putHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
        .putHeader("Pragma", "no-cache")
        .putHeader(HttpHeaders.EXPIRES, "0")
        .putHeader(HttpHeaders.LOCATION, _location)
        .setStatusCode(HttpResponseStatus.FOUND.code())
        .end("Redirecting to " + _location + ".");
  }
}
