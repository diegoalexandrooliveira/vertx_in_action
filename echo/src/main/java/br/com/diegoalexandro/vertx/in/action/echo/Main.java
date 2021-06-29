package br.com.diegoalexandro.vertx.in.action.echo;

import io.vertx.core.Vertx;
import io.vertx.core.net.NetSocket;

public class Main {

    private static int numberOfConnections = 0;

    public static void main(String[] args) {
        var vertx = Vertx.vertx();

        vertx.createNetServer()
                .connectHandler(Main::handleNewClient)
                .listen(3000);

        vertx.setPeriodic(5000, id -> System.out.println(getConnections()));

        vertx.createHttpServer()
                .requestHandler(request -> request.response().end(getConnections()))
                .listen(8080);
    }

    private static void handleNewClient(final NetSocket netSocket) {
        numberOfConnections++;
        netSocket.handler(buffer -> {
            netSocket.write(buffer);
            if (buffer.toString().equals("/quit\n")) {
                netSocket.close();
            }
        });
        netSocket.closeHandler(v -> numberOfConnections--);
    }

    private static String getConnections() {
        return String.format("We now have %s connections", numberOfConnections);
    }
}
