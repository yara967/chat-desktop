package com.example.chatdesktop.controller;

import com.example.chatdesktop.model.ChatMessage;
import com.example.chatdesktop.service.GroqService;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class ChatController {

    @FXML
    private BorderPane rootPane;

    @FXML
    private VBox chatBox;

    @FXML
    private ScrollPane scrollChat;

    @FXML
    private TextField campoMensagem;

    @FXML
    private Button botaoEnviar;

    @FXML
    private Button botaoNovaConversa;

    @FXML
    private Button botaoCopiarResposta;

    @FXML
    private Button botaoTema;

    @FXML
    private VBox historicoBox;

    @FXML
    private Button botaoOpcoes;


    private GroqService groqService;

    private List<ChatMessage> historico;

    private String ultimaRespostaIA = "";

    private boolean temaEscuro = false;


    /*
     * ID usado para saber qual conversa está aberta.
     */
    private int idConversaAtual = 0;


    /*
     * Guarda qual conversa salva está aberta.
     *
     * -1 significa que estamos em uma conversa nova.
     */
    private int conversaSalvaAtual = -1;


    /*
     * Todas as conversas salvas.
     */
    private final List<List<ChatMessage>> conversasSalvas =
            new ArrayList<>();


    @FXML
    public void initialize() {

        groqService = new GroqService();

        historico = new ArrayList<>();

        iniciarHistorico();

        temaEscuro = false;

        botaoTema.setText("🌙 Escuro");

        configurarAtalhos();
    }


    /*
     * =========================================
     * INICIAR HISTÓRICO
     * =========================================
     */

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


    /*
     * =========================================
     * ATALHOS
     * =========================================
     */

    private void configurarAtalhos() {

        rootPane.addEventFilter(
                KeyEvent.KEY_PRESSED,
                evento -> {

                    if (evento.isControlDown()
                            && evento.getCode() == KeyCode.N) {

                        novaConversa();

                        evento.consume();

                        return;
                    }


                    if (evento.isControlDown()
                            && evento.getCode() == KeyCode.L) {

                        campoMensagem.clear();

                        campoMensagem.requestFocus();

                        evento.consume();
                    }
                }
        );
    }


    /*
     * =========================================
     * ENVIAR MENSAGEM
     * =========================================
     */

    @FXML
    private void enviarMensagem() {

        String mensagem =
                campoMensagem.getText().trim();

        if (mensagem.isEmpty()) {
            return;
        }


        /*
         * Se estamos em uma conversa antiga,
         * ela continuará sendo aquela conversa.
         *
         * Se estamos em uma conversa nova,
         * continua com -1.
         */
        int idDaMensagem =
                idConversaAtual;


        campoMensagem.clear();


        adicionarBalao(
                "Você",
                mensagem,
                true
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
                        resposta ->
                                receberResposta(
                                        resposta,
                                        idDaMensagem
                                )
                )
                .exceptionally(
                        erro ->
                                tratarErro(
                                        erro,
                                        idDaMensagem
                                )
                );
    }


    /*
     * =========================================
     * RECEBER RESPOSTA
     * =========================================
     */

    private void receberResposta(
            String resposta,
            int idDaMensagem
    ) {

        Platform.runLater(() -> {

            /*
             * Impede resposta atrasada de aparecer
             * em outra conversa.
             */
            if (idDaMensagem != idConversaAtual) {

                return;
            }


            ultimaRespostaIA = resposta;


            adicionarBalao(
                    "IA",
                    resposta,
                    false
            );


            historico.add(
                    new ChatMessage(
                            "assistant",
                            resposta
                    )
            );


            /*
             * Se estamos dentro de uma conversa
             * antiga, atualiza essa conversa salva.
             */
            if (conversaSalvaAtual >= 0) {

                atualizarConversaSalva();
            }


            liberarInterface();
        });
    }


    /*
     * =========================================
     * CRIAR BALÃO
     * =========================================
     */

    private void adicionarBalao(
            String autor,
            String mensagem,
            boolean usuario
    ) {

        Label balao = new Label();

        balao.setText(
                autor + ":\n" + mensagem
        );

        balao.setWrapText(true);

        balao.setMaxWidth(650);

        balao.getStyleClass().add(
                "balao-mensagem"
        );


        HBox linha = new HBox();

        linha.setMaxWidth(
                Double.MAX_VALUE
        );


        if (usuario) {

            linha.setAlignment(
                    Pos.CENTER_RIGHT
            );

            balao.getStyleClass().add(
                    "balao-usuario"
            );

        } else {

            linha.setAlignment(
                    Pos.CENTER_LEFT
            );

            balao.getStyleClass().add(
                    "balao-ia"
            );
        }


        linha.getChildren().add(balao);

        chatBox.getChildren().add(linha);


        Platform.runLater(() ->
                scrollChat.setVvalue(1.0)
        );
    }


    /*
     * =========================================
     * NOVA CONVERSA
     * =========================================
     */

    @FXML
    private void novaConversa() {

        /*
         * Primeiro invalida qualquer resposta
         * que ainda esteja chegando da conversa anterior.
         */
        idConversaAtual++;


        /*
         * Se for uma conversa NOVA, salva ela.
         *
         * Se for uma conversa antiga que já está
         * salva, NÃO cria outra cópia.
         */
        if (conversaSalvaAtual == -1) {

            if (possuiMensagens()) {

                salvarConversaAtual();
            }
        }


        /*
         * Agora realmente começa uma nova conversa.
         */
        conversaSalvaAtual = -1;


        chatBox.getChildren().clear();

        iniciarHistorico();

        campoMensagem.clear();

        liberarInterface();

        campoMensagem.requestFocus();
    }


    /*
     * =========================================
     * VERIFICAR SE POSSUI MENSAGENS
     * =========================================
     */

    private boolean possuiMensagens() {

        for (ChatMessage mensagem : historico) {

            if ("user".equals(
                    mensagem.getRole()
            )) {

                return true;
            }
        }

        return false;
    }


    /*
     * =========================================
     * SALVAR CONVERSA NOVA
     * =========================================
     */

    private void salvarConversaAtual() {

        List<ChatMessage> copia =
                copiarHistorico();


        if (copia.isEmpty()) {
            return;
        }


        /*
         * Verifica se essa conversa já existe
         * exatamente igual no histórico.
         */
        for (List<ChatMessage> conversa :
                conversasSalvas) {

            if (mesmasConversas(
                    conversa,
                    copia
            )) {

                return;
            }
        }


        conversasSalvas.add(copia);

        atualizarHistoricoVisual();
    }


    /*
     * =========================================
     * ATUALIZAR CONVERSA EXISTENTE
     * =========================================
     */

    private void atualizarConversaSalva() {

        if (conversaSalvaAtual < 0
                || conversaSalvaAtual >=
                conversasSalvas.size()) {

            return;
        }


        conversasSalvas.set(
                conversaSalvaAtual,
                copiarHistorico()
        );


        atualizarHistoricoVisual();
    }


    /*
     * =========================================
     * COPIAR HISTÓRICO
     * =========================================
     */

    private List<ChatMessage> copiarHistorico() {

        List<ChatMessage> copia =
                new ArrayList<>();


        for (ChatMessage mensagem :
                historico) {

            copia.add(
                    new ChatMessage(
                            mensagem.getRole(),
                            mensagem.getContent()
                    )
            );
        }


        return copia;
    }


    /*
     * =========================================
     * COMPARAR CONVERSAS
     * =========================================
     */

    private boolean mesmasConversas(
            List<ChatMessage> primeira,
            List<ChatMessage> segunda
    ) {

        if (primeira.size() != segunda.size()) {

            return false;
        }


        for (int i = 0;
             i < primeira.size();
             i++) {

            ChatMessage mensagem1 =
                    primeira.get(i);

            ChatMessage mensagem2 =
                    segunda.get(i);


            if (!mensagem1.getRole()
                    .equals(
                            mensagem2.getRole()
                    )) {

                return false;
            }


            if (!mensagem1.getContent()
                    .equals(
                            mensagem2.getContent()
                    )) {

                return false;
            }
        }


        return true;
    }


    /*
     * =========================================
     * ATUALIZAR HISTÓRICO VISUAL
     * =========================================
     */

    private void atualizarHistoricoVisual() {

        historicoBox.getChildren().clear();


        for (int i = 0;
             i < conversasSalvas.size();
             i++) {

            final int indice = i;


            String titulo =
                    criarTituloConversa(
                            conversasSalvas.get(i)
                    );


            Label conversa =
                    new Label(titulo);


            conversa.setMaxWidth(
                    Double.MAX_VALUE
            );

            conversa.setWrapText(true);


            conversa.getStyleClass().add(
                    "item-historico"
            );


            /*
             * Quando clicar, abre a conversa.
             */
            conversa.setOnMouseClicked(
                    evento ->
                            abrirConversa(indice)
            );


            historicoBox.getChildren().add(
                    conversa
            );
        }
    }


    /*
     * =========================================
     * TÍTULO AUTOMÁTICO
     * =========================================
     */

    private String criarTituloConversa(
            List<ChatMessage> conversa
    ) {

        for (ChatMessage mensagem :
                conversa) {

            if ("user".equals(
                    mensagem.getRole()
            )) {

                String texto =
                        mensagem.getContent().trim();


                if (texto.length() > 25) {

                    texto =
                            texto.substring(0, 25)
                                    + "...";
                }


                return "💬 " + texto;
            }
        }


        return "💬 Nova conversa";
    }


    /*
     * =========================================
     * ABRIR CONVERSA ANTIGA
     * =========================================
     */

    private void abrirConversa(int indice) {

        if (indice < 0
                || indice >=
                conversasSalvas.size()) {

            return;
        }


        /*
         * Se estiver esperando resposta da IA,
         * não troca de conversa.
         */
        if (campoMensagem.isDisabled()) {

            return;
        }


        /*
         * Nova identificação para a conversa aberta.
         */
        idConversaAtual++;


        /*
         * Muito importante:
         * agora sabemos exatamente qual conversa
         * do histórico estamos visualizando.
         */
        conversaSalvaAtual = indice;


        List<ChatMessage> conversaSalva =
                conversasSalvas.get(indice);


        historico =
                new ArrayList<>();


        for (ChatMessage mensagem :
                conversaSalva) {

            historico.add(
                    new ChatMessage(
                            mensagem.getRole(),
                            mensagem.getContent()
                    )
            );
        }


        chatBox.getChildren().clear();

        ultimaRespostaIA = "";


        /*
         * Mostra novamente todas as mensagens.
         */
        for (ChatMessage mensagem :
                historico) {

            if ("user".equals(
                    mensagem.getRole()
            )) {

                adicionarBalao(
                        "Você",
                        mensagem.getContent(),
                        true
                );

            } else if ("assistant".equals(
                    mensagem.getRole()
            )) {

                ultimaRespostaIA =
                        mensagem.getContent();


                adicionarBalao(
                        "IA",
                        mensagem.getContent(),
                        false
                );
            }
        }


        campoMensagem.clear();

        liberarInterface();

        campoMensagem.requestFocus();
    }


    /*
     * =========================================
     * COPIAR RESPOSTA
     * =========================================
     */

    @FXML
    private void copiarResposta() {

        if (ultimaRespostaIA == null
                || ultimaRespostaIA.isEmpty()) {

            return;
        }


        Clipboard clipboard =
                Clipboard.getSystemClipboard();


        ClipboardContent conteudo =
                new ClipboardContent();


        conteudo.putString(
                ultimaRespostaIA
        );


        clipboard.setContent(
                conteudo
        );
    }


    /*
     * =========================================
     * TEMA
     * =========================================
     */

    @FXML
    private void alternarTema() {

        temaEscuro = !temaEscuro;


        if (temaEscuro) {

            if (!rootPane.getStyleClass()
                    .contains("tema-escuro")) {

                rootPane.getStyleClass().add(
                        "tema-escuro"
                );
            }


            botaoTema.setText(
                    "☀ Claro"
            );

        } else {

            rootPane.getStyleClass().remove(
                    "tema-escuro"
            );


            botaoTema.setText(
                    "🌙 Escuro"
            );
        }
    }


    /*
     * =========================================
     * OPÇÕES
     * =========================================
     */

    @FXML
    private void abrirOpcoes() {

        adicionarBalao(
                "Sistema",
                "As opções do aplicativo estarão disponíveis aqui.",
                false
        );
    }


    /*
     * =========================================
     * TRATAMENTO DE ERRO
     * =========================================
     */

    private Void tratarErro(
            Throwable erro,
            int idDaMensagem
    ) {

        Platform.runLater(() -> {

            /*
             * Não mostra erro de uma conversa
             * antiga em outra conversa.
             */
            if (idDaMensagem != idConversaAtual) {

                return;
            }


            String mensagemErro;


            if (erro.getCause() != null
                    && erro.getCause().getMessage() != null) {

                mensagemErro =
                        erro.getCause().getMessage();

            } else if (erro.getMessage() != null) {

                mensagemErro =
                        erro.getMessage();

            } else {

                mensagemErro =
                        "Não foi possível se comunicar com a IA.";
            }


            adicionarBalao(
                    "Erro",
                    mensagemErro,
                    false
            );


            liberarInterface();
        });


        return null;
    }


    /*
     * =========================================
     * BLOQUEAR INTERFACE
     * =========================================
     */

    private void bloquearInterface() {

        campoMensagem.setDisable(true);

        botaoEnviar.setDisable(true);

        botaoNovaConversa.setDisable(true);
    }


    /*
     * =========================================
     * LIBERAR INTERFACE
     * =========================================
     */

    private void liberarInterface() {

        campoMensagem.setDisable(false);

        botaoEnviar.setDisable(false);

        botaoNovaConversa.setDisable(false);

        campoMensagem.requestFocus();
    }
}