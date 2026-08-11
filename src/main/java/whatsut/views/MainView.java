package whatsut.views;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import whatsut.controllers.MainController;

public class MainView {
    public static void show(Stage stage, String nomeUsuario) throws Exception {
        FXMLLoader loader = new FXMLLoader(MainView.class.getResource("/whatsut/main-view.fxml"));
        Parent root = loader.load();

        //Passa o nome do usuário logado para o controller
        MainController controller = loader.getController();
        controller.setUsuarioLogado(nomeUsuario);

        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add(MainView.class.getResource("/whatsut/styles-main.css").toExternalForm());

        stage.setTitle("WhatsUT - Olá, " + nomeUsuario + "!");
        stage.setScene(scene);

        // Trava tamanho da janela
        stage.setResizable(false);
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        stage.setMaxWidth(800);
        stage.setMaxHeight(600);

        stage.show();
    }
}
