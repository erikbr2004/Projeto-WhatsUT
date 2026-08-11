package whatsut.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import whatsut.interfaces.IServer;
import whatsut.model.Grupo;

import java.rmi.RemoteException;
import java.util.List;
import java.util.stream.Collectors;

public class BanirIntegrantesController {

    @FXML
    private ListView<CheckBox> listCheck;

    private Grupo grupo;
    private IServer servidor;
    private String nomeAdmin;
    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void init(Grupo grupo, IServer servidor, String nomeAdmin) {
        this.grupo = grupo;
        this.servidor = servidor;
        this.nomeAdmin = nomeAdmin;

        for (String membro : grupo.getMembros()) {
            if (!membro.equals(nomeAdmin)) {
                listCheck.getItems().add(new CheckBox(membro));
            }
        }
    }

    @FXML
    private void banirSelecionados() {
        List<String> banidos = listCheck.getItems().stream()
                .filter(CheckBox::isSelected)
                .map(CheckBox::getText)
                .collect(Collectors.toList());

        for (String nome : banidos) {
            try {
                boolean sucesso = servidor.banirUsuarioDoGrupo(grupo.getNome(), nomeAdmin, nome);
                if (!sucesso) {
                    Alert alerta = new Alert(Alert.AlertType.ERROR);
                    alerta.setTitle("Erro");
                    alerta.setHeaderText(null);
                    alerta.setContentText("Não foi possível banir " + nome + ".");
                    alerta.showAndWait();
                }

            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        if (!banidos.isEmpty()) {
            Alert sucesso = new Alert(Alert.AlertType.INFORMATION);
            sucesso.setTitle("Banimento concluído");
            sucesso.setHeaderText(null);
            sucesso.setContentText("Membros banidos com sucesso.");
            sucesso.showAndWait();
        }

        if (mainController != null) {
            mainController.carregarListaDeContatos();
        }

        ((Stage) listCheck.getScene().getWindow()).close();

    }
}
