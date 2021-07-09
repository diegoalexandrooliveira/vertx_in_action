package br.com.diegoalexandro.jukebox;

import io.vertx.core.Vertx;

public class Main {


    public static void main(String[] args) {
        var vertx = Vertx.vertx();
        vertx.deployVerticle(JukeboxVerticle.class.getName());
        vertx.deployVerticle(TCPServerVerticle.class.getName());
    }


}
