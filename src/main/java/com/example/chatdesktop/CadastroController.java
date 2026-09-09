package com.example.chatdesktop;

import com.example.chatdesktop.persistence.BancoDeDados;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CadastroController {

    @FXML
    private TextField usuarioField;

    @FXML
    private PasswordField senhaField;

    @FXML
    private PasswordField confirmarSenhaField;

    @FXML
    private void cadastrar() {

        String usuario = usuarioField.getText().trim();
        String senha = senhaField.getText();
        String confirmarSenha = confirmarSenhaField.getText();

        if (usuario.isEmpty() || senha.isEmpty() || confirmarSenha.isEmpty()) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Atenção",
                    "Preencha todos os campos."
            );

            return;
        }

        if (!senha.equals(confirmarSenha)) {

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Erro",
                    "As senhas não são iguais."
            );

            return;
        }

        String sql = """
                INSERT INTO usuarios (usuario, senha)
                VALUES (?, ?)
                """;

        try (
                Connection conexao = BancoDeDados.conectar();
                PreparedStatement statement = conexao.prepareStatement(sql)
        ) {

            statement.setString(1, usuario);
            statement.setString(2, senha);

            statement.executeUpdate();

            // ==========================================
            // JANELA DE SUCESSO
            // ==========================================

            Alert alerta = new Alert(Alert.AlertType.INFORMATION);

            alerta.setTitle("Chat IA");
            alerta.setHeaderText("✦  Conta criada!");

            alerta.setContentText(
                    "Sua conta foi criada com sucesso!\n\n" +
                            "Agora você já pode entrar no Chat IA."
            );

            // Fundo geral da janela
            alerta.getDialogPane().setStyle(
                    "-fx-background-color: #F4EFFB;"
            );

            // Cabeçalho lilás
            if (alerta.getDialogPane()
                    .lookup(".header-panel") != null) {

                alerta.getDialogPane()
                        .lookup(".header-panel")
                        .setStyle(
                                "-fx-background-color: #DACBEF;"
                        );
            }

            // Texto da mensagem
            if (alerta.getDialogPane()
                    .lookup(".content.label") != null) {

                alerta.getDialogPane()
                        .lookup(".content.label")
                        .setStyle(
                                "-fx-text-fill: #2E1050;" +
                                        "-fx-font-size: 14px;"
                        );
            }

            // Botão OK
            Button botaoOK =
                    (Button) alerta.getDialogPane()
                            .lookupButton(
                                    alerta.getButtonTypes().get(0)
                            );

            botaoOK.setStyle(
                    "-fx-background-color: #7C3AED;" +
                            "-fx-text-fill: white;" +
                            "-fx-font-size: 13px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-background-radius: 10px;" +
                            "-fx-border-radius: 10px;" +
                            "-fx-padding: 8px 25px;" +
                            "-fx-cursor: hand;"
            );

            alerta.showAndWait();

            // Depois de clicar em OK, volta para o login
            abrirLogin();

        } catch (SQLException e) {

            if (e.getMessage() != null &&
                    e.getMessage().contains("UNIQUE")) {

                mostrarAlerta(
                        Alert.AlertType.ERROR,
                        "Erro",
                        "Esse usuário já está cadastrado."
                );

            } else {

                mostrarAlerta(
                        Alert.AlertType.ERROR,
                        "Erro",
                        "Não foi possível criar a conta."
                );

                e.printStackTrace();
            }
        }
    }

    @FXML
    private void abrirLogin() {

        try {

            FXMLLoader loader = new FXMLLoader(
                    Main.class.getResource(
                            "/com/example/chatdesktop/view/login-view.fxml"
                    )
            );

            Scene scene = new Scene(
                    loader.load(),
                    900,
                    600
            );

            Stage stage =
                    (Stage) usuarioField.getScene().getWindow();

            stage.setTitle("Login - Chat IA");
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    private void mostrarAlerta(
            Alert.AlertType tipo,
            String titulo,
            String mensagem
    ) {

        Alert alerta = new Alert(tipo);

        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensagem);

        alerta.showAndWait();
    }
}