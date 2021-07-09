package br.com.diegoalexandro.jukebox;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class JukeboxVerticle extends AbstractVerticle {

    private final Logger log = LoggerFactory.getLogger(JukeboxVerticle.class);

    private State currentState = State.PAUSED;
    private final Queue<String> playlist = new ArrayDeque<>();
    private final Set<HttpServerResponse> streamers = new HashSet<>();
    private AsyncFile currentFile;
    private long positionFile;

    @Override
    public void start() throws Exception {
        var eventBus = vertx.eventBus();
        eventBus.consumer(Events.LIST.getValue(), this::list);
        eventBus.consumer(Events.SCHEDULE.getValue(), this::schedule);
        eventBus.consumer(Events.PLAY.getValue(), this::play);
        eventBus.consumer(Events.PAUSE.getValue(), this::pause);

        vertx.createHttpServer()
                .requestHandler(this::handler)
                .listen(8080);

        vertx.setPeriodic(100, this::streamAudioChuck);
    }

    private void streamAudioChuck(Long id) {
        if (currentState.equals(State.PAUSED)) {
            return;
        }
        if (currentFile == null && playlist.isEmpty()) {
            currentState = State.PAUSED;
            return;
        }
        if (currentFile == null) {
            openNextFile();
        }
        currentFile.read(Buffer.buffer(4096), 0, positionFile, 4096, ar -> {
            Buffer result = ar.result();
            log.info("Resultado da leitura. {}", result.length());
            if (ar.succeeded()) {
                processReadBuffer(result);
            } else {
                log.error("Problema ao ler o arquivo.", ar.cause());
                closeCurrentFile();
            }
        });
    }

    private void processReadBuffer(Buffer buffer) {
        log.info("Processando buffer. Tamanho {}", buffer.length());
        positionFile += buffer.length();
        if (buffer.length() == 0) {
            log.info("Arquivo lido por completo");
            closeCurrentFile();
        }
        log.info("Enviando para os streamers. {}", streamers.size());
        for (HttpServerResponse streamer : streamers) {
            if (!streamer.writeQueueFull()) {
                streamer.write(buffer.copy());
            }
        }
    }

    private void closeCurrentFile() {
        log.info("Fechando arquivo atual.");
        currentFile.close(handle -> {
            currentFile = null;
            positionFile = 0;
        });
    }

    private void openNextFile() {
        positionFile = 0;
        currentFile = null;

        String file = playlist.poll();

        if (file == null) {
            return;
        }
        currentFile = vertx.fileSystem().openBlocking("tracks/" + file, new OpenOptions().setRead(true));
        log.info("Abrindo arquivo para começar o stream {}", file);
    }

    private void handler(HttpServerRequest httpServerRequest) {
        if ("/".equals(httpServerRequest.path())) {
            openAudioStream(httpServerRequest);
            return;
        } else if (httpServerRequest.path().startsWith("/download/")) {
            String filePath = httpServerRequest.path().substring(10).replace("/", "");
            download(filePath, httpServerRequest);
            return;
        }
        httpServerRequest.response().setStatusCode(404).end();
    }

    private void download(String filePath, HttpServerRequest httpServerRequest) {
        String file = "tracks/" + filePath;

        if (!vertx.fileSystem().existsBlocking(file)) {
            httpServerRequest.response().setStatusCode(404).end();
            return;
        }

        var openOptions = new OpenOptions().setRead(true);
        vertx.fileSystem().open(file, openOptions, ar -> {
            if (ar.succeeded()) {
                downloadFile(ar.result(), httpServerRequest);
            } else {
                log.error("Erro ao abrir o arquivo requisitado.", ar.cause());
                httpServerRequest.response().setStatusCode(500).end();
            }
        });


    }

    private void downloadFile(AsyncFile file, HttpServerRequest httpServerRequest) {
        HttpServerResponse response = httpServerRequest.response();

        response.setStatusCode(200)
                .putHeader("Content-Type", "audio/mpeg")
                .setChunked(true);
        file.pipeTo(response);
    }

    private void openAudioStream(HttpServerRequest httpServerRequest) {
        HttpServerResponse response = httpServerRequest.response()
                .putHeader("Content-Type", "audio/mpeg")
                .setChunked(true);

        streamers.add(response);
        response.endHandler(handler -> {
            streamers.remove(response);
            log.info("A streamer left");
        });

    }

    private void play(Message<?> message) {
        currentState = State.PLAYING;
    }

    private void pause(Message<?> message) {
        currentState = State.PAUSED;
    }

    private void list(Message<?> message) {
        vertx.fileSystem().readDir("tracks", ".*mp3$", ar -> {
            if (ar.succeeded()) {
                log.info("Listando arquivos encontrados na pasta tracks");
                JsonArray allFiles = ar.result()
                        .stream()
                        .map(File::new)
                        .map(File::getName)
                        .reduce(new JsonArray(), (array, file) -> {
                            array.add(file);
                            return array;
                        }, JsonArray::addAll);
                message.reply(new JsonObject().put("files", allFiles));
            } else {
                log.error("Não foi possível listar os arquivos da pasta tracks. ", ar.cause());
                message.fail(500, ar.cause().getMessage());
            }
        });
    }

    private void schedule(Message<JsonObject> message) {
        var file = message.body().getString("file");
        if (playlist.isEmpty() && currentState == State.PAUSED) {
            currentState = State.PLAYING;
        }
        playlist.offer(file);
        log.info("Adiciona arquivo na fila {}", file);
    }

}
