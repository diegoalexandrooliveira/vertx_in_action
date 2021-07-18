package br.com.diegoalexandro.challenge.api_gateway;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class ApiGatewayVerticle extends AbstractVerticle {

    private static final String REGISTER_MS = "register";
    private static final String STEPS_MS = "steps";
    private static final String HOST = "host";
    private static final String PORT = "port";
    private static final String DEVICE_ID = "deviceId";
    private static final String ALGORITHM = "RS256";
    private static final String USERNAME = "username";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";

    private WebClient webClient;
    private JWTAuth jwtAuth;

    @Override
    public void start() throws Exception {
        Handler<RoutingContext> jwtHandler = createJWTHandler();

        var router = Router.router(vertx);
        webClient = WebClient.create(vertx);
        var config = vertx.getOrCreateContext().config();
        var path = config.getJsonObject("api_gateway").getString("path");

        var port = config.getJsonObject("api_gateway").getInteger(PORT, 8080);
        vertx.createHttpServer()
                .requestHandler(router)
                .listen(port)
                .onSuccess(event -> log.info("Servidor sendo executado na porta {}", port))
                .onFailure(event -> log.error("Erro ao executar o servidor. {}", event.getMessage()));

        var bodyHandler = BodyHandler.create();
        router.post().handler(bodyHandler);
        router.put().handler(bodyHandler);
        router.route().handler(corsHandler());

        routesRegister(router, path, jwtHandler);
    }

    private Handler<RoutingContext> corsHandler() {
        Set<String> allowedHeahders = new HashSet<>();
        allowedHeahders.add("x-requested-with");
        allowedHeahders.add("Access-Control-Allow-Origin");
        allowedHeahders.add("origin");
        allowedHeahders.add(CONTENT_TYPE_HEADER);
        allowedHeahders.add("accept");
        allowedHeahders.add("Authorization");

        Set<HttpMethod> httpMethods = new HashSet<>();
        httpMethods.add(HttpMethod.GET);
        httpMethods.add(HttpMethod.POST);
        httpMethods.add(HttpMethod.OPTIONS);
        httpMethods.add(HttpMethod.PUT);

        return CorsHandler.create("*").allowedHeaders(allowedHeahders).allowedMethods(httpMethods);
    }

    private void routesRegister(Router router, String path, Handler<RoutingContext> jwtHandler) {
        router.post(path + "/register").handler(this::register);
        router.post(path + "/token").handler(this::token);
        router.get(path + "/:username/:year/:month")
                .handler(jwtHandler)
                .handler(this::checkUser)
                .handler(this::monthlySteps);
        router.get(path + "/:username")
                .handler(jwtHandler)
                .handler(this::checkUser)
                .handler(this::getUser);
    }

    private void getUser(RoutingContext routingContext) {
        var hostname = vertx.getOrCreateContext().config().getJsonObject(REGISTER_MS).getString(HOST);
        var port = vertx.getOrCreateContext().config().getJsonObject(REGISTER_MS).getInteger(PORT);
        GetUser.INSTANCE.call(webClient, routingContext.pathParam(USERNAME), hostname, port)
                .onSuccess(response -> forwardJsonOrStatusCode(routingContext, response))
                .onFailure(err -> sendBadGateway(routingContext, err));
    }

    private Handler<RoutingContext> createJWTHandler() throws IOException {
        var privateKey = CryptoHelper.INSTANCE().getPrivateKey();
        var publicKey = CryptoHelper.INSTANCE().getPublicKey();
        jwtAuth = JWTAuth.create(vertx, new JWTAuthOptions()
                .addPubSecKey(new PubSecKeyOptions().setAlgorithm(ALGORITHM).setBuffer(publicKey))
                .addPubSecKey(new PubSecKeyOptions().setAlgorithm(ALGORITHM).setBuffer(privateKey))
        );
        return JWTAuthHandler.create(jwtAuth);
    }

    private void monthlySteps(RoutingContext routingContext) {
        var hostname = vertx.getOrCreateContext().config().getJsonObject(STEPS_MS).getString(HOST);
        var port = vertx.getOrCreateContext().config().getJsonObject(STEPS_MS).getInteger(PORT);
        var deviceId = routingContext.user().principal().getString(DEVICE_ID);
        GetMonthlySteps.INSTANCE.call(webClient, deviceId, routingContext.pathParam("year"), routingContext.pathParam("month"), hostname, port)
                .onSuccess(response -> forwardJsonOrStatusCode(routingContext, response))
                .onFailure(err -> sendBadGateway(routingContext, err));
    }

    private void checkUser(RoutingContext routingContext) {
        var subject = routingContext.user().principal().getString("sub");
        if (!routingContext.pathParam(USERNAME).equals(subject)) {
            sendStatusCode(routingContext, 403);
            return;
        }

        routingContext.next();
    }


    private void token(RoutingContext routingContext) {
        JsonObject payload = routingContext.getBodyAsJson();
        var username = payload.getString(USERNAME);
        var hostname = context.config().getJsonObject(REGISTER_MS).getString(HOST);
        var port = context.config().getJsonObject(REGISTER_MS).getInteger(PORT);

        Authenticate.INSTANCE.call(webClient, payload, hostname, port)
                .flatMap(res -> getUserDetails(username))
                .map(res -> res.body().getString(DEVICE_ID))
                .map(deviceId -> makeJwtToken(username, deviceId))
                .onSuccess(token -> sendToken(routingContext, token))
                .onFailure(err -> handleAuthError(routingContext, err));
    }

    private void handleAuthError(RoutingContext routingContext, Throwable err) {
        log.error("Erro de autenticação", err);
        routingContext.fail(401);
    }

    private void sendToken(RoutingContext routingContext, String token) {
        routingContext.response().putHeader(CONTENT_TYPE_HEADER, "application/jwt").end(token);
    }

    private String makeJwtToken(String username, String deviceId) {
        var claims = new JsonObject()
                .put(DEVICE_ID, deviceId);
        var jwtOptions = new JWTOptions()
                .setAlgorithm(ALGORITHM)
                .setExpiresInMinutes(60)
                .setIssuer("10k_steps_api")
                .setSubject(username);
        return jwtAuth.generateToken(claims, jwtOptions);
    }

    private Future<HttpResponse<JsonObject>> getUserDetails(String username) {
        var hostname = context.config().getJsonObject(REGISTER_MS).getString(HOST);
        var port = context.config().getJsonObject(REGISTER_MS).getInteger(PORT);
        return GetUserDetails.INSTANCE.call(webClient, username, hostname, port);
    }

    private void register(RoutingContext routingContext) {
        var hostname = vertx.getOrCreateContext().config().getJsonObject(REGISTER_MS).getString(HOST);
        var port = vertx.getOrCreateContext().config().getJsonObject(REGISTER_MS).getInteger(PORT);
        UserRegister.INSTANCE.call(webClient, routingContext, hostname, port)
                .onSuccess(response -> sendStatusCode(routingContext, response.statusCode()))
                .onFailure(err -> sendBadGateway(routingContext, err));
    }

    private void sendBadGateway(RoutingContext routingContext, Throwable err) {
        log.error(err.getMessage());
        routingContext.fail(502);
    }

    private void sendStatusCode(RoutingContext routingContext, int statusCode) {
        routingContext.response().setStatusCode(statusCode).end();
    }

    private void forwardJsonOrStatusCode(RoutingContext routingContext, HttpResponse<JsonObject> response) {
        if (response.statusCode() != 200) {
            sendStatusCode(routingContext, response.statusCode());
            return;
        }

        routingContext
                .response()
                .putHeader(CONTENT_TYPE_HEADER, "application/json")
                .end(response.body().encode());
    }
}
