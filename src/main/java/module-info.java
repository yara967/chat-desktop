module com.example.chatdesktop {

    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;

    exports com.example.chatdesktop;
    opens com.example.chatdesktop to javafx.fxml;
}