package br.com.diegoalexandro.vertx.in.action.verticle.api_gateway;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import java.util.Random;
import java.util.UUID;

public class HeatSensor extends AbstractVerticle {

    private final Random random = new Random();
    private final String sensorId = UUID.randomUUID().toString();
    private double temperature = 21.0;

    @Override
    public void start() {
        vertx.createHttpServer()
                .requestHandler(this::handleRequest)
                .listen(config().getInteger("port", 3000));

        scheduleNextUpdate();
    }

    private void handleRequest(HttpServerRequest httpServerRequest) {
        JsonObject data = new JsonObject()
                .put("id", sensorId)
                .put("temperature", temperature);

        httpServerRequest.response()
                .putHeader("Content-Type", "application/json")
                .end(data.encode());
    }

    private void scheduleNextUpdate() {
        vertx.setTimer(random.nextInt(5000) + 1000L, this::update);
    }

    private void update(long timerId) {
        temperature = temperature + (delta() / 10);
        scheduleNextUpdate();
    }

    private double delta() {
        if (random.nextInt() > 0) {
            return random.nextGaussian();
        } else {
            return -random.nextGaussian();
        }
    }
}
