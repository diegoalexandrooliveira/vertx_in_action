package br.com.diegoalexandro.jukebox;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetSocket;
import io.vertx.core.parsetools.RecordParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TCPServerVerticle extends AbstractVerticle {

    private Logger log = LoggerFactory.getLogger(TCPServerVerticle.class);

    @Override
    public void start() throws Exception {
        vertx.createNetServer()
                .connectHandler(this::handleClient)
                .listen(3000);
    }

    private void handleClient(NetSocket netSocket) {
        RecordParser.newDelimited("\n", netSocket)
                .handler(buffer -> handleBuffer(netSocket, buffer))
                .endHandler(v -> log.info("Connection ended"));

    }

    private void handleBuffer(NetSocket netSocket, Buffer buffer) {
        var command = buffer.toString();
        switch (command) {
            case "/list":
                listCommand(netSocket);
                break;
            case "/play":
                vertx.eventBus().send(Events.PLAY.getValue(), "");
                break;
            case "/pause":
                vertx.eventBus().send(Events.PAUSE.getValue(), "");
                break;
            default:
                if (command.startsWith("/schedule")) {
                    scheduleCommand(command);
                } else {
                    netSocket.write("Comando desconhecido.\n");
                }
        }
    }

    private void scheduleCommand(String command) {
        var music = command.substring(10);
        JsonObject file = new JsonObject().put("file", music);
        vertx.eventBus().send(Events.SCHEDULE.getValue(), file);
    }

    private void listCommand(NetSocket netSocket) {
        vertx.eventBus().request(Events.LIST.getValue(), "", reply -> {
            if (reply.succeeded()) {
                ((JsonObject) reply.result().body()).getJsonArray("files")
                        .stream()
                        .forEach(fileName -> netSocket.write(fileName.toString() + "\n"));
            } else {
                log.error("Erro no comando /list", reply.cause());
                netSocket.write(reply.cause().getMessage());
            }
        });
    }

}
