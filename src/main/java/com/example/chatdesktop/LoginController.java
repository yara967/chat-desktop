package com.example.chatdesktop;

import com.example.chatdesktop.persistence.BancoDeDados;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginController {

    @FXML
    private TextField usuarioField;

    @FXML
    private PasswordField senhaField;

    @FXML
    private void fazerLogin() {

        String usuario = usuarioField.getText().trim();
        String senha = senhaField.getText();

        String sql = """
                SELECT * FROM usuarios
                WHERE usuario = ? AND senha = ?
                """;

        try (
                Connection conexao = BancoDeDados.conectar();
                PreparedStatement statement = conexao.prepareStatement(sql)
        ) {

            statement.setString(1, usuario);
            statement.setString(2, senha);

            ResultSet resultado = statement.executeQuery();

            if (resultado.next()) {

                System.out.println("Login realizado com sucesso!");

                abrirChat();

            } else {

                System.out.println("Usuário ou senha incorretos!");
            }

        } catch (Exception e) {

            System.out.println("Erro ao realizar login:");
            e.printStackTrace();
        }
    }

    @FXML
    private void abrirCadastro() {

        try {

            FXMLLoader loader = new FXMLLoader(
                    Main.class.getResource(
                            "/com/example/chatdesktop/view/cadastro-view.fxml"
                    )
            );

            Scene scene = new Scene(
                    loader.load(),
                    900,
                    600
            );

            Stage stage =
                    (Stage) usuarioField.getScene().getWindow();

            stage.setTitle("Criar conta - Chat IA");
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    private void abrirChat() {

        try {

            FXMLLoader loader = new FXMLLoader(
                    Main.class.getResource(
                            "/com/example/chatdesktop/view/chat-view.fxml"
                    )
            );

            Scene scene = new Scene(
                    loader.load(),
                    700,
                    600
            );

            Stage stage =
                    (Stage) usuarioField.getScene().getWindow();

            stage.setTitle("Chat JavaFX + Groq");
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}