package br.com.diegoalexandro.vertx.in.action.verticle.api_gateway;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class Main {

    public static void main(String[] args) {
        var vertx = Vertx.vertx();
        vertx.deployVerticle(SnapshotServiceVerticle.class.getName());
        vertx.deployVerticle(GatewayVerticle.class.getName());
        vertx.deployVerticle(HeatSensor.class.getName(), new DeploymentOptions().setConfig(new JsonObject().put("port", 3000)));
        vertx.deployVerticle(HeatSensor.class.getName(), new DeploymentOptions().setConfig(new JsonObject().put("port", 3001)));
        vertx.deployVerticle(HeatSensor.class.getName(), new DeploymentOptions().setConfig(new JsonObject().put("port", 3002)));
    }
}
