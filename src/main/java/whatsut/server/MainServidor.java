package whatsut.server;

import whatsut.interfaces.IServer;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class MainServidor {
    public static void main(String[] args) {
        try {
            // Cria e expõe o registry na porta 1099
            Registry registry = LocateRegistry.createRegistry(1099);

            // Cria a instância do servidor
            IServer servidor = new ServidorImpl();

            // Registra o servidor com o nome "WhatsUTServer"
            registry.rebind("WhatsUTServer", servidor);

            System.out.println("Servidor WhatsUT ativo e registrado como 'WhatsUTServer'");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
