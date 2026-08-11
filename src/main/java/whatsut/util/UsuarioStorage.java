package whatsut.util;

import org.json.JSONArray;
import org.json.JSONObject;
import whatsut.model.Usuario;

import java.io.*;
import java.util.*;

public class UsuarioStorage {
    private static final String DIR = "data";
    private static final String USUARIOS_FILE = DIR + "/usuarios.json";
    private static final String BANIDOS_FILE = DIR + "/usuariosBanidos.json";

    static {
        File pasta = new File("data");
        if (!pasta.exists())
            pasta.mkdirs();
    }

    public static synchronized void salvarUsuarios(Map<String, Usuario> usuarios) {
        JSONArray array = new JSONArray();
        for (Usuario u : usuarios.values()) {
            JSONObject obj = new JSONObject();
            obj.put("nome", u.getNome());
            obj.put("senha", u.getSenhaCriptografada());
            array.put(obj);
        }

        try (FileWriter writer = new FileWriter(USUARIOS_FILE)) {
            writer.write(array.toString(4)); // identado
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized void salvarBanido(Usuario usuario) {
        JSONArray array = carregarBanidosArray();

        JSONObject obj = new JSONObject();
        obj.put("nome", usuario.getNome());
        obj.put("senha", usuario.getSenhaCriptografada());

        // Adiciona data/hora atual
        String dataHora = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        obj.put("dataBanimento", dataHora);

        array.put(obj);

        try (FileWriter writer = new FileWriter(BANIDOS_FILE)) {
            writer.write(array.toString(4)); // identado
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized Map<String, Usuario> carregarUsuarios() {
        Map<String, Usuario> usuarios = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(USUARIOS_FILE))) {
            StringBuilder sb = new StringBuilder();
            String linha;
            while ((linha = reader.readLine()) != null) {
                sb.append(linha);
            }
            JSONArray array = new JSONArray(sb.toString());
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String nome = obj.getString("nome");
                String senha = obj.getString("senha");
                usuarios.put(nome, new Usuario(nome, senha, null));
            }
        } catch (Exception e) {
            // Se o arquivo não existir ou estiver vazio, retorna mapa vazio
        }
        return usuarios;
    }

    public static synchronized Map<String, Usuario> carregarBanidos() {
        Map<String, Usuario> banidos = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader("data/usuariosBanidos.json"))) {
            StringBuilder sb = new StringBuilder();
            String linha;
            while ((linha = reader.readLine()) != null) {
                sb.append(linha);
            }
            org.json.JSONArray array = new org.json.JSONArray(sb.toString());
            for (int i = 0; i < array.length(); i++) {
                org.json.JSONObject obj = array.getJSONObject(i);
                String nome = obj.getString("nome");
                String senha = obj.getString("senha");
                banidos.put(nome, new Usuario(nome, senha, null));
            }
        } catch (Exception e) {
            // Arquivo pode estar vazio ou não existir ainda
        }
        return banidos;
    }

    private static JSONArray carregarBanidosArray() {
        try (BufferedReader reader = new BufferedReader(new FileReader(BANIDOS_FILE))) {
            StringBuilder sb = new StringBuilder();
            String linha;
            while ((linha = reader.readLine()) != null) {
                sb.append(linha);
            }
            return new JSONArray(sb.toString());
        } catch (Exception e) {
            return new JSONArray();
        }
    }
}