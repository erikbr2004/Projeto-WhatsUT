package whatsut.model;

import whatsut.interfaces.IClient;

public class Usuario {
    private String nome;
    private String senhaCriptografada;
    private IClient callback; // pode ser null
    public boolean isOnline = false;

    public Usuario(String nome, String senhaCriptografada, IClient callback) {
        this.nome = nome;
        this.senhaCriptografada = senhaCriptografada;
        this.callback = callback;
    }

    public String getNome() {
        return nome;
    }

    public String getSenhaCriptografada() {
        return senhaCriptografada;
    }

    public IClient getCallback() {
        return callback;
    }

    public void setCallback(IClient callback) {
        this.callback = callback;
    }
}