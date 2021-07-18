package br.com.diegoalexandro.challenge.api_gateway;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;

public enum Authenticate {

    INSTANCE;

    Future<HttpResponse<JsonObject>> call(WebClient webClient, JsonObject payload, String hostname, int port) {
        return webClient.post(port, hostname, "/authenticate")
                .expect(ResponsePredicate.SC_SUCCESS)
                .as(BodyCodec.jsonObject())
                .sendJsonObject(payload);
    }
}
