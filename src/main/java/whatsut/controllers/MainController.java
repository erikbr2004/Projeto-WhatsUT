package whatsut.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import whatsut.interfaces.IServer;
import whatsut.model.Grupo;
import whatsut.model.Mensagem;
import whatsut.model.Usuario;
import whatsut.util.GrupoStorage;
import whatsut.util.MensagemStorage;
import whatsut.util.UsuarioStorage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MainController {

    @FXML private TextField searchField;
    @FXML private ListView<String> contactList;
    @FXML private VBox chatArea;
    @FXML private TextField inputMensagem;
    @FXML private TextArea chatHistory;
    @FXML private Label chatTitle;
    @FXML private Button botaoArquivo;
    @FXML private Button botaoEnviar;
    @FXML private Button botaoIntegrantes;
    @FXML private Button botaoBanir;
    @FXML private Button botaoSairGrupo;


    private String usuarioLogado;
    private String contatoAtual;
    private boolean contatoAtualEhGrupo;
    private Timeline atualizadorTimeline;
    private Timeline atualizadorContatos;
    private List<String> contatosOriginais = new ArrayList<>();

    private File arquivoSelecionado = null;
    private IServer servidor;

    public void setUsuarioLogado(String nome) {
        this.usuarioLogado = nome;
        carregarListaDeContatos();
        iniciarAtualizacaoContatos();

        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            servidor = (IServer) registry.lookup("WhatsUTServer");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void carregarListaDeContatos() {
        List<String> novaLista = new ArrayList<>();
        Map<String, Usuario> usuarios = UsuarioStorage.carregarUsuarios();
        for (String nome : usuarios.keySet()) {
            if (!nome.equals(usuarioLogado)) {
                novaLista.add(nome);
            }
        }
        Map<String, Grupo> grupos = GrupoStorage.carregarGrupos();
        for (Grupo g : grupos.values()) {
            if (g.getMembros().contains(usuarioLogado)) {
                novaLista.add("Grupo: " + g.getNome());
            }
        }
        contatosOriginais = novaLista;
        filtrarContatos(searchField.getText());
    }

    private void filtrarContatos(String filtro) {
        if (filtro == null || filtro.isBlank()) {
            contactList.getItems().setAll(contatosOriginais);
        } else {
            String filtroLower = filtro.toLowerCase();
            List<String> filtrados = contatosOriginais.stream()
                    .filter(c -> c.toLowerCase().contains(filtroLower))
                    .toList();
            contactList.getItems().setAll(filtrados);
        }
    }

    @FXML
    public void initialize() {
        contactList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                if (newVal.startsWith("Grupo: ")) {
                    abrirConversa(newVal.substring(7), true);
                } else {
                    abrirConversa(newVal, false);
                }
            }
        });
        searchField.textProperty().addListener((obs, oldText, newText) -> filtrarContatos(newText));

        contactList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String nome, boolean vazio) {
                super.updateItem(nome, vazio);
                if (vazio || nome == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(nome);

                    if (!nome.startsWith("Grupo: ")) {
                        boolean online = false;
                        try {
                            List<String> onlineList = servidor.listarUsuariosOnline();
                            online = onlineList.contains(nome);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        setStyle("-fx-text-fill: " + (online ? "green;" : "black;"));
                    } else {
                        setStyle(""); // mantém cor padrão para grupos
                    }
                }
            }
        });
    }

    private void abrirConversa(String nome, boolean isGrupo) {
        if (atualizadorTimeline != null) {
            atualizadorTimeline.stop();
        }

        contatoAtual = nome;
        contatoAtualEhGrupo = isGrupo;

        chatTitle.setText(isGrupo
                ? "Conversando com o grupo " + nome
                : "Conversando com " + nome);

        chatHistory.clear();
        atualizarHistorico(chatHistory, contatoAtual, contatoAtualEhGrupo);

        inputMensagem.clear();

        iniciarAtualizacaoAutomatica();

        botaoIntegrantes.setVisible(isGrupo);
        botaoSairGrupo.setVisible(isGrupo);

        if (isGrupo) {
            Map<String, Grupo> grupos = GrupoStorage.carregarGrupos();
            Grupo grupo = grupos.get(nome);

            if (grupo != null && !grupo.isMembro(usuarioLogado)) {
                Alert alerta = new Alert(Alert.AlertType.WARNING);
                alerta.setTitle("Acesso negado");
                alerta.setHeaderText(null);
                alerta.setContentText("Você não é mais membro deste grupo.");
                alerta.showAndWait();
                return;
            }

            botaoBanir.setVisible(grupo != null && grupo.getCriador().equals(usuarioLogado));
        } else {
            botaoBanir.setVisible(false);
        }

    }

    @FXML
    private void handleMostrarIntegrantes() {
        if (!contatoAtualEhGrupo || contatoAtual == null) {
            Alert alerta = new Alert(Alert.AlertType.INFORMATION);
            alerta.setTitle("Informação");
            alerta.setHeaderText(null);
            alerta.setContentText("Selecione um grupo para visualizar os integrantes.");
            alerta.show();
            return;
        }

        Map<String, Grupo> grupos = GrupoStorage.carregarGrupos();
        Grupo grupo = grupos.get(contatoAtual);

        if (grupo != null) {
            String criador = grupo.getCriador();
            StringBuilder membrosTexto = new StringBuilder("Integrantes do grupo " + grupo.getNome() + ":\n\n");

            for (String membro : grupo.getMembros()) {
                if (membro.equals(criador)) {
                    membrosTexto.append("- ").append(membro).append(" (admin)").append("\n");
                } else {
                    membrosTexto.append("- ").append(membro).append("\n");
                }
            }

            Alert alerta = new Alert(Alert.AlertType.INFORMATION);
            alerta.setTitle("Integrantes do grupo");
            alerta.setHeaderText(null);
            alerta.setContentText(membrosTexto.toString());
            alerta.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            alerta.show();
        } else {
            Alert alerta = new Alert(Alert.AlertType.ERROR);
            alerta.setTitle("Erro");
            alerta.setHeaderText(null);
            alerta.setContentText("Grupo não encontrado.");
            alerta.show();
        }
    }

    @FXML
    private void handleBanirIntegrante() {
        if (!contatoAtualEhGrupo || contatoAtual == null) return;

        try {
            Map<String, Grupo> grupos = GrupoStorage.carregarGrupos();
            Grupo grupo = grupos.get(contatoAtual);

            if (grupo == null || !grupo.getCriador().equals(usuarioLogado)) {
                Alert alerta = new Alert(Alert.AlertType.ERROR);
                alerta.setTitle("Erro");
                alerta.setHeaderText(null);
                alerta.setContentText("Apenas o administrador pode banir integrantes.");
                alerta.show();
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/whatsut/banir-integrantes-view.fxml"));
            Parent root = loader.load();

            BanirIntegrantesController controller = loader.getController();
            controller.init(grupo, servidor, usuarioLogado);
            controller.setMainController(this);

            Stage stage = new Stage();
            stage.setTitle("Banir integrantes do grupo");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void handleSairDoGrupo() {
        if (!contatoAtualEhGrupo || contatoAtual == null) {
            Alert alerta = new Alert(Alert.AlertType.INFORMATION);
            alerta.setTitle("Informação");
            alerta.setHeaderText(null);
            alerta.setContentText("Selecione um grupo para sair.");
            alerta.show();
            return;
        }

        Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacao.setTitle("Confirmar saída");
        confirmacao.setHeaderText(null);
        confirmacao.setContentText("Tem certeza que deseja sair do grupo \"" + contatoAtual + "\"?");

        Optional<ButtonType> resultado = confirmacao.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            try {
                boolean sucesso = servidor.sairDoGrupo(contatoAtual, usuarioLogado);
                if (sucesso) {
                    Alert ok = new Alert(Alert.AlertType.INFORMATION);
                    ok.setTitle("Sucesso");
                    ok.setHeaderText(null);
                    ok.setContentText("Você saiu do grupo \"" + contatoAtual + "\".");
                    ok.show();

                    contatoAtual = null;
                    contatoAtualEhGrupo = false;
                    chatTitle.setText("");
                    chatHistory.clear();
                    inputMensagem.clear();
                    botaoIntegrantes.setVisible(false);
                    botaoSairGrupo.setVisible(false);

                    carregarListaDeContatos();
                } else {
                    Alert erro = new Alert(Alert.AlertType.ERROR);
                    erro.setTitle("Erro");
                    erro.setHeaderText(null);
                    erro.setContentText("Não foi possível sair do grupo.");
                    erro.show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleEscolherArquivo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Escolher Arquivo para Enviar");

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Todos os Arquivos", "*.*"),
                new FileChooser.ExtensionFilter("Documentos", "*.pdf", "*.docx", "*.txt"),
                new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg")
        );

        arquivoSelecionado = fileChooser.showOpenDialog(null);

        if (arquivoSelecionado != null) {
            Alert alerta = new Alert(Alert.AlertType.INFORMATION);
            alerta.setHeaderText("Arquivo selecionado");
            alerta.setContentText("Arquivo: " + arquivoSelecionado.getName());
            alerta.show();

            inputMensagem.setText("[Arquivo selecionado] " + arquivoSelecionado.getName());
        }
    }

    @FXML
    private void handleEnviarMensagem() {
        try {
            String texto = inputMensagem.getText().trim();
            String dataHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy"));

            if (arquivoSelecionado != null) {
                byte[] dados = Files.readAllBytes(arquivoSelecionado.toPath());
                String nomeArquivo = arquivoSelecionado.getName();
                String conteudo = "[Arquivo enviado: " + nomeArquivo + "]";

                if (contatoAtualEhGrupo) {
                    servidor.enviarArquivoGrupo(usuarioLogado, contatoAtual, nomeArquivo, dados);
                } else {
                    servidor.enviarArquivo(usuarioLogado, contatoAtual, nomeArquivo, dados);
                }


                inputMensagem.clear();
                arquivoSelecionado = null;
                return;
            }

            if (!texto.isEmpty()) {
                if (contatoAtualEhGrupo) {
                    servidor.enviarMensagemGrupo(usuarioLogado, contatoAtual, texto);
                } else {
                    servidor.enviarMensagemPrivada(usuarioLogado, contatoAtual, texto);
                }
                inputMensagem.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void atualizarHistorico(TextArea historico, String destino, boolean isGrupo) {
        double scrollY = historico.getScrollTop(); // salva posição atual do scroll

        historico.clear();
        historico.appendText("***Histórico de conversa***\n\n");

        List<Mensagem> mensagens = isGrupo
                ? MensagemStorage.carregarMensagensGrupo(destino)
                : MensagemStorage.carregarMensagensPrivadas(usuarioLogado, destino);

        for (Mensagem m : mensagens) {
            String remetente = m.getRemetente().equals(usuarioLogado) ? "Você" : m.getRemetente();
            String data = m.getDataHora() != null ? m.getDataHora() : "";
            historico.appendText(remetente + " [" + data + "]: " + m.getConteudo() + "\n");
        }

        historico.setScrollTop(scrollY); // restaura a posição anterior
    }


    private void iniciarAtualizacaoAutomatica() {
        atualizadorTimeline = new Timeline(
                new KeyFrame(Duration.seconds(3), e -> {
                    if (chatHistory != null && contatoAtual != null) {
                        atualizarHistorico(chatHistory, contatoAtual, contatoAtualEhGrupo);
                    }
                })
        );
        atualizadorTimeline.setCycleCount(Timeline.INDEFINITE);
        atualizadorTimeline.play();
    }

    private void iniciarAtualizacaoContatos() {
        atualizadorContatos = new Timeline(
                new KeyFrame(Duration.seconds(5), e -> carregarListaDeContatos())
        );
        atualizadorContatos.setCycleCount(Timeline.INDEFINITE);
        atualizadorContatos.play();
    }

    @FXML
    private void handleCreateGroupClick() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/whatsut/criar-grupo-view.fxml"));
            Parent root = loader.load();

            // Obtém o controller da nova janela
            CriarGrupoController controller = loader.getController();
            controller.setUsuarioLogado(usuarioLogado);
            controller.setMainController(this);

            // Carrega todos os usuários, menos o logado
            Map<String, whatsut.model.Usuario> todos = UsuarioStorage.carregarUsuarios();
            List<String> nomes = new ArrayList<>();
            for (String nome : todos.keySet()) {
                if (!nome.equals(usuarioLogado)) {
                    nomes.add(nome);
                }
            }
            controller.setAllUsers(nomes);

            Stage stage = new Stage();
            stage.setTitle("Criar Grupo");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addContact(String contactName) {
        contatosOriginais.add(contactName);
        filtrarContatos(searchField.getText());
    }
}
