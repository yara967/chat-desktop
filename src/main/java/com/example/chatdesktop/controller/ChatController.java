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
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class ChatController {

    @FXML
    private StackPane rootPane;

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
    private Button botaoRegenerarResposta;

    @FXML
    private Button botaoTema;

    @FXML
    private VBox historicoBox;

    @FXML
    private Button botaoOpcoes;

    @FXML
    private Label indicadorOrigem;

    private GroqService groqService;

    private List<ChatMessage> historico;

    private final List<List<ChatMessage>> conversasSalvas =
            new ArrayList<>();

    private int conversaSalvaAtual = -1;

    private int idConversaAtual = 0;

    private String ultimaRespostaIA = "";

    private String ultimaPergunta = "";

    private boolean temaEscuro = false;

    /*
     * Controla se já existe uma regeneração em andamento.
     * Isso evita que o primeiro clique seja perdido
     * ou que várias requisições sejam enviadas ao mesmo tempo.
     */
    private boolean regenerando = false;


    // =========================================================
    // INICIALIZAÇÃO
    // =========================================================

    @FXML
    public void initialize() {

        groqService = new GroqService();

        historico = new ArrayList<>();

        iniciarHistorico();

        botaoTema.setText("☀ Claro");

        indicadorOrigem.setText("");

        botaoRegenerarResposta.setDisable(true);

        configurarAtalhos();
    }


    // =========================================================
    // INICIAR CONVERSA
    // =========================================================

    private void iniciarHistorico() {

        historico = new ArrayList<>();

        historico.add(
                new ChatMessage(
                        "system",
                        "Você é um assistente útil, educado e objetivo. "
                                + "Responda sempre em português do Brasil."
                )
        );

        ultimaRespostaIA = "";

        ultimaPergunta = "";

        regenerando = false;

        if (indicadorOrigem != null) {
            indicadorOrigem.setText("");
        }

        if (botaoRegenerarResposta != null) {
            botaoRegenerarResposta.setDisable(true);
        }
    }


    // =========================================================
    // ATALHOS
    // =========================================================

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

                        return;
                    }

                    if (evento.getCode() == KeyCode.ENTER
                            && campoMensagem.isFocused()) {

                        enviarMensagem();

                        evento.consume();
                    }
                }
        );
    }


    // =========================================================
    // ENVIAR MENSAGEM
    // =========================================================

    @FXML
    private void enviarMensagem() {

        String mensagem =
                campoMensagem.getText().trim();

        if (mensagem.isEmpty()) {
            return;
        }

        if (regenerando) {
            return;
        }

        int idDaMensagem =
                idConversaAtual;

        campoMensagem.clear();

        ultimaPergunta = mensagem;

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
                        resultado ->
                                receberResposta(
                                        resultado,
                                        idDaMensagem,
                                        false
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


    // =========================================================
    // REGENERAR RESPOSTA
    // =========================================================

    @FXML
    private void regenerarResposta() {

        /*
         * Se já estiver regenerando, não envia outra requisição.
         */
        if (regenerando) {
            return;
        }

        /*
         * Não existe pergunta para regenerar.
         */
        if (ultimaPergunta == null
                || ultimaPergunta.isBlank()) {

            return;
        }

        /*
         * Não existe resposta anterior.
         */
        if (ultimaRespostaIA == null
                || ultimaRespostaIA.isBlank()) {

            return;
        }

        /*
         * Marca imediatamente como regenerando.
         */
        regenerando = true;

        /*
         * Guarda o ID da conversa.
         */
        int idDaMensagem =
                idConversaAtual;

        /*
         * Cria uma cópia do histórico.
         */
        List<ChatMessage> historicoRegeneracao =
                copiarHistorico();

        /*
         * Remove a resposta anterior.
         *
         * O histórico normalmente termina assim:
         *
         * system
         * user
         * assistant
         *
         * Então removemos somente o assistant.
         */
        if (!historicoRegeneracao.isEmpty()) {

            int ultimoIndice =
                    historicoRegeneracao.size() - 1;

            ChatMessage ultimaMensagem =
                    historicoRegeneracao.get(
                            ultimoIndice
                    );

            if ("assistant".equals(
                    ultimaMensagem.getRole()
            )) {

                historicoRegeneracao.remove(
                        ultimoIndice
                );
            }
        }

        /*
         * Desativa os controles enquanto a IA responde.
         */
        botaoRegenerarResposta.setDisable(true);

        botaoEnviar.setDisable(true);

        botaoNovaConversa.setDisable(true);

        campoMensagem.setDisable(true);

        /*
         * Envia imediatamente a requisição.
         */
        groqService
                .enviarMensagem(historicoRegeneracao)
                .thenAccept(
                        resultado ->
                                receberResposta(
                                        resultado,
                                        idDaMensagem,
                                        true
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


    // =========================================================
    // RECEBER RESPOSTA
    // =========================================================

    private void receberResposta(
            GroqService.ResultadoResposta resultado,
            int idDaMensagem,
            boolean regeneracao
    ) {

        Platform.runLater(() -> {

            if (idDaMensagem != idConversaAtual) {

                regenerando = false;

                return;
            }

            String resposta =
                    resultado.getResposta();

            String origem =
                    resultado.getOrigem();

            String fonte =
                    resultado.getFonte();

            /*
             * Se for regeneração, remove a resposta
             * anterior da tela e do histórico.
             */
            if (regeneracao) {

                removerUltimoBalaoIA();

                removerUltimaRespostaDoHistorico();
            }

            ultimaRespostaIA = resposta;

            adicionarBalao(
                    "IA",
                    resposta,
                    false
            );

            atualizarIndicadorOrigem(
                    origem,
                    fonte
            );

            historico.add(
                    new ChatMessage(
                            "assistant",
                            resposta
                    )
            );

            if (conversaSalvaAtual >= 0) {

                atualizarConversaSalva();
            }

            /*
             * Finaliza a regeneração.
             */
            regenerando = false;

            /*
             * Libera a interface.
             */
            campoMensagem.setDisable(false);

            botaoEnviar.setDisable(false);

            botaoNovaConversa.setDisable(false);

            /*
             * O botão volta a ficar disponível
             * imediatamente após a resposta.
             */
            botaoRegenerarResposta.setDisable(false);

            campoMensagem.requestFocus();
        });
    }


    // =========================================================
    // REMOVER ÚLTIMA RESPOSTA DO HISTÓRICO
    // =========================================================

    private void removerUltimaRespostaDoHistorico() {

        if (historico.isEmpty()) {
            return;
        }

        ChatMessage ultimaMensagem =
                historico.get(
                        historico.size() - 1
                );

        if ("assistant".equals(
                ultimaMensagem.getRole()
        )) {

            historico.remove(
                    historico.size() - 1
            );
        }
    }


    // =========================================================
    // REMOVER ÚLTIMO BALÃO DA IA
    // =========================================================

    private void removerUltimoBalaoIA() {

        if (chatBox.getChildren().isEmpty()) {
            return;
        }

        for (int i =
             chatBox.getChildren().size() - 1;
             i >= 0;
             i--) {

            if (chatBox.getChildren().get(i)
                    instanceof HBox) {

                HBox linha =
                        (HBox) chatBox
                                .getChildren()
                                .get(i);

                if (!linha.getChildren().isEmpty()
                        && linha.getChildren().get(0)
                        instanceof Label) {

                    Label balao =
                            (Label) linha
                                    .getChildren()
                                    .get(0);

                    String texto =
                            balao.getText();

                    if (texto != null
                            && texto.startsWith("IA:")) {

                        chatBox.getChildren().remove(i);

                        break;
                    }
                }
            }
        }
    }


    // =========================================================
    // ATUALIZAR INDICADOR DE ORIGEM
    // =========================================================

    private void atualizarIndicadorOrigem(
            String origem,
            String fonte
    ) {

        if (origem == null || origem.isBlank()) {
            origem = "Desconhecida";
        }

        if (fonte == null || fonte.isBlank()) {
            fonte = "Nenhuma";
        }

        indicadorOrigem.setText(
                "Origem: " + origem
                        + " • Fonte: " + fonte
        );
    }


    // =========================================================
    // ADICIONAR BALÃO
    // =========================================================

    private void adicionarBalao(
            String autor,
            String mensagem,
            boolean usuario
    ) {

        if (mensagem == null) {
            mensagem = "";
        }

        Label balao = new Label();

        balao.setText(
                autor + ":\n" + mensagem
        );

        balao.setWrapText(true);

        balao.setMaxWidth(650);

        balao.setMinHeight(
                javafx.scene.layout.Region.USE_PREF_SIZE
        );

        balao.getStyleClass().add(
                "balao-mensagem"
        );

        HBox linha = new HBox();

        linha.setMaxWidth(
                Double.MAX_VALUE
        );

        linha.setFillHeight(true);

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

        Platform.runLater(() -> {

            scrollChat.layout();

            scrollChat.setVvalue(1.0);
        });
    }


    // =========================================================
    // NOVA CONVERSA
    // =========================================================

    @FXML
    private void novaConversa() {

        idConversaAtual++;

        regenerando = false;

        if (conversaSalvaAtual == -1
                && possuiMensagens()) {

            salvarConversaAtual();
        }

        conversaSalvaAtual = -1;

        chatBox.getChildren().clear();

        iniciarHistorico();

        campoMensagem.clear();

        liberarInterface();

        campoMensagem.requestFocus();
    }


    // =========================================================
    // VERIFICAR MENSAGENS
    // =========================================================

    private boolean possuiMensagens() {

        for (ChatMessage mensagem :
                historico) {

            if ("user".equals(
                    mensagem.getRole()
            )) {

                return true;
            }
        }

        return false;
    }


    // =========================================================
    // SALVAR CONVERSA
    // =========================================================

    private void salvarConversaAtual() {

        List<ChatMessage> copia =
                copiarHistorico();

        if (copia.isEmpty()) {
            return;
        }

        conversasSalvas.add(copia);

        atualizarHistoricoVisual();
    }


    // =========================================================
    // ATUALIZAR CONVERSA SALVA
    // =========================================================

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


    // =========================================================
    // COPIAR HISTÓRICO
    // =========================================================

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


    // =========================================================
    // ATUALIZAR HISTÓRICO VISUAL
    // =========================================================

    private void atualizarHistoricoVisual() {

        historicoBox.getChildren().clear();

        for (int i = 0;
             i < conversasSalvas.size();
             i++) {

            final int indice = i;

            List<ChatMessage> conversa =
                    conversasSalvas.get(i);

            String titulo =
                    criarTituloConversa(conversa);

            Label item =
                    new Label(titulo);

            item.setMaxWidth(
                    Double.MAX_VALUE
            );

            item.setWrapText(true);

            item.getStyleClass().add(
                    "item-historico"
            );

            item.setOnMouseClicked(
                    evento ->
                            abrirConversa(indice)
            );

            historicoBox.getChildren().add(item);
        }
    }


    // =========================================================
    // CRIAR TÍTULO
    // =========================================================

    private String criarTituloConversa(
            List<ChatMessage> conversa
    ) {

        for (ChatMessage mensagem :
                conversa) {

            if ("user".equals(
                    mensagem.getRole()
            )) {

                String texto =
                        mensagem.getContent();

                if (texto == null) {
                    texto = "Nova conversa";
                }

                texto = texto
                        .trim()
                        .replace("\n", " ")
                        .replace("\r", " ");

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


    // =========================================================
    // ABRIR CONVERSA
    // =========================================================

    private void abrirConversa(int indice) {

        if (indice < 0
                || indice >=
                conversasSalvas.size()) {

            return;
        }

        if (campoMensagem.isDisabled()) {
            return;
        }

        idConversaAtual++;

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

        ultimaPergunta = "";

        regenerando = false;

        indicadorOrigem.setText("");

        botaoRegenerarResposta.setDisable(true);

        for (ChatMessage mensagem :
                historico) {

            String role =
                    mensagem.getRole();

            String content =
                    mensagem.getContent();

            if ("user".equals(role)) {

                ultimaPergunta = content;

                adicionarBalao(
                        "Você",
                        content,
                        true
                );

            } else if ("assistant".equals(role)) {

                ultimaRespostaIA = content;

                adicionarBalao(
                        "IA",
                        content,
                        false
                );
            }
        }

        if (!ultimaPergunta.isBlank()
                && !ultimaRespostaIA.isBlank()) {

            botaoRegenerarResposta.setDisable(false);
        }

        campoMensagem.clear();

        liberarInterface();

        campoMensagem.requestFocus();

        Platform.runLater(() ->
                scrollChat.setVvalue(1.0)
        );
    }


    // =========================================================
    // COPIAR RESPOSTA
    // =========================================================

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


    // =========================================================
    // TEMA
    // =========================================================

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
                    "🌙 Escuro"
            );

        } else {

            rootPane.getStyleClass().remove(
                    "tema-escuro"
            );

            botaoTema.setText(
                    "☀ Claro"
            );
        }
    }


    // =========================================================
    // OPÇÕES
    // =========================================================

    @FXML
    private void abrirOpcoes() {

        adicionarBalao(
                "Sistema",
                "As opções do aplicativo estarão disponíveis aqui.",
                false
        );
    }


    // =========================================================
    // TRATAMENTO DE ERRO
    // =========================================================

    private Void tratarErro(
            Throwable erro,
            int idDaMensagem
    ) {

        Platform.runLater(() -> {

            if (idDaMensagem != idConversaAtual) {
                return;
            }

            regenerando = false;

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


    // =========================================================
    // BLOQUEAR INTERFACE
    // =========================================================

    private void bloquearInterface() {

        campoMensagem.setDisable(true);

        botaoEnviar.setDisable(true);

        botaoNovaConversa.setDisable(true);

        botaoRegenerarResposta.setDisable(true);
    }


    // =========================================================
    // LIBERAR INTERFACE
    // =========================================================

    private void liberarInterface() {

        campoMensagem.setDisable(false);

        botaoEnviar.setDisable(false);

        botaoNovaConversa.setDisable(false);

        campoMensagem.requestFocus();

        if (!regenerando
                && ultimaRespostaIA != null
                && !ultimaRespostaIA.isBlank()) {

            botaoRegenerarResposta.setDisable(false);
        }
    }
}