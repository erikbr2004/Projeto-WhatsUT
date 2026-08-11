package whatsut.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IServer extends Remote {

    boolean registrarUsuario(String nome, String senhaCriptografada) throws RemoteException;
    boolean autenticarUsuario(String nome, String senhaCriptografada) throws RemoteException;
    boolean conectar(String nome, IClient client) throws RemoteException;
    void desconectar(String nome) throws RemoteException;
    List<String> listarUsuariosOnline() throws RemoteException;

    boolean criarGrupo(String nomeGrupo, String criador, Set<String> membrosSelecionados, String modoAoSairDoGrupo) throws RemoteException;
    List<String> listarGruposDoUsuario(String nomeUsuario) throws RemoteException;
    List<String> listarUsuariosRegistrados() throws RemoteException;
    boolean pedirEntradaGrupo(String nomeGrupo, String nomeUsuario) throws RemoteException;
    Set<String> listarPedidosPendentes(String nomeGrupo, String solicitante) throws RemoteException;
    boolean aprovarEntrada(String nomeGrupo, String nomeAdmin, String nomeSolicitante) throws RemoteException;
    boolean recusarEntrada(String nomeGrupo, String nomeAdmin, String nomeSolicitante) throws RemoteException;
    List<String> listarTodosGrupos() throws RemoteException;

    void enviarMensagemPrivada(String de, String para, String conteudo) throws RemoteException;
    void enviarMensagemGrupo(String de, String grupo, String conteudo) throws RemoteException;

    void enviarArquivo(String remetente, String destinatario, String nomeArquivo, byte[] dados) throws RemoteException;
    void enviarArquivoGrupo(String remetente, String nomeGrupo, String nomeArquivo, byte[] dados) throws RemoteException;


    boolean banirUsuario(String solicitante, String alvo) throws RemoteException;
    boolean banirUsuarioDoGrupo(String grupo, String solicitante, String alvo) throws RemoteException;

    boolean sairDoGrupo(String grupo, String nome) throws RemoteException;
    int getQuantidadeMembrosDoGrupo(String nomeGrupo) throws RemoteException;

    boolean solicitarBanimento(String solicitante, String alvo) throws RemoteException;
    Map<String, Set<String>> listarPedidosBanimento(String admin) throws RemoteException;
    boolean processarBanimento(String admin, String alvo, boolean aprovar) throws RemoteException;
}