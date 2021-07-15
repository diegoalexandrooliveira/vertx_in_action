package br.com.diegoalexandro.challenge.api_gateway;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

public enum UserRegister {

    INSTANCE;

    Future<HttpResponse<Buffer>> register(WebClient webClient, RoutingContext routingContext, String hostname, int port) {
        return webClient
                .post(port, hostname, "/register")
                .putHeader("Content-Type", "application/json")
                .sendJsonObject(routingContext.getBodyAsJson());
    }
}
