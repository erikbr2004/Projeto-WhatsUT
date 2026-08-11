package whatsut.server;

import whatsut.interfaces.IClient;
import whatsut.interfaces.IServer;
import whatsut.model.Grupo;
import whatsut.model.Mensagem;
import whatsut.model.Usuario;
import whatsut.util.GrupoStorage;
import whatsut.util.HashUtil;
import whatsut.util.MensagemStorage;
import whatsut.util.UsuarioStorage;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ServidorImpl extends UnicastRemoteObject implements IServer {

    private final Map<String, Usuario> usuariosRegistrados;
    private final Map<String, IClient> usuariosOnline;
    private final Map<String, Grupo> grupos;
    private final Map<String, Set<String>> pedidosBanimento = new HashMap<>();

    public ServidorImpl() throws RemoteException {
        super();

        this.usuariosRegistrados = UsuarioStorage.carregarUsuarios();
        Usuario admin = new Usuario("admin", HashUtil.hash("admin123"), null);
        usuariosRegistrados.putIfAbsent("admin", admin);
        UsuarioStorage.salvarUsuarios(usuariosRegistrados);

        this.usuariosOnline = new HashMap<>();
        this.grupos = GrupoStorage.carregarGrupos();
    }

    @Override
    public synchronized boolean registrarUsuario(String nome, String senhaCriptografada) throws RemoteException {
        // Verifica se está banido
        Map<String, Usuario> banidos = UsuarioStorage.carregarBanidos();
        if (banidos.containsKey(nome)) {
            System.out.println("Tentativa de registro com nome banido: " + nome);
            return false;
        }

        if (usuariosRegistrados.containsKey(nome))
            return false;

        Usuario novo = new Usuario(nome, senhaCriptografada, null);
        usuariosRegistrados.put(nome, novo);
        UsuarioStorage.salvarUsuarios(usuariosRegistrados);

        System.out.println("Usuário registrado: " + nome);
        return true;
    }

    @Override
    public synchronized boolean autenticarUsuario(String nome, String senhaCriptografada) throws RemoteException {
        Usuario user = usuariosRegistrados.get(nome);
        if (user == null) return false;
        return user.getSenhaCriptografada().equals(senhaCriptografada);
    }

    @Override
    public synchronized boolean conectar(String nome, IClient client) throws RemoteException {
        Usuario user = usuariosRegistrados.get(nome);
        if (user == null) return false;

        user.setCallback(client);
        user.isOnline = true;
        usuariosOnline.put(nome, client);

        System.out.println("[SERVIDOR] " + nome + " está online.");
        return true;
    }

    @Override
    public synchronized void desconectar(String nome) throws RemoteException {
        Usuario user = usuariosRegistrados.get(nome);
        if (user != null) {
            user.setCallback(null);
            user.isOnline = false;
        }
        usuariosOnline.remove(nome);
        System.out.println("[SERVIDOR] " + nome + " foi desconectado.");
    }

    @Override
    public synchronized List<String> listarUsuariosOnline() throws RemoteException {
        return new ArrayList<>(usuariosOnline.keySet());
    }

    @Override
    public synchronized List<String> listarUsuariosRegistrados() throws RemoteException {
        return new ArrayList<>(usuariosRegistrados.keySet());
    }

    @Override
    public synchronized boolean criarGrupo(String nomeGrupo, String criador, Set<String> membrosSelecionados, String modoAoSairDoGrupo) throws RemoteException {
        if (grupos.containsKey(nomeGrupo)) return false;

        // Valida que todos os membros existem (mesmo que estejam offline)
        for (String membro : membrosSelecionados) {
            if (!usuariosRegistrados.containsKey(membro)) {
                return false;
            }
        }

        Grupo grupo = new Grupo(nomeGrupo, criador, membrosSelecionados, modoAoSairDoGrupo);
        grupos.put(nomeGrupo, grupo);
        GrupoStorage.salvarGrupos(grupos);
        return true;
    }

    @Override
    public synchronized List<String> listarTodosGrupos() throws RemoteException {
        return new ArrayList<>(grupos.keySet());
    }

    @Override
    public synchronized List<String> listarGruposDoUsuario(String nomeUsuario) throws RemoteException {
        List<String> lista = new ArrayList<>();
        for (Grupo grupo : grupos.values()) {
            if (grupo.isMembro(nomeUsuario)) {
                lista.add(grupo.getNome());
            }
        }
        return lista;
    }

    @Override
    public synchronized boolean pedirEntradaGrupo(String nomeGrupo, String nomeUsuario) throws RemoteException {
        Grupo grupo = grupos.get(nomeGrupo);
        if (grupo == null || grupo.isMembro(nomeUsuario)) return false;

        grupo.adicionarPedido(nomeUsuario);
        GrupoStorage.salvarGrupos(grupos);
        return true;
    }

    @Override
    public synchronized Set<String> listarPedidosPendentes(String nomeGrupo, String solicitante) throws RemoteException {
        Grupo grupo = grupos.get(nomeGrupo);
        if (grupo == null || !grupo.getCriador().equals(solicitante)) return Collections.emptySet();
        return new HashSet<>(grupo.getPendentes());
    }

    @Override
    public synchronized boolean aprovarEntrada(String nomeGrupo, String nomeAdmin, String nomeSolicitante) throws RemoteException {
        Grupo grupo = grupos.get(nomeGrupo);
        if (grupo == null || !grupo.getCriador().equals(nomeAdmin)) return false;

        grupo.aprovarPedido(nomeSolicitante);

        // Envia mensagem para todos os membros
        for (String membro : grupo.getMembros()) {
            IClient cli = usuariosOnline.get(membro);
            if (cli != null) {
                Mensagem msg = new Mensagem("Sistema", nomeGrupo, nomeSolicitante + " entrou no grupo.");
                cli.receberMensagem(msg);
            }
        }
        GrupoStorage.salvarGrupos(grupos);

        return true;
    }

    @Override
    public synchronized boolean recusarEntrada(String nomeGrupo, String nomeAdmin, String nomeSolicitante) throws RemoteException {
        Grupo grupo = grupos.get(nomeGrupo);
        if (grupo == null || !grupo.getCriador().equals(nomeAdmin)) return false;

        grupo.recusarPedido(nomeSolicitante);
        GrupoStorage.salvarGrupos(grupos);
        return true;
    }

    @Override
    public synchronized void enviarMensagemPrivada(String de, String para, String conteudo) throws RemoteException {
        IClient destinatario = usuariosOnline.get(para);
        if (destinatario != null) {
            String agora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy"));
            Mensagem msg = new Mensagem(de, para, conteudo, agora);
            MensagemStorage.salvarMensagemPrivada(msg);
            destinatario.receberMensagem(msg);
        } else {
            throw new RemoteException("Usuário " + para + " está offline ou não existe.");
        }
    }

    @Override
    public synchronized void enviarMensagemGrupo(String de, String grupo, String conteudo) throws RemoteException {
        Map<String, Grupo> gruposAtualizados = GrupoStorage.carregarGrupos();
        Grupo g = gruposAtualizados.get(grupo);

        if (g == null)
            throw new RemoteException("Grupo não encontrado.");
        if (!g.getMembros().contains(de))
            throw new RemoteException("Você não é membro do grupo.");

        String agora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy"));
        Mensagem msg = new Mensagem(de, grupo, conteudo, agora);
        MensagemStorage.salvarMensagemGrupo(msg);

        for (String membro : g.getMembros()) {
            if (!membro.equals(de)) {
                IClient cli = usuariosOnline.get(membro);
                if (cli != null) {
                    cli.receberMensagem(msg);
                }
            }
        }
    }

    @Override
    public synchronized void enviarArquivo(String remetente, String destinatario, String nomeArquivo, byte[] dados) throws RemoteException {
        IClient receiver = usuariosOnline.get(destinatario);
        if (receiver != null) {
            receiver.receberArquivo(remetente, nomeArquivo, dados);

            // Salva também como mensagem no histórico
            String agora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy"));
            String conteudo = "[Arquivo enviado: " + nomeArquivo + "]";
            MensagemStorage.salvarMensagemPrivada(new Mensagem(remetente, destinatario, conteudo, agora));
        } else {
            throw new RemoteException("Usuário " + destinatario + " está offline.");
        }
    }

    @Override
    public synchronized void enviarArquivoGrupo(String remetente, String nomeGrupo, String nomeArquivo, byte[] dados) throws RemoteException {
        Map<String, Grupo> gruposAtualizados = GrupoStorage.carregarGrupos();
        Grupo g = gruposAtualizados.get(nomeGrupo);

        if (g == null)
            throw new RemoteException("Grupo não encontrado.");
        if (!g.getMembros().contains(remetente))
            throw new RemoteException("Você não faz parte do grupo.");

        String agora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy"));
        String conteudo = "[Arquivo enviado: " + nomeArquivo + "]";
        Mensagem msg = new Mensagem(remetente, nomeGrupo, conteudo, agora);

        // Salva no histórico do grupo
        MensagemStorage.salvarMensagemGrupo(msg);

        for (String membro : g.getMembros()) {
            if (!membro.equals(remetente)) {
                IClient cli = usuariosOnline.get(membro);
                if (cli != null) {
                    cli.receberMensagem(msg);
                    cli.receberArquivo(remetente, nomeArquivo, dados);
                }
            }
        }
    }

    @Override
    public synchronized boolean banirUsuario(String solicitante, String alvo) throws RemoteException {
        if (!solicitante.equals("admin"))
            return false;
        Usuario user = usuariosRegistrados.remove(alvo);
        if (user != null) {
            usuariosOnline.remove(alvo);

            for (Grupo g : grupos.values()) {
                g.removerParticipante(alvo);
                g.getPendentes().remove(alvo);
            }

            UsuarioStorage.salvarUsuarios(usuariosRegistrados);
            UsuarioStorage.salvarBanido(user);

            System.out.println(alvo + ": foi banido da aplicação.");
            return true;
        }
        return false;
    }

    @Override
    public synchronized boolean banirUsuarioDoGrupo(String grupo, String solicitante, String alvo) throws RemoteException {
        Map<String, Grupo> gruposAtualizados = GrupoStorage.carregarGrupos();
        Grupo g = gruposAtualizados.get(grupo);

        if (g != null && g.getCriador().equals(solicitante)) {
            g.removerParticipante(alvo);
            g.getPendentes().remove(alvo);
            for (String membro : g.getMembros()) {
                IClient cli = usuariosOnline.get(membro);
                if (cli != null) {
                    Mensagem msg = new Mensagem("Sistema", grupo, alvo + " foi removido do grupo por " + solicitante + ".");
                    cli.receberMensagem(msg);
                }
            }
            GrupoStorage.salvarGrupos(gruposAtualizados);
            System.out.println(alvo + " foi removido do grupo " + grupo);
            return true;
        }
        return false;
    }

    @Override
    public synchronized boolean sairDoGrupo(String grupo, String nome) throws RemoteException {
        Map<String, Grupo> gruposAtualizados = GrupoStorage.carregarGrupos();
        Grupo g = gruposAtualizados.get(grupo);
        if (g == null) return false;

        boolean deletar = g.processarSaida(nome);

        if (deletar) {
            gruposAtualizados.remove(grupo);
            GrupoStorage.salvarGrupos(gruposAtualizados);

            for (String membro : g.getMembros()) {
                IClient cli = usuariosOnline.get(membro);
                if (cli != null) {
                    Mensagem m = new Mensagem("Sistema", grupo, "O grupo foi deletado porque todos saíram.");
                    cli.receberMensagem(m);
                }
            }

            System.out.println("Grupo " + grupo + " foi deletado.");
            return true;
        }

        GrupoStorage.salvarGrupos(gruposAtualizados);

        String aviso;
        if (nome.equals(g.getCriador())) {
            aviso = nome + " saiu do grupo. Novo administrador: " + g.getCriador();
        } else {
            aviso = nome + " saiu do grupo.";
        }

        for (String membro : g.getMembros()) {
            IClient cli = usuariosOnline.get(membro);
            if (cli != null) {
                cli.receberMensagem(new Mensagem("Sistema", grupo, aviso));
            }
        }

        System.out.println(nome + " saiu do grupo " + grupo + ".");
        return true;
    }


    @Override
    public synchronized int getQuantidadeMembrosDoGrupo(String nomeGrupo) throws RemoteException {
        Grupo g = grupos.get(nomeGrupo);
        return (g != null) ? g.getMembros().size() : 0;
    }

    @Override
    public synchronized boolean solicitarBanimento(String solicitante, String alvo) throws RemoteException {
        if (!usuariosRegistrados.containsKey(alvo))
            return false;
        if (solicitante.equals(alvo))
            return false;

        // Não pode banir o próprio admin
        if (alvo.equals("admin"))
            return false;

        // Se quem solicitou for o admin, bane direto
        if (solicitante.equals("admin"))
            return banirUsuario("admin", alvo);

        // Caso comum: adiciona à fila de pedidos
        pedidosBanimento.putIfAbsent(alvo, new HashSet<>());
        pedidosBanimento.get(alvo).add(solicitante);
        return true;
    }

    @Override
    public synchronized Map<String, Set<String>> listarPedidosBanimento(String admin) throws RemoteException {
        if (!admin.equals("admin")) return Collections.emptyMap();
        return new HashMap<>(pedidosBanimento);
    }

    @Override
    public synchronized boolean processarBanimento(String admin, String alvo, boolean aprovar) throws RemoteException {
        if (!admin.equals("admin") || !pedidosBanimento.containsKey(alvo)) return false;

        if (aprovar) {
            banirUsuario(admin, alvo);
        }

        pedidosBanimento.remove(alvo);
        return true;
    }
}