package br.com.diegoalexandro.vertx.in.action.verticle.distributed_eventbus;

import br.com.diegoalexandro.vertx.in.action.verticle.eventbus.HeatSensor;
import br.com.diegoalexandro.vertx.in.action.verticle.eventbus.HttpServer;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstaceOne {

    private static final Logger log = LoggerFactory.getLogger(InstaceOne.class);

    public static void main(String[] args) {
        Vertx.clusteredVertx(new VertxOptions(), ar -> {
           if(ar.succeeded()){
               log.info("Instância 1 iniciada com sucesso");
               var vertx = ar.result();
               vertx.deployVerticle(HeatSensor.class.getName(), new DeploymentOptions().setInstances(4));
               vertx.deployVerticle(HttpServer.class.getCanonicalName(), new DeploymentOptions().setConfig(new JsonObject().put("port", 8080)));
           } else {
               log.error("Erro ao iniciar a instância", ar.cause());
           }
        });
    }
}
