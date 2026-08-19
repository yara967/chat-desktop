package com.example.chatdesktop;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HelloApplication extends Application {


private static final String API_KEY = System.getenv("GROQ_API_KEY");

    private final TextArea chat = new TextArea();
    private final TextField mensagem = new TextField();
    private final Button enviar = new Button("Enviar");

    @Override
    public void start(Stage stage) {

        chat.setEditable(false);
        chat.setWrapText(true);

        mensagem.setPromptText("Digite sua mensagem...");

        enviar.setOnAction(e -> enviarMensagem());

        mensagem.setOnAction(e -> enviarMensagem());

        HBox entrada = new HBox(10);
        entrada.setPadding(new Insets(10));
        entrada.getChildren().addAll(mensagem, enviar);

        BorderPane layout = new BorderPane();

        layout.setCenter(chat);
        layout.setBottom(entrada);

        Scene scene = new Scene(layout, 600, 500);

        stage.setTitle("Chat com Groq");
        stage.setScene(scene);
        stage.show();
    }

    private void enviarMensagem() {

        String texto = mensagem.getText().trim();

        if (texto.isEmpty()) {
            return;
        }

        chat.appendText("Você: " + texto + "\n\n");

        mensagem.clear();
        enviar.setDisable(true);

        Thread thread = new Thread(() -> {

            try {

                String resposta = chamarGroq(texto);

                Platform.runLater(() -> {
                    chat.appendText("IA: " + resposta + "\n\n");
                    enviar.setDisable(false);
                });

            } catch (Exception e) {

                Platform.runLater(() -> {
                    chat.appendText("Erro: " + e.getMessage() + "\n\n");
                    enviar.setDisable(false);
                });
            }

        });

        thread.setDaemon(true);
        thread.start();
    }

    private String chamarGroq(String texto) throws Exception {

        String json = """
                {
                   "model": "openai/gpt-oss-20b",
                    "messages": [
                        {
                            "role": "user",
                            "content": "%s"
                        }
                    ]
                }
                """.formatted(escaparJson(texto));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "https://api.groq.com/openai/v1/chat/completions"
                ))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpClient client = HttpClient.newHttpClient();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "Erro da Groq: "
                            + response.statusCode()
                            + "\n"
                            + response.body()
            );
        }

        return extrairResposta(response.body());
    }

    private String escaparJson(String texto) {

        return texto
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private String extrairResposta(String json) {

        String marcador = "\"content\":\"";

        int inicio = json.indexOf(marcador);

        if (inicio == -1) {
            return "Não consegui encontrar a resposta da IA.";
        }

        inicio += marcador.length();

        int fim = json.indexOf("\"", inicio);

        if (fim == -1) {
            return "Resposta inválida.";
        }

        return json.substring(inicio, fim)
                .replace("\\n", "\n")
                .replace("\\\"", "\"");
    }

    public static void main(String[] args) {
        launch();
    }
}