package br.com.diegoalexandro.vertx.in.action.verticle.eventbus;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;

public class HttpServer extends AbstractVerticle {

    @Override
    public void start() throws Exception {
        vertx.createHttpServer()
                .requestHandler(this::handler)
                .listen(config().getInteger("port", 8080));
    }

    private void handler(HttpServerRequest httpServerRequest) {
        String path = httpServerRequest.path();
        if ("/".equals(path)) {
            httpServerRequest.response().sendFile("index.html");
        } else if ("/sse".equals(path)) {
            sse(httpServerRequest);
        } else {
            httpServerRequest.response().setStatusCode(404);
        }
    }

    private void sse(HttpServerRequest httpServerRequest) {
        HttpServerResponse response = httpServerRequest.response();

        response.putHeader("Content-Type", "text/event-stream")
                .putHeader("Cache-Control", "no-cache")
                .setChunked(true);

        MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer("sensor.updates");

        consumer.handler(message -> {
            response.write("event: update\n");
            response.write("data: " + message.body().encode() + "\n\n");
        });

        var timeoutStream = vertx.periodicStream(5000);
        timeoutStream.handler(id ->
                vertx.eventBus().<JsonObject>request("sensor.average", "", reply -> {
                    if (reply.succeeded()) {
                        response.write("event: average\n");
                        response.write("data: " + reply.result().body().encode() + "\n\n");
                    }
                })
        );

        response.endHandler(event -> {
            consumer.unregister();
            timeoutStream.cancel();
        });
    }
}
