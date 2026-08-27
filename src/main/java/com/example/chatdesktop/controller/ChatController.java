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
    private Button botaoTema;

    @FXML
    private VBox historicoBox;

    @FXML
    private Button botaoOpcoes;

    @FXML
    private Label indicadorOrigem;


    private GroqService groqService;

    /*
     * Conversa que está sendo exibida atualmente.
     */
    private List<ChatMessage> historico;

    /*
     * Todas as conversas salvas no histórico.
     */
    private final List<List<ChatMessage>> conversasSalvas =
            new ArrayList<>();

    /*
     * Índice da conversa aberta.
     *
     * -1 = conversa nova ainda não salva.
     */
    private int conversaSalvaAtual = -1;

    /*
     * Número que identifica a conversa atual.
     * Serve para impedir respostas atrasadas.
     */
    private int idConversaAtual = 0;

    private String ultimaRespostaIA = "";

    private boolean temaEscuro = false;


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
                        "Você é um assistente útil, educado e objetivo. " +
                                "Responda sempre em português do Brasil."
                )
        );

        ultimaRespostaIA = "";

        if (indicadorOrigem != null) {
            indicadorOrigem.setText("");
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
                        resultado ->
                                receberResposta(
                                        resultado,
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


    // =========================================================
    // RECEBER RESPOSTA
    // =========================================================

    private void receberResposta(
            GroqService.ResultadoResposta resultado,
            int idDaMensagem
    ) {

        Platform.runLater(() -> {

            if (idDaMensagem != idConversaAtual) {
                return;
            }

            String resposta =
                    resultado.getResposta();

            String origem =
                    resultado.getOrigem();

            String fonte =
                    resultado.getFonte();

            ultimaRespostaIA = resposta;

            /*
             * Mostra somente a resposta da IA
             * dentro da conversa.
             */
            adicionarBalao(
                    "IA",
                    resposta,
                    false
            );

            /*
             * Mostra a origem e a fonte
             * no rodapé da aplicação.
             */
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

            /*
             * Se a conversa já está salva,
             * atualiza ela.
             */
            if (conversaSalvaAtual >= 0) {

                atualizarConversaSalva();
            }

            liberarInterface();
        });
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

        /*
         * Garante que nunca vamos criar
         * um balão com mensagem nula.
         */
        if (mensagem == null) {
            mensagem = "";
        }

        Label balao = new Label();

        /*
         * O texto é colocado diretamente no Label.
         */
        balao.setText(
                autor + ":\n" + mensagem
        );

        balao.setWrapText(true);

        /*
         * Permite que o balão cresça
         * conforme a mensagem aumenta.
         */
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

        /*
         * Faz o ScrollPane ir para o final.
         */
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

        /*
         * Invalida respostas antigas.
         */
        idConversaAtual++;

        /*
         * Se estamos em uma conversa nova
         * e ela possui mensagens, salva.
         */
        if (conversaSalvaAtual == -1
                && possuiMensagens()) {

            salvarConversaAtual();
        }

        /*
         * A partir daqui é uma conversa nova.
         */
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

        /*
         * Adiciona uma única conversa.
         */
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

        /*
         * Não deixa trocar de conversa
         * enquanto a IA está respondendo.
         */
        if (campoMensagem.isDisabled()) {
            return;
        }

        idConversaAtual++;

        conversaSalvaAtual = indice;

        List<ChatMessage> conversaSalva =
                conversasSalvas.get(indice);

        /*
         * Faz uma cópia real da conversa.
         */
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

        /*
         * Limpa a tela.
         */
        chatBox.getChildren().clear();

        ultimaRespostaIA = "";

        /*
         * Limpa o indicador porque
         * a conversa salva não guarda
         * atualmente a origem/fonte.
         */
        indicadorOrigem.setText("");

        /*
         * Recria TODOS os balões.
         */
        for (ChatMessage mensagem :
                historico) {

            String role =
                    mensagem.getRole();

            String content =
                    mensagem.getContent();

            if ("user".equals(role)) {

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

        campoMensagem.clear();

        liberarInterface();

        campoMensagem.requestFocus();

        /*
         * Vai para o final da conversa.
         */
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
    }


    // =========================================================
    // LIBERAR INTERFACE
    // =========================================================

    private void liberarInterface() {

        campoMensagem.setDisable(false);

        botaoEnviar.setDisable(false);

        botaoNovaConversa.setDisable(false);

        campoMensagem.requestFocus();
    }
}