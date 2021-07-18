package br.com.diegoalexandro.challenge.api_gateway;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;

public enum GetMonthlySteps {

    INSTANCE;

    Future<HttpResponse<JsonObject>> call(WebClient webClient, String deviceId, String year, String month, String hostname, int port) {
        return webClient
                .get(port, hostname, "/" + deviceId + "/" + year + "/" + month)
                .as(BodyCodec.jsonObject())
                .putHeader("Content-Type", "application/json")
                .send();
    }
}
