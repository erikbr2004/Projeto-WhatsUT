package whatsut.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import whatsut.cliente.ClienteImpl;
import whatsut.interfaces.IServer;
import whatsut.model.Usuario;
import whatsut.util.HashUtil;
import whatsut.util.UsuarioStorage;
import whatsut.views.MainView;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Map;

public class LoginController {

    @FXML
    private TextField userField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label erroLabel;

    @FXML
    protected void onLoginClick(ActionEvent event) {
        String nome = userField.getText().trim();
        String senha = passwordField.getText();

        if (autenticar(nome, senha)) {
            try {
                // Conectar ao servidor RMI
                Registry registry = LocateRegistry.getRegistry("localhost", 1099);
                IServer servidor = (IServer) registry.lookup("WhatsUTServer");

                // Criar a instância do cliente (callback)
                ClienteImpl cliente = new ClienteImpl();

                // Registrar o cliente no servidor
                boolean conectado = servidor.conectar(nome, cliente);
                if (!conectado) {
                    erroLabel.setText("Erro ao conectar com o servidor.");
                    return;
                }

                // Abrir tela principal
                Stage stage = (Stage) userField.getScene().getWindow();
                MainView.show(stage, nome);

            } catch (Exception e) {
                erroLabel.setText("Erro ao conectar com o servidor.");
                e.printStackTrace();
            }
        } else {
            erroLabel.setText("Usuário ou senha inválidos.");
        }
    }

    private boolean autenticar(String nome, String senhaDigitada) {
        Map<String, Usuario> usuarios = UsuarioStorage.carregarUsuarios();

        // Verifica se o usuário existe
        if (!usuarios.containsKey(nome)) {
            return false;
        }

        Usuario usuario = usuarios.get(nome);

        // Criptografa a senha digitada
        String senhaCriptografada = HashUtil.hash(senhaDigitada);

        // Compara com a senha armazenada
        return senhaCriptografada.equals(usuario.getSenhaCriptografada());
    }
}
