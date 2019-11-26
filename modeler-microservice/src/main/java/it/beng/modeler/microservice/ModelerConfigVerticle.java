package it.beng.modeler.microservice;

import io.netty.util.internal.logging.Log4J2LoggerFactory;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import it.beng.microservice.common.MicroServiceVerticle;
import it.beng.modeler.config.cpd;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class is a member of <strong>modeler-microservice</strong> project.
 *
 * @author vince
 */
public class ModelerConfigVerticle extends MicroServiceVerticle {
  private static final Logger logger = LogManager.getLogger(ModelerConfigVerticle.class);

  static {
    io.netty.util.internal.logging.InternalLoggerFactory.setDefaultFactory(
        Log4J2LoggerFactory.INSTANCE);
  }

  @Override
  public void start() {
    super.start();

    cpd.setup(
        vertx,
        config(),
        done -> {
          if (done.succeeded()) {
            vertx.deployVerticle(
                new ModelerServerVerticle(),
                new DeploymentOptions().setConfig(config()),
                complete -> {
                  if (complete.succeeded()) {
                    logger.info("Succesfully deployed ModelerServerVerticle: " + complete.result());
                  } else {
                    logger.fatal(
                        "Cannot deploy ModelerServerVerticle: " + complete.cause().getMessage());
                    throw new IllegalStateException(complete.cause());
                  }
                });
          } else {
            throw new IllegalStateException(done.cause());
          }
        });

    // config.set(this.vertx, config(), configSet -> {
    //     if (configSet.succeeded()) {
    //         vertx.deployVerticle(new ModelerServerVerticle(), new
    // DeploymentOptions().setConfig(config()), ar -> {
    //             if (ar.succeeded()) {
    //                 logger.info("Succesfully deployed ModelerServerVerticle: " + ar.result());
    //             } else {
    //                 logger.severe("Cannot deploy ModelerServerVerticle: " +
    // ar.cause().getMessage());
    //             }
    //         });

    //         // Publish the services in the discovery infrastructure.
    //         /*
    //         publishMessageSource("api", "data", rec -> {
    //             if (!rec.succeeded()) {
    //                 rec.cause().printStackTrace();
    //             }
    //             System.out.println("Modeler API service published: " + rec.succeeded());
    //         });

    //         publishHttpEndpoint("quotes", "localhost", config().getInteger("http.port", 8080),
    // null, ar -> {
    //             if (ar.failed()) {
    //                 ar.cause().printStackTrace();
    //             } else {
    //                 System.out.println("Quotes (REST endpoint) service published : " +
    // ar.succeeded());
    //             }
    //         });
    //         */
    //     } else {
    //         throw new IllegalStateException(configSet.cause());
    //     }
    // });

  }

  @Override
  public void stop(Future<Void> future) throws Exception {
    logger.info("Killing main thread...");
    super.stop(future);
    cpd.tearDown();
  }
}
