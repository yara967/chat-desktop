module com.example.chatdesktop {

    requires javafx.controls;
    requires javafx.fxml;

    requires com.google.gson;
    requires java.net.http;
    requires java.prefs;

// SQLite
    requires java.sql;
    requires org.xerial.sqlitejdbc;

    opens com.example.chatdesktop.controller to javafx.fxml;
    opens com.example.chatdesktop.model to com.google.gson;

    exports com.example.chatdesktop;
    exports com.example.chatdesktop.animacoes;
    exports com.example.chatdesktop.service;
    exports com.example.chatdesktop.config;

}
