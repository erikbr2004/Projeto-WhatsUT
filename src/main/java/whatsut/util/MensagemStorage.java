package whatsut.util;

import org.json.JSONArray;
import org.json.JSONObject;
import whatsut.model.Mensagem;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MensagemStorage {
    private static final String DIR_PRIVADAS = "data/mensagens";
    private static final String DIR_GRUPOS = "data/mensagensGrupos";

    public static void salvarMensagemPrivada(Mensagem msg) {
        if (msg.getDataHora() == null || msg.getDataHora().isBlank()) {
            String agora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy"));
            msg.setDataHora(agora);
        }

        String nome1 = msg.getRemetente();
        String nome2 = msg.getDestinatario();
        String nomeArquivo = nome1.compareTo(nome2) < 0
                ? "privado_" + nome1 + "_" + nome2 + ".json"
                : "privado_" + nome2 + "_" + nome1 + ".json";
        salvarMensagem(DIR_PRIVADAS, nomeArquivo, msg);
    }

    public static void salvarMensagemGrupo(Mensagem msg) {
        if (msg.getDataHora() == null || msg.getDataHora().isBlank()) {
            String agora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy"));
            msg.setDataHora(agora);
        }

        String nomeArquivo = "grupo_" + msg.getDestinatario() + ".json";
        salvarMensagem(DIR_GRUPOS, nomeArquivo, msg);
    }

    private static void salvarMensagem(String pasta, String arquivo, Mensagem msg) {
        try {
            File dir = new File(pasta);
            dir.mkdirs();
            File file = new File(dir, arquivo);

            JSONArray array;
            if (file.exists()) {
                String conteudo = Files.readString(file.toPath());
                array = new JSONArray(conteudo);
            } else {
                array = new JSONArray();
            }

            JSONObject obj = new JSONObject();
            obj.put("de", msg.getRemetente());
            if (pasta.equals(DIR_PRIVADAS)) {
                obj.put("para", msg.getDestinatario());
            } else {
                obj.put("grupo", msg.getDestinatario());
            }
            obj.put("conteudo", msg.getConteudo());
            obj.put("dataHora", msg.getDataHora());

            array.put(obj);
            Files.writeString(file.toPath(), array.toString(2));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<Mensagem> carregarMensagensPrivadas(String de, String para) {
        String nomeArquivo = de.compareTo(para) < 0
                ? "privado_" + de + "_" + para + ".json"
                : "privado_" + para + "_" + de + ".json";
        return carregarMensagens(DIR_PRIVADAS, nomeArquivo);
    }

    public static List<Mensagem> carregarMensagensGrupo(String grupo) {
        return carregarMensagens(DIR_GRUPOS, "grupo_" + grupo + ".json");
    }

    private static List<Mensagem> carregarMensagens(String pasta, String nomeArquivo) {
        List<Mensagem> mensagens = new ArrayList<>();
        File file = new File(pasta, nomeArquivo);
        if (!file.exists()) return mensagens;

        try {
            String conteudo = Files.readString(file.toPath());
            JSONArray array = new JSONArray(conteudo);
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);

                String de = o.optString("de", "desconhecido");
                String paraOuGrupo = o.has("para")
                        ? o.getString("para")
                        : o.optString("grupo", "desconhecido");
                String texto = o.optString("conteudo", "");
                String data = o.optString("dataHora", "");

                mensagens.add(new Mensagem(de, paraOuGrupo, texto, data));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return mensagens;
    }
}
