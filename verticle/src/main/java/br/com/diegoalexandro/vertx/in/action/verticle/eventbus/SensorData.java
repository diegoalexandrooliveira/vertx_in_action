package br.com.diegoalexandro.vertx.in.action.verticle.eventbus;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class SensorData extends AbstractVerticle {

    private final Map<String, Double> temperatures = new HashMap<>();

    @Override
    public void start() throws Exception {
        var eventBus = vertx.eventBus();
        eventBus.consumer("sensor.updates", this::update);
        eventBus.consumer("sensor.average", this::average);
    }

    private void average(Message<JsonObject> message) {
        var average = temperatures.values()
                .stream()
                .collect(Collectors.averagingDouble(Double::doubleValue));

        JsonObject response = new JsonObject().put("average", average);

        message.reply(response);
    }

    private void update(Message<JsonObject> message) {
        JsonObject body = message.body();
        temperatures.put(body.getString("id"), body.getDouble("temperature"));
    }
}
