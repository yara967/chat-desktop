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

    // =========================================================
    // RESULTADO DA RESPOSTA
    // =========================================================

    public static class ResultadoResposta {

        private final String resposta;
        private final String origem;
        private final String fonte;

        public ResultadoResposta(
                String resposta,
                String origem,
                String fonte
        ) {
            this.resposta = resposta;
            this.origem = origem;
            this.fonte = fonte;
        }

        public String getResposta() {
            return resposta;
        }

        public String getOrigem() {
            return origem;
        }

        public String getFonte() {
            return fonte;
        }
    }

    // =========================================================
    // CONSTRUTOR
    // =========================================================

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

    // =========================================================
    // ENVIAR MENSAGEM NORMAL
    // =========================================================

    public CompletableFuture<ResultadoResposta> enviarMensagem(
            List<ChatMessage> historico
    ) {

        return enviarRequisicao(
                historico,
                false
        );
    }

    // =========================================================
    // REGENERAR RESPOSTA
    // =========================================================

    public CompletableFuture<ResultadoResposta> regenerarResposta(
            List<ChatMessage> historico
    ) {

        return enviarRequisicao(
                historico,
                true
        );
    }

    // =========================================================
    // ENVIAR REQUISIÇÃO
    // =========================================================

    private CompletableFuture<ResultadoResposta> enviarRequisicao(
            List<ChatMessage> historico,
            boolean regeneracao
    ) {

        String json;

        try {

            json =
                    criarJson(
                            historico,
                            regeneracao
                    );

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
                                    "Bearer " + apiKey
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
                        HttpResponse.BodyHandlers.ofString()
                )
                .thenApply(
                        this::processarResposta
                )
                .exceptionallyCompose(
                        erro -> {

                            Throwable causa =
                                    obterCausa(erro);

                            if (causa instanceof ConnectException
                                    || causa instanceof UnknownHostException) {

                                return CompletableFuture.failedFuture(
                                        new RuntimeException(
                                                "Sem conexão com a internet. "
                                                        + "Verifique sua conexão e tente novamente."
                                        )
                                );
                            }

                            if (causa instanceof HttpTimeoutException) {

                                return CompletableFuture.failedFuture(
                                        new RuntimeException(
                                                "A comunicação com a IA demorou demais. "
                                                        + "Verifique sua conexão e tente novamente."
                                        )
                                );
                            }

                            if (causa instanceof RuntimeException) {

                                return CompletableFuture.failedFuture(
                                        causa
                                );
                            }

                            return CompletableFuture.failedFuture(
                                    new RuntimeException(
                                            "Não foi possível se comunicar "
                                                    + "com o serviço da IA. "
                                                    + "Tente novamente."
                                    )
                            );
                        }
                );
    }

    // =========================================================
    // CRIAR JSON
    // =========================================================

    private String criarJson(
            List<ChatMessage> historico,
            boolean regeneracao
    ) {

        JsonObject json =
                new JsonObject();

        json.addProperty(
                "model",
                GroqConfig.MODEL
        );

        /*
         * Temperatura maior na regeneração.
         *
         * Isso aumenta a possibilidade de a IA
         * produzir uma resposta diferente.
         */
        if (regeneracao) {

            json.addProperty(
                    "temperature",
                    0.9
            );

        } else {

            json.addProperty(
                    "temperature",
                    0.7
            );
        }

        JsonArray mensagens =
                new JsonArray();

        // =====================================================
        // LER DOCUMENTO DO RAG
        // =====================================================

        LeitorDocumento leitorDocumento =
                new LeitorDocumento();

        String conhecimento =
                leitorDocumento.lerDocumento();

        // =====================================================
        // INSTRUÇÃO PARA A IA
        // =====================================================

        JsonObject instrucao =
                new JsonObject();

        instrucao.addProperty(
                "role",
                "system"
        );

        String textoInstrucao =

                "Você é um assistente inteligente e útil. "

                        + "Ao responder uma pergunta, siga esta prioridade: "

                        + "1. Primeiro, consulte e utilize as informações "
                        + "presentes no DOCUMENTO fornecido. "

                        + "2. Quando a informação estiver no DOCUMENTO, "
                        + "dê preferência a ela e não a substitua por "
                        + "informações externas ou pelo seu conhecimento geral. "

                        + "3. Se a informação solicitada não estiver "
                        + "presente no DOCUMENTO, você pode utilizar "
                        + "seu conhecimento geral para responder. "

                        + "4. Nunca invente informações. "

                        + "5. Responda sempre em português do Brasil. ";

        /*
         * Instrução adicional somente para regeneração.
         */
        if (regeneracao) {

            textoInstrucao +=

                    "\n\nEsta é uma REGENERAÇÃO da resposta anterior. "
                            + "Responda novamente à última pergunta, "
                            + "mas procure apresentar a resposta de uma "
                            + "forma diferente da resposta anterior, "
                            + "mantendo as informações corretas. "
                            + "Não diga que está regenerando a resposta.";
        }

        textoInstrucao +=

                "\n\nDOCUMENTO DO RAG:\n"
                        + conhecimento;

        instrucao.addProperty(
                "content",
                textoInstrucao
        );

        mensagens.add(
                instrucao
        );

        // =====================================================
        // ADICIONAR HISTÓRICO
        // =====================================================

        for (ChatMessage mensagem :
                historico) {

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

            mensagens.add(
                    item
            );
        }

        json.add(
                "messages",
                mensagens
        );

        return gson.toJson(
                json
        );
    }

    // =========================================================
    // PROCESSAR RESPOSTA
    // =========================================================

    private ResultadoResposta processarResposta(
            HttpResponse<String> response
    ) {

        int status =
                response.statusCode();

        if (status == 401) {

            throw new RuntimeException(
                    "A chave da API é inválida ou não foi autorizada. "
                            + "Verifique a configuração da aplicação."
            );
        }

        if (status == 429) {

            throw new RuntimeException(
                    "O limite da API foi atingido. "
                            + "Aguarde alguns instantes e tente novamente."
            );
        }

        if (status == 400) {

            throw new RuntimeException(
                    "Não foi possível processar sua solicitação. "
                            + "Verifique a mensagem e tente novamente."
            );
        }

        if (status == 403) {

            throw new RuntimeException(
                    "Acesso à API não autorizado. "
                            + "Verifique sua chave e as permissões da aplicação."
            );
        }

        if (status >= 500
                && status <= 599) {

            throw new RuntimeException(
                    "O serviço da IA está temporariamente indisponível. "
                            + "Tente novamente daqui a pouco."
            );
        }

        if (status != 200) {

            throw new RuntimeException(
                    "Ocorreu uma falha de comunicação com o serviço da IA. "
                            + "Tente novamente."
            );
        }

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

            String resposta =
                    choices
                            .get(0)
                            .getAsJsonObject()
                            .getAsJsonObject(
                                    "message"
                            )
                            .get(
                                    "content"
                            )
                            .getAsString();

            // =================================================
            // DEFINIR ORIGEM E FONTE
            // =================================================

            LeitorDocumento leitorDocumento =
                    new LeitorDocumento();

            boolean documentoExiste =
                    leitorDocumento.documentoExiste();

            String origem;

            String fonte;

            if (documentoExiste) {

                origem =
                        "RAG";

                fonte =
                        leitorDocumento
                                .getNomeFonte();

            } else {

                origem =
                        "Fallback local";

                fonte =
                        "Nenhuma";
            }

            return new ResultadoResposta(
                    resposta,
                    origem,
                    fonte
            );

        } catch (RuntimeException erro) {

            throw erro;

        } catch (Exception erro) {

            throw new RuntimeException(
                    "A resposta da IA não pôde ser processada. "
                            + "Tente novamente."
            );
        }
    }

    // =========================================================
    // OBTER CAUSA REAL DO ERRO
    // =========================================================

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

            causa =
                    causa.getCause();
        }

        return causa;
    }
}