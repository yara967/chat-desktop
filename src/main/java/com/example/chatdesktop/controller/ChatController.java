package com.example.chatdesktop.controller;

import com.example.chatdesktop.model.ChatMessage;
import com.example.chatdesktop.service.GroqService;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.util.ArrayList;
import java.util.List;

public class ChatController {

    @FXML
    private TextArea areaChat;

    @FXML
    private TextField campoMensagem;

    @FXML
    private Button botaoEnviar;

    @FXML
    private Button botaoNovaConversa;

    private GroqService groqService;

    private List<ChatMessage> historico;


    @FXML
    public void initialize() {

        groqService = new GroqService();

        historico = new ArrayList<>();

        iniciarHistorico();
    }


    private void iniciarHistorico() {

        historico.clear();

        historico.add(
                new ChatMessage(
                        "system",
                        "Você é um assistente útil, educado e objetivo. " +
                                "Responda sempre em português do Brasil."
                )
        );
    }


    @FXML
    private void enviarMensagem() {

        String mensagem =
                campoMensagem
                        .getText()
                        .trim();

        if (mensagem.isEmpty()) {
            return;
        }

        campoMensagem.clear();

        areaChat.appendText(
                "Você:\n"
                        + mensagem
                        + "\n\n"
        );

        historico.add(
                new ChatMessage(
                        "user",
                        mensagem
                )
        );

        bloquearInterface();

        groqService
                .enviarMensagem(historico)
                .thenAccept(
                        this::receberResposta
                )
                .exceptionally(
                        this::tratarErro
                );
    }


    private void receberResposta(String resposta) {

        Platform.runLater(() -> {

            areaChat.appendText(
                    "IA:\n"
                            + resposta
                            + "\n\n"
            );

            historico.add(
                    new ChatMessage(
                            "assistant",
                            resposta
                    )
            );

            liberarInterface();
        });
    }


    private Void tratarErro(Throwable erro) {

        Platform.runLater(() -> {

            areaChat.appendText(
                    "Erro:\n"
                            + erro.getMessage()
                            + "\n\n"
            );

            liberarInterface();
        });

        return null;
    }


    @FXML
    private void novaConversa() {

        // Limpa as mensagens que aparecem na tela
        areaChat.clear();

        // Limpa o histórico e adiciona novamente
        // a instrução inicial da IA
        iniciarHistorico();

        // Limpa o campo de mensagem
        campoMensagem.clear();

        // Garante que a interface fique liberada
        liberarInterface();

        // Coloca o cursor no campo de mensagem
        campoMensagem.requestFocus();
    }


    private void bloquearInterface() {

        campoMensagem.setDisable(true);

        botaoEnviar.setDisable(true);

        botaoNovaConversa.setDisable(true);
    }


    private void liberarInterface() {

        campoMensagem.setDisable(false);

        botaoEnviar.setDisable(false);

        botaoNovaConversa.setDisable(false);

        campoMensagem.requestFocus();
    }
}