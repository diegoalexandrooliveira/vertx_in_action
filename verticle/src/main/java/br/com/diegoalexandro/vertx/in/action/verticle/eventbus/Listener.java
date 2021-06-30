package br.com.diegoalexandro.vertx.in.action.verticle.eventbus;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;

public class Listener extends AbstractVerticle {
    private final Logger log = LoggerFactory.getLogger(Listener.class);
    private final DecimalFormat decimalFormat = new DecimalFormat("#.##");

    @Override
    public void start() throws Exception {
        var eventBus = vertx.eventBus();
        eventBus.<JsonObject>consumer("sensor.updates", msg -> {
            var body = msg.body();
            log.info("{} reports a temperature ~{}C", body.getString("id"),
                    decimalFormat.format(body.getDouble("temperature")));
        });
    }
}
