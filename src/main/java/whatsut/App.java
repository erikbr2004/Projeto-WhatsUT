package whatsut;

import javafx.application.Application;
import javafx.stage.Stage;
import whatsut.views.LoginView;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        LoginView.show(stage);
    }

    public static void main(String[] args) {
        launch();
    }
}
