package com.example.chatdesktop;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML
    private TextField usuarioField;

    @FXML
    private PasswordField senhaField;

    @FXML
    private void fazerLogin() {

        String usuario = usuarioField.getText();
        String senha = senhaField.getText();

        if (usuario.equals("admin") && senha.equals("123")) {

            System.out.println("Login realizado com sucesso!");

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

                Stage stage = (Stage) usuarioField.getScene().getWindow();

                stage.setTitle("Chat JavaFX + Groq");
                stage.setScene(scene);
                stage.show();

            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            System.out.println("Usuário ou senha incorretos!");
        }
    }
}