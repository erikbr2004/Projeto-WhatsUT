package whatsut.model;

import java.io.Serializable;

public class Mensagem implements Serializable {
    private static final long serialVersionUID = 1L;

    private String remetente;
    private String destinatario;
    private String conteudo;
    private String dataHora;

    public Mensagem(String remetente, String destinatario, String conteudo) {
        this(remetente, destinatario, conteudo, null);
    }

    public Mensagem(String remetente, String destinatario, String conteudo, String dataHora) {
        this.remetente = remetente;
        this.destinatario = destinatario;
        this.conteudo = conteudo;
        this.dataHora = dataHora;
    }

    public String getRemetente() {
        return remetente;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public String getConteudo() {
        return conteudo;
    }

    public String getDataHora() {
        return dataHora;
    }

    public void setDataHora(String dataHora) {
        this.dataHora = dataHora;
    }
}
