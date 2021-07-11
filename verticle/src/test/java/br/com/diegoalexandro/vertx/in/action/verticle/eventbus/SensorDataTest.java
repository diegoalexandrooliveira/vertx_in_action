package br.com.diegoalexandro.vertx.in.action.verticle.eventbus;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.assertj.core.api.Assertions;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class SensorDataTest {

    @BeforeEach
    void setup(Vertx vertx, VertxTestContext vertxTestContext) {
        vertx.deployVerticle(SensorData.class.getName()).onSuccess(handler -> vertxTestContext.completeNow());
    }

    @Test
    @DisplayName("Should get average from SensorData")
    void average(Vertx vertx, VertxTestContext ctx) {
        vertx.eventBus().request("sensor.updates", new JsonObject().put("id", "123").put("temperature", 22D));
        vertx.eventBus().request("sensor.updates", new JsonObject().put("id", "456").put("temperature", 20D));

        Checkpoint checkpoint = ctx.checkpoint();

        Future<Message<JsonObject>> reply = vertx.eventBus().request("sensor.average", "");
        reply.onSuccess(handler -> ctx.verify(() -> {
            Double actualAverage = handler.body().getDouble("average");
            Assertions.assertThat(actualAverage)
                    .isCloseTo(21D, Percentage.withPercentage(1D));
            checkpoint.flag();
        }));
        reply.onFailure(ctx::failNow);
    }

}