package br.com.diegoalexandro.challenge.api_gateway;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;

public enum GetUser {

    INSTANCE;

    Future<HttpResponse<JsonObject>> call(WebClient webClient, String username, String hostname, int port) {
        return webClient
                .get(port, hostname, "/" + username)
                .as(BodyCodec.jsonObject())
                .putHeader("Content-Type", "application/json")
                .send();
    }
}
