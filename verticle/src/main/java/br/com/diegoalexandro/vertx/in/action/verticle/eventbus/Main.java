package br.com.diegoalexandro.vertx.in.action.verticle.eventbus;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;

public class Main {

    public static void main(String[] args) {
        var vertx = Vertx.vertx();

        vertx.deployVerticle(HeatSensor.class.getCanonicalName(), new DeploymentOptions().setInstances(8));
        vertx.deployVerticle(Listener.class.getCanonicalName());
        vertx.deployVerticle(SensorData.class.getCanonicalName());
        vertx.deployVerticle(HttpServer.class.getCanonicalName());
    }
}
