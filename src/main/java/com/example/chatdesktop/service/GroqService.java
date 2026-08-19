package com.example.chatdesktop.service;

import com.example.chatdesktop.config.GroqConfig;
import com.example.chatdesktop.model.ChatMessage;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.time.Duration;

import java.util.List;

import java.util.concurrent.CompletableFuture;

public class GroqService {

    private final HttpClient httpClient;

    private final Gson gson;

    public GroqService() {

        httpClient =
                HttpClient
                        .newBuilder()
                        .connectTimeout(
                                Duration.ofSeconds(20)
                        )
                        .build();

        gson = new Gson();
    }

    public CompletableFuture<String> enviarMensagem(
            List<ChatMessage> historico
    ) {

        String json =
                criarJson(historico);

        HttpRequest request =
                HttpRequest
                        .newBuilder()
                        .uri(
                                URI.create(
                                        GroqConfig.API_URL
                                )
                        )
                        .timeout(
                                Duration.ofSeconds(60)
                        )
                        .header(
                                "Authorization",
                                "Bearer "
                                        + GroqConfig.getApiKey()
                        )
                        .header(
                                "Content-Type",
                                "application/json"
                        )
                        .POST(
                                HttpRequest
                                        .BodyPublishers
                                        .ofString(json)
                        )
                        .build();

        return httpClient
                .sendAsync(
                        request,
                        HttpResponse
                                .BodyHandlers
                                .ofString()
                )
                .thenApply(
                        this::processarResposta
                );
    }

    private String criarJson(
            List<ChatMessage> historico
    ) {

        JsonObject json =
                new JsonObject();

        json.addProperty(
                "model",
                GroqConfig.MODEL
        );

        JsonArray mensagens =
                new JsonArray();

        for (ChatMessage mensagem : historico) {

            JsonObject item =
                    new JsonObject();

            item.addProperty(
                    "role",
                    mensagem.getRole()
            );

            item.addProperty(
                    "content",
                    mensagem.getContent()
            );

            mensagens.add(item);
        }

        json.add(
                "messages",
                mensagens
        );

        return gson.toJson(json);
    }

    private String processarResposta(
            HttpResponse<String> response
    ) {

        if (response.statusCode() != 200) {

            throw new RuntimeException(
                    "Erro Groq: "
                            + response.statusCode()
                            + "\n"
                            + response.body()
            );
        }

        JsonObject json =
                JsonParser
                        .parseString(
                                response.body()
                        )
                        .getAsJsonObject();

        return json
                .getAsJsonArray("choices")
                .get(0)
                .getAsJsonObject()
                .getAsJsonObject("message")
                .get("content")
                .getAsString();
    }
}