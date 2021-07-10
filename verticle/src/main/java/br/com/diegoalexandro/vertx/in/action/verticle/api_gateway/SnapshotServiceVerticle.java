package br.com.diegoalexandro.vertx.in.action.verticle.api_gateway;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SnapshotServiceVerticle extends AbstractVerticle {

    private final Logger log = LoggerFactory.getLogger(SnapshotServiceVerticle.class);

    @Override
    public void start() throws Exception {
        vertx.createHttpServer()
                .requestHandler(request -> {
                    if (badRequest(request)) {
                        request.response().setStatusCode(400).end();
                    }

                    request.bodyHandler(buffer -> {
                        log.info("Ultimas temperaturas: {}", buffer.toJsonObject().encodePrettily());
                        request.response().end();
                    });
                })
                .listen(4000);
    }

    private boolean badRequest(HttpServerRequest request) {
        return !request.method().equals(HttpMethod.POST) ||
                !"application/json".equals(request.getHeader("Content-Type"));
    }
}
