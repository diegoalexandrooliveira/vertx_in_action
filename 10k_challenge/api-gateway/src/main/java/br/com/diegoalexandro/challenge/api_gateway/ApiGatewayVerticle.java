package br.com.diegoalexandro.challenge.api_gateway;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ApiGatewayVerticle extends AbstractVerticle {

    private WebClient webClient;

    @Override
    public void start() throws Exception {
        var router = Router.router(vertx);
        webClient = WebClient.create(vertx);
        var config = vertx.getOrCreateContext().config();
        var path = config.getJsonObject("api_gateway").getString("path");

        var port = config.getJsonObject("api_gateway").getInteger("port", 8080);
        vertx.createHttpServer()
                .requestHandler(router)
                .listen(port)
                .onSuccess(event -> log.info("Servidor sendo executado na porta {}", port))
                .onFailure(event -> log.error("Erro ao executar o servidor. {}", event.getMessage()));

        var bodyHandler = BodyHandler.create();
        router.post().handler(bodyHandler);
        router.put().handler(bodyHandler);
        router.post(path + "/register").handler(this::register);
        router.post(path + "/token").handler(this::token);

        Handler<RoutingContext> jwtHandler = null;

        router.get(path + "/:username/:year/:month")
                .handler(jwtHandler)
                .handler(this::checkUser)
                .handler(this::monthlySteps);
    }

    private void monthlySteps(RoutingContext routingContext) {

    }

    private void checkUser(RoutingContext routingContext) {

    }

    private void token(RoutingContext routingContext) {

    }

    private void register(RoutingContext routingContext) {
        var port = vertx.getOrCreateContext().config().getJsonObject("register").getInteger("port");
        var hostname = vertx.getOrCreateContext().config().getJsonObject("register").getString("host");
        UserRegister.INSTANCE.register(webClient, routingContext, hostname, port)
                .onSuccess(response -> sendStatusCode(routingContext, response))
                .onFailure(err -> sendBadGateway(routingContext, err));
    }

    private void sendBadGateway(RoutingContext routingContext, Throwable err) {
        log.error(err.getMessage());
        routingContext.fail(502);
    }

    private void sendStatusCode(RoutingContext routingContext, HttpResponse<Buffer> response) {
        routingContext.response().setStatusCode(response.statusCode()).end();
    }
}
