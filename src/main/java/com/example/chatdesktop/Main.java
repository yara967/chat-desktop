package com.example.chatdesktop;

import com.example.chatdesktop.persistence.BancoDeDados;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader loader = new FXMLLoader(
                Main.class.getResource(
                        "/com/example/chatdesktop/view/login-view.fxml"
                )
        );

        Scene scene = new Scene(
                loader.load(),
                700,
                600
        );

        stage.setTitle("Chat JavaFX + Groq");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        BancoDeDados.inicializar();
        launch(args);
    }

}