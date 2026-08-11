package whatsut.util;

import org.json.JSONArray;
import org.json.JSONObject;
import whatsut.model.Grupo;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class GrupoStorage {
    private static final String FILE = "data/grupos.json";

    public static synchronized void salvarGrupos(Map<String, Grupo> grupos) {
        JSONArray array = new JSONArray();
        for (Grupo g : grupos.values()) {
            JSONObject obj = new JSONObject();
            obj.put("nome", g.getNome());
            obj.put("criador", g.getCriador());
            obj.put("modoAoSair", g.getModoAoSairDoGrupo());
            obj.put("dataCriacao", g.getDataCriacao());
            obj.put("membros", new JSONArray(g.getMembros()));
            obj.put("pendentes", new JSONArray(g.getPendentes()));

            array.put(obj);
        }

        try {
            Files.createDirectories(new File("data").toPath());
            Files.writeString(new File(FILE).toPath(), array.toString(4));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized Map<String, Grupo> carregarGrupos() {
        Map<String, Grupo> grupos = new HashMap<>();
        File file = new File(FILE);
        if (!file.exists()) return grupos;

        try {
            JSONArray array = new JSONArray(Files.readString(file.toPath()));
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);

                String nome = obj.getString("nome");
                String criador = obj.getString("criador");
                String modo = obj.getString("modoAoSair");

                Set<String> membros = jsonArrayToSet(obj.getJSONArray("membros"));
                Set<String> pendentes = jsonArrayToSet(obj.getJSONArray("pendentes"));

                Grupo g = new Grupo(nome, criador, membros, modo);
                g.setDataCriacao(obj.optString("dataCriacao", "desconhecida"));
                for (String p : pendentes) {
                    g.adicionarPedido(p);
                }

                grupos.put(nome, g);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return grupos;
    }

    private static Set<String> jsonArrayToSet(JSONArray array) {
        Set<String> set = new HashSet<>();
        for (int i = 0; i < array.length(); i++) {
            set.add(array.getString(i));
        }
        return set;
    }
}