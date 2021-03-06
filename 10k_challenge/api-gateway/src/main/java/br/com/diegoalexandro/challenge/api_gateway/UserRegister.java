package br.com.diegoalexandro.challenge.api_gateway;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;

public enum UserRegister {

    INSTANCE;

    Future<HttpResponse<JsonObject>> call(WebClient webClient, RoutingContext routingContext, String hostname, int port) {
        return webClient
                .post(port, hostname, "/register")
                .as(BodyCodec.jsonObject())
                .putHeader("Content-Type", "application/json")
                .sendJsonObject(routingContext.getBodyAsJson());
    }
}
