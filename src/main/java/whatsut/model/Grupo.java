package whatsut.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Grupo implements Serializable {
    private String nome;
    private String criador;
    private Set<String> membros;
    private Set<String> pendentes;
    private String modoAoSairDoGrupo;
    private String dataCriacao;

    public Grupo(String nome, String criador, Set<String> membros, String modoAoSairDoGrupo) {
        this.nome = nome;
        this.criador = criador;
        this.membros = new HashSet<>(membros);
        this.pendentes = new HashSet<>();
        this.modoAoSairDoGrupo = modoAoSairDoGrupo;
        this.dataCriacao = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }

    public String getNome() {
        return nome;
    }

    public String getCriador() {
        return criador;
    }

    public void setCriador(String novoAdmin) {
        this.criador = novoAdmin;
    }

    public Set<String> getMembros() {
        return membros;
    }

    public Set<String> getPendentes() {
        return pendentes;
    }

    public String getModoAoSairDoGrupo() {
        return modoAoSairDoGrupo;
    }

    public String getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(String data) {
        this.dataCriacao = data;
    }

    public void adicionarPedido(String nome) {
        pendentes.add(nome);
    }

    public void aprovarPedido(String nome) {
        pendentes.remove(nome);
        membros.add(nome);
    }

    public void recusarPedido(String nome) {
        pendentes.remove(nome);
    }

    public boolean isMembro(String nome) {
        return membros.contains(nome);
    }

    public void removerParticipante(String nome) {
        membros.remove(nome);
    }

    public boolean processarSaida(String nome) {
        membros.remove(nome);

        if (membros.isEmpty()) {
            return true; // Deletar grupo
        }

        if (nome.equals(criador)) {
            if (modoAoSairDoGrupo.equalsIgnoreCase("transferir")) {
                Optional<String> novoAdmin = membros.stream().findFirst();
                novoAdmin.ifPresent(this::setCriador);
            } else {
                return true; // Deletar se for modo "deletar"
            }
        }

        return false; // grupo continua
    }
}
