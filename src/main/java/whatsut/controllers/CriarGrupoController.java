package whatsut.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import whatsut.model.Grupo;
import whatsut.util.GrupoStorage;

import java.util.*;
import java.util.stream.Collectors;

public class CriarGrupoController {

    @FXML
    private TextField nomeGrupoField;

    @FXML private VBox vboxUsuarios;
    private List<CheckBox> checkBoxesUsuarios = new ArrayList<>();


    @FXML
    private RadioButton radioTransferir;

    @FXML
    private RadioButton radioDeletar;

    private ToggleGroup modoSaidaGroup;

    private List<String> allUsers;
    private MainController mainController;
    private String usuarioLogado;

    public void setAllUsers(List<String> users) {
        this.allUsers = users;
        for (String user : users) {
            if (!user.equals(usuarioLogado)) {
                CheckBox cb = new CheckBox(user);
                vboxUsuarios.getChildren().add(cb);
                checkBoxesUsuarios.add(cb);
            }
        }
    }


    public void setMainController(MainController controller) {
        this.mainController = controller;
    }

    public void setUsuarioLogado(String nome) {
        this.usuarioLogado = nome;
    }

    @FXML
    private void initialize() {
        // Inicializa o ToggleGroup dos RadioButtons
        modoSaidaGroup = new ToggleGroup();
        radioTransferir.setToggleGroup(modoSaidaGroup);
        radioDeletar.setToggleGroup(modoSaidaGroup);
        radioTransferir.setSelected(true); // padrão
    }

    @FXML
    private void handleCriarGrupo() {
        String nomeGrupo = nomeGrupoField.getText().trim();
        if (nomeGrupo.isEmpty()) {
            showAlert("Erro", "O nome do grupo é obrigatório.");
            return;
        }

        List<String> selecionados = checkBoxesUsuarios.stream()
                .filter(CheckBox::isSelected)
                .map(CheckBox::getText)
                .collect(Collectors.toList());


        if (selecionados.isEmpty()) {
            showAlert("Erro", "Selecione pelo menos um usuário para o grupo.");
            return;
        }

        if (!selecionados.contains(usuarioLogado)) {
            selecionados.add(usuarioLogado);
        }

        Set<String> membros = new HashSet<>(selecionados);
        String modoSaida = radioTransferir.isSelected() ? "transferir" : "deletar";

        Map<String, Grupo> grupos = GrupoStorage.carregarGrupos();
        if (grupos.containsKey(nomeGrupo)) {
            showAlert("Erro", "Já existe um grupo com esse nome.");
            return;
        }

        Grupo novoGrupo = new Grupo(nomeGrupo, usuarioLogado, membros, modoSaida);
        grupos.put(nomeGrupo, novoGrupo);
        GrupoStorage.salvarGrupos(grupos);

        mainController.carregarListaDeContatos();

        Stage stage = (Stage) nomeGrupoField.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String titulo, String conteudo) {
        Alert alert = new Alert(Alert.AlertType.ERROR, conteudo, ButtonType.OK);
        alert.setHeaderText(titulo);
        alert.showAndWait();
    }
}
