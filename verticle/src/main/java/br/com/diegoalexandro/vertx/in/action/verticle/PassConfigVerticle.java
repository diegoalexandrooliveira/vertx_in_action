package br.com.diegoalexandro.vertx.in.action.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PassConfigVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(PassConfigVerticle.class);


    @Override
    public void start() throws Exception {
        logger.info("n = {}", config().getInteger("n", -1));
    }

    public static void main(String[] args) {
        var vertx = Vertx.vertx();

        for (var i = 0; i < 4; i++) {
            JsonObject config = new JsonObject().put("n", i);
            var deploymentOptions = new DeploymentOptions()
                    .setConfig(config)
                    .setInstances(i);
            vertx.deployVerticle("br.com.diegoalexandro.vertx.in.action.verticle.PassConfigVerticle", deploymentOptions);
        }
    }
}
