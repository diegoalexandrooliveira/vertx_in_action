package br.com.diegoalexandro.vertx.in.action.verticle.distributed_eventbus;

import br.com.diegoalexandro.vertx.in.action.verticle.eventbus.HeatSensor;
import br.com.diegoalexandro.vertx.in.action.verticle.eventbus.HttpServer;
import br.com.diegoalexandro.vertx.in.action.verticle.eventbus.Listener;
import br.com.diegoalexandro.vertx.in.action.verticle.eventbus.SensorData;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstanceTwo {

    private static final Logger log = LoggerFactory.getLogger(InstanceTwo.class);

    public static void main(String[] args) {
        Vertx.clusteredVertx(new VertxOptions(), ar -> {
            if (ar.succeeded()) {
                log.info("Instância 2 iniciada com sucesso");
                var vertx = ar.result();
                vertx.deployVerticle(HeatSensor.class.getName(), new DeploymentOptions().setInstances(4));
                vertx.deployVerticle(Listener.class.getName());
                vertx.deployVerticle(SensorData.class.getName());
                vertx.deployVerticle(HttpServer.class.getCanonicalName(), new DeploymentOptions().setConfig(new JsonObject().put("port", 8081)));
            } else {
                log.error("Erro ao iniciar a instância", ar.cause());
            }
        });
    }
}
