package it.beng.microservice.common;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * <p>This class is a member of <strong>microservice-stack</strong> project.</p>
 *
 * @author vince
 */
public final class Launcher extends io.vertx.core.Launcher {

  private static final Logger logger = LogManager.getLogger(Launcher.class);

  private static final String VERTX_LOGGER_DELEGATE_FACTORY_PROPERTY_NAME = "vertx.logger-delegate-factory-class-name";
  private static final String VERTX_LOGGER_DELEGATE_FACTORY_PROPERTY_VALUE = "io.vertx.core.logging.Log4j2LogDelegateFactory";
  private static final String DEFAULT_CONFIG_FILE = "conf/config.json";

  static {
    /* use log4j2 as default logging system */
    // -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.Log4j2LogDelegateFactory
    System.setProperty(VERTX_LOGGER_DELEGATE_FACTORY_PROPERTY_NAME,
        VERTX_LOGGER_DELEGATE_FACTORY_PROPERTY_VALUE);
  }

  private static JsonObject getDefaultConfig() {
    JsonObject configuration = new JsonObject();
    File configFile = new File(DEFAULT_CONFIG_FILE);
    if (configFile.isFile()) {
      logger.info("Reading config file: " + configFile.getAbsolutePath());
      try (Scanner scanner = new Scanner(configFile).useDelimiter("\\A")) {
        String strConfig = scanner.next();
        try {
          configuration = new JsonObject(strConfig);
        } catch (DecodeException e) {
          logger.error("Configuration file " + strConfig + " does not contain a valid JSON object");
        }
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      }
    } else {
      logger.error("Config file not found " + configFile.getAbsolutePath());
    }
    return configuration;
  }

  public static void main(String[] args) {
    new Launcher().dispatch(args);
  }

  @Override
  public void beforeStartingVertx(VertxOptions options) {
    options.setClustered(true);
  }

  @Override
  public void afterStoppingVertx() {
    System.clearProperty(VERTX_LOGGER_DELEGATE_FACTORY_PROPERTY_NAME);
  }

  @Override
  public void beforeDeployingVerticle(DeploymentOptions deploymentOptions) {
    super.beforeDeployingVerticle(deploymentOptions);
    if (deploymentOptions.getConfig() == null || deploymentOptions.getConfig().isEmpty()) {
      deploymentOptions.setConfig(new JsonObject().mergeIn(getDefaultConfig()));
    }
    logger.info(deploymentOptions.getConfig().encodePrettily());
  }

}
