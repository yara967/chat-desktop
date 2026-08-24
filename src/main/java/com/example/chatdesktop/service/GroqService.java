package com.example.chatdesktop.service;

import com.example.chatdesktop.config.GroqConfig;
import com.example.chatdesktop.model.ChatMessage;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;

import java.time.Duration;

import java.util.List;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

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

        String json;

        try {

            json = criarJson(historico);

        } catch (Exception erro) {

            return CompletableFuture.failedFuture(
                    new RuntimeException(
                            "Não foi possível preparar a mensagem. "
                                    + "Tente novamente."
                    )
            );
        }


        HttpRequest request;

        try {

            String apiKey =
                    GroqConfig.getApiKey();


            if (apiKey == null
                    || apiKey.isBlank()) {

                return CompletableFuture.failedFuture(
                        new RuntimeException(
                                "A chave da API não foi configurada. "
                                        + "Verifique a configuração da aplicação."
                        )
                );
            }


            request =
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
                                            + apiKey
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

        } catch (Exception erro) {

            return CompletableFuture.failedFuture(
                    new RuntimeException(
                            "Não foi possível preparar a comunicação com a API. "
                                    + "Verifique a configuração da aplicação."
                    )
            );
        }


        return httpClient
                .sendAsync(
                        request,
                        HttpResponse
                                .BodyHandlers
                                .ofString()
                )
                .thenApply(
                        this::processarResposta
                )
                .exceptionallyCompose(
                        erro -> {

                            Throwable causa =
                                    obterCausa(erro);


                            // =========================================
                            // SEM INTERNET
                            // =========================================

                            if (causa instanceof ConnectException
                                    || causa instanceof UnknownHostException) {

                                return CompletableFuture.failedFuture(
                                        new RuntimeException(
                                                "Não foi possível conectar à internet. "
                                                        + "Verifique sua conexão e tente novamente."
                                        )
                                );
                            }


                            // =========================================
                            // TEMPO LIMITE
                            // =========================================

                            if (causa instanceof HttpTimeoutException) {

                                return CompletableFuture.failedFuture(
                                        new RuntimeException(
                                                "A comunicação com a IA demorou demais. "
                                                        + "Verifique sua conexão e tente novamente."
                                        )
                                );
                            }


                            // =========================================
                            // ERRO JÁ TRATADO
                            // =========================================

                            if (causa instanceof RuntimeException) {

                                return CompletableFuture.failedFuture(
                                        causa
                                );
                            }


                            // =========================================
                            // ERRO DESCONHECIDO
                            // =========================================

                            return CompletableFuture.failedFuture(
                                    new RuntimeException(
                                            "Ocorreu uma falha de comunicação "
                                                    + "com o serviço da IA. "
                                                    + "Tente novamente."
                                    )
                            );
                        }
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

        int status =
                response.statusCode();


        // =========================================
        // CHAVE INVÁLIDA
        // =========================================

        if (status == 401) {

            throw new RuntimeException(
                    "A chave da API é inválida ou não foi autorizada. "
                            + "Verifique a configuração da aplicação."
            );
        }


        // =========================================
        // LIMITE DA API
        // =========================================

        if (status == 429) {

            throw new RuntimeException(
                    "O limite da API foi atingido. "
                            + "Aguarde alguns instantes e tente novamente."
            );
        }


        // =========================================
        // REQUISIÇÃO INVÁLIDA
        // =========================================

        if (status == 400) {

            throw new RuntimeException(
                    "Não foi possível processar sua solicitação. "
                            + "Verifique a mensagem e tente novamente."
            );
        }


        // =========================================
        // NÃO AUTORIZADO / PERMISSÃO
        // =========================================

        if (status == 403) {

            throw new RuntimeException(
                    "Acesso à API não autorizado. "
                            + "Verifique sua chave e as permissões da aplicação."
            );
        }


        // =========================================
        // SERVIDOR INDISPONÍVEL
        // =========================================

        if (status >= 500 && status <= 599) {

            throw new RuntimeException(
                    "O serviço da IA está temporariamente indisponível. "
                            + "Tente novamente daqui a pouco."
            );
        }


        // =========================================
        // OUTROS ERROS HTTP
        // =========================================

        if (status != 200) {

            throw new RuntimeException(
                    "Ocorreu uma falha de comunicação com o serviço. "
                            + "Tente novamente."
            );
        }


        // =========================================
        // PROCESSAR RESPOSTA NORMAL
        // =========================================

        try {

            JsonObject json =
                    JsonParser
                            .parseString(
                                    response.body()
                            )
                            .getAsJsonObject();


            JsonArray choices =
                    json.getAsJsonArray(
                            "choices"
                    );


            if (choices == null
                    || choices.isEmpty()) {

                throw new RuntimeException(
                        "A IA não retornou uma resposta válida."
                );
            }


            return choices
                    .get(0)
                    .getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content")
                    .getAsString();

        } catch (RuntimeException erro) {

            throw erro;

        } catch (Exception erro) {

            throw new RuntimeException(
                    "A resposta da IA não pôde ser processada. "
                            + "Tente novamente."
            );
        }
    }


    private Throwable obterCausa(
            Throwable erro
    ) {

        Throwable causa =
                erro;


        while (
                (causa instanceof CompletionException
                        || causa instanceof RuntimeException)
                        && causa.getCause() != null
        ) {

            /*
             * Só continua descendo quando existe
             * uma causa real por trás do erro.
             */
            causa =
                    causa.getCause();
        }


        return causa;
    }
}