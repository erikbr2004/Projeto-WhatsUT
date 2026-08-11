package whatsut.views;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginView {

    public static void show(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(LoginView.class.getResource("/whatsut/login-view.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root);

        stage.setWidth(450);
        stage.setHeight(500);
        stage.setMinWidth(400);
        stage.setMinHeight(400);

        scene.getStylesheets().add(LoginView.class.getResource("/whatsut/styles-login.css").toExternalForm());

        stage.setTitle("WhatsUT - Login");
        stage.setScene(scene);
        stage.show();
    }


}
