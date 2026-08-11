package whatsut.cliente;

import whatsut.interfaces.IClient;
import whatsut.model.Mensagem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ClienteImpl extends UnicastRemoteObject implements IClient {

    public ClienteImpl() throws RemoteException {
        super();
    }

    @Override
    public void receberMensagem(Mensagem mensagem) throws RemoteException {
        System.out.println("\n[Mensagem de " + mensagem.getRemetente() + "] " + mensagem.getConteudo());
    }

    @Override
    public void receberArquivo(String remetente, String nomeArquivo, byte[] dados) throws RemoteException {
        try {
            File dir = new File("transferencias");
            if (!dir.exists()) dir.mkdir();

            FileOutputStream fos = new FileOutputStream("transferencias/" + nomeArquivo);
            fos.write(dados);
            fos.close();

            System.out.println("Arquivo '" + nomeArquivo + "' recebido de " + remetente + " e salvo na pasta 'transferencias'.");
        } catch (IOException e) {
            System.err.println("Erro ao salvar arquivo: " + e.getMessage());
        }
    }
}