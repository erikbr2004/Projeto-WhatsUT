package whatsut.cliente;

import whatsut.interfaces.IServer;

public class SessaoCliente {
    private String nome;
    private IServer servidor;

    public SessaoCliente(String nome, IServer servidor) {
        this.nome = nome;
        this.servidor = servidor;
    }

    public String getNome() {
        return nome;
    }

    public IServer getServidor() {
        return servidor;
    }
}