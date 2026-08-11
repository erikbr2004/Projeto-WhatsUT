package whatsut.interfaces;

import whatsut.model.Mensagem;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IClient extends Remote {
    void receberMensagem(Mensagem mensagem) throws RemoteException;
    void receberArquivo(String remetente, String nomeArquivo, byte[] dados) throws RemoteException;

}