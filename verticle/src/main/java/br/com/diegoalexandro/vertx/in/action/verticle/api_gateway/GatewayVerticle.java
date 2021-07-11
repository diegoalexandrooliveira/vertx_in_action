package br.com.diegoalexandro.vertx.in.action.verticle.api_gateway;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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

    private Future<JsonObject> fetchTemperature(int port) {
        return webClient
                .get(port, "localhost", "/")
                .expect(ResponsePredicate.SC_SUCCESS)
                .as(BodyCodec.jsonObject())
                .send()
                .map(HttpResponse::body);
    }

    private void handleRequest(HttpServerRequest httpServerRequest) {

        CompositeFuture.all(
                fetchTemperature(3000),
                fetchTemperature(3001),
                fetchTemperature(3002))
                .flatMap(this::sendToSnapshot)
                .onSuccess(data -> httpServerRequest.response()
                        .putHeader("Content-Type", "application/json")
                        .end(data.encode()))
                .onFailure(err -> {
                    log.error("Problema ao chamar o serviço de temperatura", err);
                    httpServerRequest.response().setStatusCode(500).end();
                });

    }

    private Future<JsonObject> sendToSnapshot(CompositeFuture temperatures) {
        List<JsonObject> allTemps = temperatures.list();
        JsonObject data = new JsonObject()
                .put("data", new JsonArray()
                        .add(allTemps.get(0))
                        .add(allTemps.get(1))
                        .add(allTemps.get(2)));
        return webClient
                .post(4000, "localhost", "/")
                .expect(ResponsePredicate.SC_SUCCESS)
                .as(BodyCodec.jsonObject())
                .sendJsonObject(data)
                .map(response -> data);
    }
}
