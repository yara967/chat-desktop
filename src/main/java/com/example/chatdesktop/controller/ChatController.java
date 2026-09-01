package com.example.chatdesktop.controller;

import com.example.chatdesktop.animacoes.AnimacoesChat;
import com.example.chatdesktop.animacoes.ParticulasFundo;
import com.example.chatdesktop.model.ChatMessage;
import com.example.chatdesktop.persistence.ConversaDAO;
import com.example.chatdesktop.service.GroqService;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public class ChatController {

    @FXML
    private StackPane rootPane;

    @FXML
    private ParticulasFundo particulasFundo;

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

    private final List<String> nomesConversas =
            new ArrayList<>();

    /*
     * Guarda o ID real de cada conversa no banco.
     * O índice dessa lista corresponde ao índice
     * de conversasSalvas.
     */
    private final List<Integer> idsConversas =
            new ArrayList<>();

    private int conversaSalvaAtual = -1;

    private int idConversaAtual = 0;

    private String ultimaRespostaIA = "";

    private String ultimaPergunta = "";

    private boolean temaEscuro = false;

    private boolean regenerando = false;

    private final Preferences preferencias =
            Preferences.userNodeForPackage(ChatController.class);


    // =========================================================
    // TOOLTIP
    // =========================================================

    private Tooltip criarTooltipRapida(String texto) {

        Tooltip dica = new Tooltip(texto);

        dica.setShowDelay(Duration.millis(150));
        dica.setHideDelay(Duration.millis(100));

        return dica;
    }


    // =========================================================
    // INICIALIZAÇÃO
    // =========================================================

    @FXML
    public void initialize() {

        groqService = new GroqService();

        historico = new ArrayList<>();

        iniciarHistorico();

        botaoTema.setText("☀");
        botaoTema.setTooltip(
                criarTooltipRapida("Deseja mudar o tema?")
        );

        botaoCopiarResposta.setText("⧉");
        botaoCopiarResposta.setTooltip(
                criarTooltipRapida("Copiar resposta")
        );

        botaoRegenerarResposta.setText("⟳");
        botaoRegenerarResposta.setTooltip(
                criarTooltipRapida("Regenerar resposta")
        );

        indicadorOrigem.setText("");

        botaoRegenerarResposta.setDisable(true);

        configurarAtalhos();

        if (particulasFundo != null) {
            particulasFundo.aplicarTemaClaro();
        }

        carregarConversasDoBanco();

        mostrarEstadoVazioSeNecessario();

        boolean temaSalvoEscuro =
                preferencias.getBoolean("temaEscuro", false);

        if (temaSalvoEscuro) {
            alternarTema();
        }
    }


    // =========================================================
    // CARREGAR CONVERSAS DO BANCO
    // =========================================================

    private void carregarConversasDoBanco() {

        try {

            /*
             * Limpa as listas antes de carregar.
             * Isso evita duplicação caso o método seja chamado novamente.
             */
            conversasSalvas.clear();
            nomesConversas.clear();
            idsConversas.clear();

            List<Integer> ids =
                    ConversaDAO.listarIds();

            List<String> titulos =
                    ConversaDAO.listarTitulos();

            for (int i = 0; i < ids.size(); i++) {

                int id = ids.get(i);

                List<ChatMessage> mensagens =
                        ConversaDAO.buscarMensagens(id);

                if (!mensagens.isEmpty()) {

                    conversasSalvas.add(
                            new ArrayList<>(mensagens)
                    );

                    idsConversas.add(id);

                    if (i < titulos.size()
                            && titulos.get(i) != null
                            && !titulos.get(i).isBlank()) {

                        nomesConversas.add(
                                titulos.get(i)
                        );

                    } else {

                        nomesConversas.add(
                                criarTituloConversa(mensagens)
                                        .replace("💬 ", "")
                        );
                    }
                }
            }

            atualizarHistoricoVisual();

        } catch (Exception e) {

            System.out.println(
                    "Erro ao carregar histórico: "
                            + e.getMessage()
            );
        }
    }


    // =========================================================
    // INICIAR HISTÓRICO
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

        final int idDaMensagem =
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
    // REGENERAR
    // =========================================================

    @FXML
    private void regenerarResposta() {

        if (regenerando) {
            return;
        }

        if (ultimaPergunta == null
                || ultimaPergunta.isBlank()) {

            return;
        }

        if (ultimaRespostaIA == null
                || ultimaRespostaIA.isBlank()) {

            return;
        }

        regenerando = true;

        final int idDaMensagem =
                idConversaAtual;

        List<ChatMessage> historicoRegeneracao =
                copiarHistorico();

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

        botaoRegenerarResposta.setDisable(true);
        botaoEnviar.setDisable(true);
        botaoNovaConversa.setDisable(true);
        campoMensagem.setDisable(true);

        groqService
                .regenerarResposta(historicoRegeneracao)
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

            salvarOuAtualizarNoBanco();

            if (conversaSalvaAtual >= 0) {

                atualizarConversaSalva();
            }

            regenerando = false;

            campoMensagem.setDisable(false);
            botaoEnviar.setDisable(false);
            botaoNovaConversa.setDisable(false);

            botaoRegenerarResposta.setDisable(false);

            campoMensagem.requestFocus();
        });
    }


    // =========================================================
    // SALVAR OU ATUALIZAR BANCO
    // =========================================================

    private void salvarOuAtualizarNoBanco() {

        try {

            List<ChatMessage> mensagens =
                    copiarHistorico();

            if (!possuiMensagens()) {
                return;
            }

            String titulo =
                    criarTituloConversa(mensagens)
                            .replace("💬 ", "");

            /*
             * Se ainda não existe no banco,
             * cria uma nova conversa.
             */
            if (conversaSalvaAtual == -1) {

                int novoId =
                        ConversaDAO.criarConversa(titulo);

                if (novoId != -1) {

                    idConversaAtual = novoId;

                    for (ChatMessage mensagem :
                            mensagens) {

                        if (!"system".equals(
                                mensagem.getRole()
                        )) {

                            ConversaDAO.salvarMensagem(
                                    novoId,
                                    mensagem
                            );
                        }
                    }

                    conversasSalvas.add(
                            copiarHistorico()
                    );

                    nomesConversas.add(
                            titulo
                    );

                    idsConversas.add(
                            novoId
                    );

                    conversaSalvaAtual =
                            conversasSalvas.size() - 1;

                    atualizarHistoricoVisual();
                }

            } else {

                /*
                 * A conversa já existe.
                 *
                 * Atualizamos o conteúdo dela no banco.
                 */
                if (conversaSalvaAtual <
                        conversasSalvas.size()
                        && conversaSalvaAtual <
                        idsConversas.size()) {

                    int idBanco =
                            idsConversas.get(
                                    conversaSalvaAtual
                            );

                    atualizarMensagensNoBanco(
                            idBanco,
                            mensagens
                    );

                    ConversaDAO.atualizarTitulo(
                            idBanco,
                            titulo
                    );

                    if (conversaSalvaAtual <
                            nomesConversas.size()) {

                        nomesConversas.set(
                                conversaSalvaAtual,
                                titulo
                        );
                    }

                    conversasSalvas.set(
                            conversaSalvaAtual,
                            copiarHistorico()
                    );

                    atualizarHistoricoVisual();
                }
            }

        } catch (Exception e) {

            System.out.println(
                    "Erro ao salvar conversa: "
                            + e.getMessage()
            );
        }
    }


    // =========================================================
    // ATUALIZAR MENSAGENS NO BANCO
    // =========================================================

    private void atualizarMensagensNoBanco(
            int conversaId,
            List<ChatMessage> mensagens
    ) {

        /*
         * Como o DAO atual não possui um método para
         * atualizar todas as mensagens, removemos as
         * mensagens antigas e salvamos novamente.
         */

        ConversaDAO.excluirMensagens(conversaId);

        for (ChatMessage mensagem :
                mensagens) {

            if (!"system".equals(
                    mensagem.getRole()
            )) {

                ConversaDAO.salvarMensagem(
                        conversaId,
                        mensagem
                );
            }
        }
    }


    // =========================================================
    // REMOVER ÚLTIMA RESPOSTA
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
    // REMOVER BALÃO IA
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
    // INDICADOR DE ORIGEM
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
    // ESTADO VAZIO
    // =========================================================

    private void mostrarEstadoVazioSeNecessario() {

        if (!chatBox.getChildren().isEmpty()) {
            return;
        }

        VBox estadoVazio =
                new VBox(10);

        estadoVazio.setId(
                "estado-vazio"
        );

        estadoVazio.getStyleClass().add(
                "estado-vazio-container"
        );

        estadoVazio.setAlignment(
                Pos.CENTER
        );

        estadoVazio.setMaxWidth(
                Double.MAX_VALUE
        );

        estadoVazio.setMaxHeight(
                Double.MAX_VALUE
        );

        VBox.setVgrow(
                estadoVazio,
                javafx.scene.layout.Priority.ALWAYS
        );

        Label icone =
                new Label("✦");

        icone.getStyleClass().add(
                "estado-vazio-icone"
        );

        Label texto =
                new Label(
                        "Oi! Sobre o que vamos conversar hoje?"
                );

        texto.getStyleClass().add(
                "estado-vazio-texto"
        );

        texto.setWrapText(true);

        texto.setMaxWidth(320);

        texto.setTextAlignment(
                javafx.scene.text.TextAlignment.CENTER
        );

        estadoVazio.getChildren().addAll(
                icone,
                texto
        );

        chatBox.getChildren().add(
                estadoVazio
        );

        AnimacoesChat.entradaMensagem(
                estadoVazio
        );
    }


    private void removerEstadoVazioSeExistir() {

        chatBox.getChildren().removeIf(
                no ->
                        "estado-vazio".equals(
                                no.getId()
                        )
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

        removerEstadoVazioSeExistir();

        if (mensagem == null) {
            mensagem = "";
        }

        Label balao =
                new Label();

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

        HBox linha =
                new HBox();

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

        linha.getChildren().add(
                balao
        );

        chatBox.getChildren().add(
                linha
        );

        AnimacoesChat.entradaMensagem(
                linha
        );

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
         * A conversa já é salva automaticamente quando
         * a IA responde.
         *
         * Se por algum motivo ainda não estiver salva,
         * salvamos antes de criar a nova.
         */
        if (possuiMensagens()
                && conversaSalvaAtual == -1) {

            salvarConversaAtual();
        }

        idConversaAtual++;

        conversaSalvaAtual = -1;

        regenerando = false;

        chatBox.getChildren().clear();

        iniciarHistorico();

        mostrarEstadoVazioSeNecessario();

        campoMensagem.clear();

        liberarInterface();

        campoMensagem.requestFocus();
    }


    // =========================================================
    // POSSUI MENSAGENS
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

        if (copia.isEmpty()
                || !possuiMensagens()) {

            return;
        }

        String titulo =
                criarTituloConversa(copia)
                        .replace("💬 ", "");

        int idBanco =
                ConversaDAO.criarConversa(titulo);

        if (idBanco == -1) {
            return;
        }

        for (ChatMessage mensagem :
                copia) {

            if (!"system".equals(
                    mensagem.getRole()
            )) {

                ConversaDAO.salvarMensagem(
                        idBanco,
                        mensagem
                );
            }
        }

        conversasSalvas.add(copia);

        nomesConversas.add(titulo);

        idsConversas.add(idBanco);

        conversaSalvaAtual =
                conversasSalvas.size() - 1;

        idConversaAtual = idBanco;

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

            String titulo;

            if (indice < nomesConversas.size()
                    && nomesConversas.get(indice) != null
                    && !nomesConversas.get(indice).isBlank()) {

                titulo =
                        "💬 "
                                + nomesConversas.get(indice);

            } else {

                titulo =
                        criarTituloConversa(
                                conversa
                        );
            }

            HBox linhaHistorico =
                    new HBox();

            linhaHistorico.setSpacing(5);

            linhaHistorico.setAlignment(
                    Pos.CENTER_LEFT
            );

            linhaHistorico.setMaxWidth(
                    Double.MAX_VALUE
            );

            Label item =
                    new Label(titulo);

            item.setMaxWidth(
                    Double.MAX_VALUE
            );

            item.setWrapText(true);

            item.getStyleClass().add(
                    "item-historico"
            );

            HBox.setHgrow(
                    item,
                    javafx.scene.layout.Priority.ALWAYS
            );

            Button botaoRenomear =
                    new Button("✏");

            botaoRenomear.getStyleClass().add(
                    "botao-renomear"
            );

            botaoRenomear.setFocusTraversable(false);

            botaoRenomear.setOnAction(
                    evento ->
                            renomearConversa(indice)
            );

            Button botaoExcluir =
                    new Button("🗑");

            botaoExcluir.getStyleClass().add(
                    "botao-excluir"
            );

            botaoExcluir.setFocusTraversable(false);

            botaoExcluir.setOnAction(
                    evento ->
                            excluirConversa(indice)
            );

            item.setOnMouseClicked(
                    evento ->
                            abrirConversa(indice)
            );

            linhaHistorico.getChildren().addAll(
                    item,
                    botaoRenomear,
                    botaoExcluir
            );

            historicoBox.getChildren().add(
                    linhaHistorico
            );
        }
    }


    // =========================================================
    // RENOMEAR
    // =========================================================

    private void renomearConversa(int indice) {

        if (indice < 0
                || indice >= conversasSalvas.size()) {

            return;
        }

        String nomeAtual;

        if (indice < nomesConversas.size()
                && nomesConversas.get(indice) != null
                && !nomesConversas.get(indice).isBlank()) {

            nomeAtual =
                    nomesConversas.get(indice);

        } else {

            nomeAtual =
                    criarTituloConversa(
                            conversasSalvas.get(indice)
                    ).replace("💬 ", "");
        }

        mostrarJanelaRenomear(
                indice,
                nomeAtual
        );
    }


    // =========================================================
    // JANELA RENOMEAR
    // =========================================================

    private void mostrarJanelaRenomear(
            int indice,
            String nomeAtual
    ) {

        Stage janela =
                new Stage();

        janela.initStyle(
                StageStyle.TRANSPARENT
        );

        janela.initModality(
                Modality.APPLICATION_MODAL
        );

        if (rootPane.getScene() != null) {

            janela.initOwner(
                    rootPane.getScene().getWindow()
            );
        }

        janela.setTitle(
                "Renomear conversa"
        );

        janela.setResizable(false);

        Label titulo =
                new Label(
                        "✎ Renomear conversa"
                );

        titulo.setStyle(
                "-fx-font-size: 19px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-text-fill: #5B3E9C;"
        );

        Label descricao =
                new Label(
                        "Digite um novo nome para esta conversa:"
                );

        descricao.setStyle(
                "-fx-font-size: 13px;"
                        + "-fx-text-fill: #8873B3;"
        );

        TextField campoNome =
                new TextField(nomeAtual);

        campoNome.setPrefWidth(320);

        campoNome.setStyle(
                "-fx-background-color: #F5F0FB;"
                        + "-fx-background-radius: 10;"
                        + "-fx-border-color: #D8C7F0;"
                        + "-fx-border-radius: 10;"
                        + "-fx-padding: 10;"
                        + "-fx-font-size: 14px;"
                        + "-fx-text-fill: #4C3A6E;"
        );

        Button botaoCancelar =
                new Button("Cancelar");

        botaoCancelar.setStyle(
                "-fx-background-color: #EDE4F8;"
                        + "-fx-text-fill: #6A31C9;"
                        + "-fx-background-radius: 10;"
                        + "-fx-padding: 9 18;"
                        + "-fx-font-weight: bold;"
                        + "-fx-cursor: hand;"
        );

        Button botaoSalvar =
                new Button("Salvar");

        botaoSalvar.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #8B6FCB, #6A31C9);"
                        + "-fx-text-fill: white;"
                        + "-fx-background-radius: 10;"
                        + "-fx-padding: 9 22;"
                        + "-fx-font-weight: bold;"
                        + "-fx-cursor: hand;"
        );

        HBox botoes =
                new HBox(
                        10,
                        botaoCancelar,
                        botaoSalvar
                );

        botoes.setAlignment(
                Pos.CENTER_RIGHT
        );

        VBox conteudo =
                new VBox(
                        12,
                        titulo,
                        descricao,
                        campoNome,
                        botoes
                );

        conteudo.setPadding(
                new Insets(25)
        );

        conteudo.setStyle(
                "-fx-background-color: white;"
                        + "-fx-background-radius: 18;"
                        + "-fx-border-color: #E4D6F4;"
                        + "-fx-border-radius: 18;"
                        + "-fx-border-width: 1;"
        );

        DropShadow sombra =
                new DropShadow();

        sombra.setRadius(24);

        sombra.setColor(
                javafx.scene.paint.Color.rgb(
                        76,
                        42,
                        133,
                        0.35
                )
        );

        conteudo.setEffect(sombra);

        botaoCancelar.setOnAction(
                evento ->
                        janela.close()
        );

        botaoSalvar.setOnAction(
                evento -> {

                    String novoNome =
                            campoNome
                                    .getText()
                                    .trim();

                    if (novoNome.isEmpty()) {

                        campoNome.requestFocus();

                        return;
                    }

                    if (novoNome.length() > 35) {

                        novoNome =
                                novoNome.substring(
                                        0,
                                        35
                                );
                    }

                    if (indice <
                            nomesConversas.size()) {

                        nomesConversas.set(
                                indice,
                                novoNome
                        );

                    } else {

                        while (
                                nomesConversas.size()
                                        <= indice
                        ) {

                            nomesConversas.add("");
                        }

                        nomesConversas.set(
                                indice,
                                novoNome
                        );
                    }

                    /*
                     * Agora também salva o nome no banco.
                     */
                    if (indice <
                            idsConversas.size()) {

                        int idBanco =
                                idsConversas.get(indice);

                        ConversaDAO.atualizarTitulo(
                                idBanco,
                                novoNome
                        );
                    }

                    atualizarHistoricoVisual();

                    janela.close();
                }
        );

        campoNome.setOnKeyPressed(
                evento -> {

                    if (evento.getCode()
                            == KeyCode.ENTER) {

                        botaoSalvar.fire();

                    } else if (
                            evento.getCode()
                                    == KeyCode.ESCAPE
                    ) {

                        janela.close();
                    }
                }
        );

        Scene cena =
                new Scene(
                        conteudo,
                        390,
                        205
                );

        cena.setFill(
                javafx.scene.paint.Color.TRANSPARENT
        );

        janela.setScene(cena);

        janela.showAndWait();
    }


    // =========================================================
    // EXCLUIR
    // =========================================================

    private void excluirConversa(int indice) {

        if (indice < 0
                || indice >= conversasSalvas.size()) {

            return;
        }

        String titulo;

        if (indice < nomesConversas.size()
                && nomesConversas.get(indice) != null
                && !nomesConversas.get(indice).isBlank()) {

            titulo =
                    nomesConversas.get(indice);

        } else {

            titulo =
                    criarTituloConversa(
                            conversasSalvas.get(indice)
                    ).replace("💬 ", "");
        }

        boolean confirmou =
                mostrarJanelaExclusao(
                        titulo
                );

        if (!confirmou) {
            return;
        }

        /*
         * =====================================================
         * CORREÇÃO PRINCIPAL DO BUG
         * =====================================================
         *
         * Agora excluímos também a conversa do BANCO.
         */
        if (indice < idsConversas.size()) {

            int idBanco =
                    idsConversas.get(indice);

            ConversaDAO.excluirConversa(
                    idBanco
            );
        }

        /*
         * Se a conversa excluída era a que estava aberta,
         * limpamos a tela.
         */
        if (indice == conversaSalvaAtual) {

            idConversaAtual++;

            conversaSalvaAtual = -1;

            chatBox.getChildren().clear();

            iniciarHistorico();

            mostrarEstadoVazioSeNecessario();

            campoMensagem.clear();

            liberarInterface();

            campoMensagem.requestFocus();

        } else if (conversaSalvaAtual > indice) {

            conversaSalvaAtual--;
        }

        /*
         * Remove das listas da memória.
         */
        conversasSalvas.remove(indice);

        if (indice <
                nomesConversas.size()) {

            nomesConversas.remove(indice);
        }

        if (indice <
                idsConversas.size()) {

            idsConversas.remove(indice);
        }

        atualizarHistoricoVisual();
    }


    // =========================================================
    // JANELA EXCLUSÃO
    // =========================================================

    private boolean mostrarJanelaExclusao(
            String nomeConversa
    ) {

        Stage janela =
                new Stage();

        janela.initStyle(
                StageStyle.TRANSPARENT
        );

        janela.initModality(
                Modality.APPLICATION_MODAL
        );

        if (rootPane.getScene() != null) {

            janela.initOwner(
                    rootPane.getScene().getWindow()
            );
        }

        janela.setTitle(
                "Excluir conversa"
        );

        janela.setResizable(false);

        final boolean[] confirmou =
                {false};

        Label icone =
                new Label("🗑");

        icone.setStyle(
                "-fx-font-size: 30px;"
        );

        Label titulo =
                new Label(
                        "Excluir conversa?"
                );

        titulo.setStyle(
                "-fx-font-size: 19px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-text-fill: #5B3E9C;"
        );

        Label nome =
                new Label(
                        "\"" + nomeConversa + "\""
                );

        nome.setWrapText(true);

        nome.setMaxWidth(320);

        nome.setStyle(
                "-fx-font-size: 14px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-text-fill: #6A31C9;"
        );

        Label aviso =
                new Label(
                        "Essa conversa será removida do histórico."
                                + "\n"
                                + "Essa ação não poderá ser desfeita."
                );

        aviso.setWrapText(true);

        aviso.setStyle(
                "-fx-font-size: 13px;"
                        + "-fx-text-fill: #8873B3;"
        );

        Button botaoCancelar =
                new Button("Cancelar");

        botaoCancelar.setStyle(
                "-fx-background-color: #EDE4F8;"
                        + "-fx-text-fill: #6A31C9;"
                        + "-fx-background-radius: 10;"
                        + "-fx-padding: 9 18;"
                        + "-fx-font-weight: bold;"
                        + "-fx-cursor: hand;"
        );

        Button botaoExcluir =
                new Button("Excluir");

        botaoExcluir.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #A166B0, #6A31C9);"
                        + "-fx-text-fill: white;"
                        + "-fx-background-radius: 10;"
                        + "-fx-padding: 9 22;"
                        + "-fx-font-weight: bold;"
                        + "-fx-cursor: hand;"
        );

        HBox botoes =
                new HBox(
                        10,
                        botaoCancelar,
                        botaoExcluir
                );

        botoes.setAlignment(
                Pos.CENTER_RIGHT
        );

        VBox conteudo =
                new VBox(
                        10,
                        icone,
                        titulo,
                        nome,
                        aviso,
                        botoes
                );

        conteudo.setPadding(
                new Insets(25)
        );

        conteudo.setStyle(
                "-fx-background-color: white;"
                        + "-fx-background-radius: 18;"
                        + "-fx-border-color: #E4D6F4;"
                        + "-fx-border-radius: 18;"
                        + "-fx-border-width: 1;"
        );

        DropShadow sombra =
                new DropShadow();

        sombra.setRadius(24);

        sombra.setColor(
                javafx.scene.paint.Color.rgb(
                        76,
                        42,
                        133,
                        0.35
                )
        );

        conteudo.setEffect(sombra);

        botaoCancelar.setOnAction(
                evento ->
                        janela.close()
        );

        botaoExcluir.setOnAction(
                evento -> {

                    confirmou[0] = true;

                    janela.close();
                }
        );

        Scene cena =
                new Scene(
                        conteudo,
                        390,
                        245
                );

        cena.setFill(
                javafx.scene.paint.Color.TRANSPARENT
        );

        janela.setScene(cena);

        janela.showAndWait();

        return confirmou[0];
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

                texto =
                        texto
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
                || indice >= conversasSalvas.size()) {

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

            if ("system".equals(role)) {
                continue;
            }

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

        Platform.runLater(
                () ->
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

        preferencias.putBoolean(
                "temaEscuro",
                temaEscuro
        );

        if (temaEscuro) {

            if (!rootPane.getStyleClass()
                    .contains("tema-escuro")) {

                rootPane.getStyleClass().add(
                        "tema-escuro"
                );
            }

            botaoTema.setText("🌙");

            botaoTema.setTooltip(
                    criarTooltipRapida(
                            "Deseja mudar o tema?"
                    )
            );

            if (particulasFundo != null) {
                particulasFundo.aplicarTemaEscuro();
            }

        } else {

            rootPane.getStyleClass().remove(
                    "tema-escuro"
            );

            botaoTema.setText("☀");

            botaoTema.setTooltip(
                    criarTooltipRapida(
                            "Deseja mudar o tema?"
                    )
            );

            if (particulasFundo != null) {
                particulasFundo.aplicarTemaClaro();
            }
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

