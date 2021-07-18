package br.com.diegoalexandro.challenge.api_gateway;

import lombok.Getter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

public class CryptoHelper {

    @Getter
    private final String publicKey;

    @Getter
    private final String privateKey;

    private static CryptoHelper instance;

    public static CryptoHelper INSTANCE() throws IOException {
        if (Objects.isNull(instance)) {
            instance = new CryptoHelper();
        }
        return instance;
    }

    private CryptoHelper() throws IOException {
        publicKey = read("public_key.pem");
        privateKey = read("private_key.pem");
    }

    private String read(String key) throws IOException {
        var path = Paths.get("public-api", key);
        if (!path.toFile().exists()) {
            path = Paths.get("10k_challenge", "api-gateway", "src", "main", "resources", "public-api", key);
        }
        return String.join("\n", Files.readAllLines(path, StandardCharsets.UTF_8));
    }
}
