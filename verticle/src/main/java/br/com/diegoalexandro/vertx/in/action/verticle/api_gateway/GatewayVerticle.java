package br.com.diegoalexandro.vertx.in.action.verticle.api_gateway;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class GatewayVerticle extends AbstractVerticle {

    private final Logger log = LoggerFactory.getLogger(GatewayVerticle.class);
    private WebClient webClient;

    @Override
    public void start() throws Exception {
        webClient = WebClient.create(vertx);
        vertx.createHttpServer()
                .requestHandler(this::handleRequest)
                .listen(8080);
    }

    private void handleRequest(HttpServerRequest httpServerRequest) {
        List<JsonObject> responses = new ArrayList<>();
        var counter = new AtomicInteger(0);
        for (var i = 0; i < 3; i++) {
            webClient
                    .get(3000 + i, "localhost", "/")
                    .expect(ResponsePredicate.SC_SUCCESS)
                    .as(BodyCodec.jsonObject())
                    .send(ar -> {
                        log.info("Chamou o serviço de temperatura.");
                        if (ar.succeeded()) {
                            responses.add(ar.result().body());
                        } else {
                            log.error("Problema ao chamar o serviço de temperatura", ar.cause());
                        }
                        if (counter.incrementAndGet() == 3) {
                            JsonObject data = new JsonObject()
                                    .put("data", new JsonArray(responses));
                            sendToSnapshot(httpServerRequest, data);
                        }
                    });
        }
    }

    private void sendToSnapshot(HttpServerRequest httpServerRequest, JsonObject data) {
        webClient
                .post(4000, "localhost", "/")
                .expect(ResponsePredicate.SC_SUCCESS)
                .sendJsonObject(data, ar -> {
                    if (ar.succeeded()) {
                        httpServerRequest.response()
                                .putHeader("Content-Type", "application/json")
                                .end(data.encode());
                    } else {
                        log.error("Falha ao enviar dados para o Snapshot Service", ar.cause());
                        httpServerRequest.response().setStatusCode(500).end();
                    }
                });
    }
}
