package br.com.diegoalexandro.challenge.api_gateway;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

    public static void main(String[] args) {
        var vertx = Vertx.vertx();

        ConfigStoreOptions fileStore = new ConfigStoreOptions()
                .setType("file")
                .setFormat("yaml")
                .setConfig(new JsonObject().put("path", "config.yml"));
        var configRetrieverOptions = new ConfigRetrieverOptions().addStore(fileStore);
        var configRetriever = ConfigRetriever.create(vertx, configRetrieverOptions);
        configRetriever
                .getConfig()
                .onSuccess(config -> {
                    log.info("Configurações recuperadas com sucesso. Iniciando verticles.");
                    var deploymentOptions = new DeploymentOptions().setConfig(config);
                    vertx.deployVerticle(ApiGatewayVerticle.class.getName(), deploymentOptions);
                })
                .onFailure(handler -> {
                    log.error("Problema ao recuperar as configurações. {}", handler.getMessage());
                    System.exit(1);
                });
    }
}
