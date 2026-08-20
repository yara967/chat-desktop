package com.example.chatdesktop.controller;

import com.example.chatdesktop.model.ChatMessage;
import com.example.chatdesktop.service.GroqService;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

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

    @FXML
    private Button botaoCopiarResposta;

    private GroqService groqService;

    private List<ChatMessage> historico;

    private String ultimaRespostaIA = "";


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

        ultimaRespostaIA = "";
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

            ultimaRespostaIA = resposta;

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

        areaChat.clear();

        iniciarHistorico();

        campoMensagem.clear();

        liberarInterface();

        campoMensagem.requestFocus();
    }


    @FXML
    private void copiarResposta() {

        if (ultimaRespostaIA == null || ultimaRespostaIA.isEmpty()) {
            return;
        }

        Clipboard clipboard = Clipboard.getSystemClipboard();

        ClipboardContent conteudo = new ClipboardContent();

        conteudo.putString(ultimaRespostaIA);

        clipboard.setContent(conteudo);
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