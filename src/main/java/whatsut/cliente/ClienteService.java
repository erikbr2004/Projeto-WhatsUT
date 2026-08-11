package whatsut.cliente;

import whatsut.interfaces.IClient;
import whatsut.interfaces.IServer;
import whatsut.util.HashUtil;

import java.io.File;
import java.nio.file.Files;
import java.rmi.Naming;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class ClienteService {
    private SessaoCliente sessao;
    private Scanner sc = new Scanner(System.in);

    public void iniciar() {
        try {
            System.out.print("Digite seu nome de usuário: ");
            String nome = sc.nextLine();

            System.out.print("Digite sua senha: ");
            String senha = sc.nextLine();

            String senhaHash = HashUtil.hash(senha);

            IServer servidor = (IServer) Naming.lookup("rmi://localhost:1099/WhatsUTServer");

            boolean autenticado = servidor.autenticarUsuario(nome, senhaHash);

            if (!autenticado) {
                System.out.println("Usuário não encontrado. Deseja se registrar? (s/n)");
                String opcao = sc.nextLine();
                if (opcao.equalsIgnoreCase("s")) {
                    boolean registrado = servidor.registrarUsuario(nome, senhaHash);
                    if (registrado) {
                        System.out.println("Registrado com sucesso!");
                    } else {
                        System.out.println("Erro ao registrar. Nome já existe.");
                        return;
                    }
                } else {
                    System.out.println("Encerrando...");
                    return;
                }
            } else {
                System.out.println("Login realizado com sucesso!");
            }

            ClienteImpl callback = new ClienteImpl();
            servidor.conectar(nome, callback);

            sessao = new SessaoCliente(nome, servidor);

            menu();

            servidor.desconectar(nome);
            UnicastRemoteObject.unexportObject(callback, true);
            System.out.println("Desconectado com sucesso.");

        } catch (Exception e) {
            System.err.println("Erro ao conectar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void menu() {
        while (true) {
            System.out.println("\nMenu:");
            System.out.println("1  - Mostrar usuarios online");
            System.out.println("2  - Criar grupo");
            System.out.println("3  - Listar grupos e pedir para entrar");
            System.out.println("4  - Ver meus grupos");
            System.out.println("5  - Ver pedidos de entrada em grupos que administro");
            System.out.println("6  - Enviar mensagem privada");
            System.out.println("7  - Enviar mensagem para grupo");
            System.out.println("8  - Enviar arquivo privado");
            System.out.println("9 - Banir usuário de grupo (admin do grupo)");
            System.out.println("10 - Sair de um grupo");
            System.out.println("11 - Solicitar banimento de um usuário");
            System.out.println("12 - (ADMIN) Ver e processar solicitações de banimento");
            System.out.println("0  - Sair");

            String opcao = sc.nextLine();

            switch (opcao) {
                case "1":
                    mostrarUsuariosOnline();
                    break;
                case "2":
                    criarGrupo();
                    break;
                case "3":
                    listarGruposEPedirEntrada();
                    break;
                case "4":
                    listarGruposDoUsuario();
                    break;
                case "5":
                    processarPedidosPendentes();
                    break;
                case "6":
                    enviarMensagemPrivada();
                    break;
                case "7":
                    listarGruposDoUsuario();
                    enviarMensagemGrupo();
                    break;
                case "8":
                    enviarArquivoPrivado();
                    break;
                case "9":
                    banirUsuarioDoGrupo();
                    break;
                case "10":
                    sairDeGrupo();
                    break;
                case "11":
                    solicitarBanimento();
                    break;
                case "12":
                    processarSolicitacoesBanimento();
                    break;
                case "0":
                    return;
                default:
                    System.out.println("Opção inválida.");
            }
        }
    }

    private void criarGrupo() {
        try {
            System.out.print("Nome do grupo: ");
            String nomeGrupo = sc.nextLine();

            List<String> registrados = sessao.getServidor().listarUsuariosRegistrados();
            Set<String> membros = new HashSet<>();
            String modoAoSairDoGrupo;

            do {
                System.out.print("Modo caso criador saia do grupo (deletar/transferir): ");
                modoAoSairDoGrupo = sc.nextLine();
            } while (!modoAoSairDoGrupo.equalsIgnoreCase("deletar") && !modoAoSairDoGrupo.equalsIgnoreCase("transferir"));

            System.out.println("Adicione membros (ENTER vazio para encerrar):");
            for (String user : registrados) {
                if (!user.equals(sessao.getNome())) {
                    System.out.println("- " + user);
                }
            }

            while (true) {
                System.out.print("Adicionar: ");
                String membro = sc.nextLine();
                if (membro.isEmpty())
                    break;
                if (registrados.contains(membro) && !membro.equals(sessao.getNome())) {
                    membros.add(membro);
                } else {
                    System.out.println("Usuário inválido.");
                }
            }

            boolean sucesso = sessao.getServidor().criarGrupo(nomeGrupo, sessao.getNome(), membros, modoAoSairDoGrupo);
            System.out.println(sucesso ? "Grupo criado com sucesso!" : "Erro: grupo já existe.");

        } catch (Exception e) {
            System.err.println("Erro ao criar grupo: " + e.getMessage());
        }
    }

    private void listarGruposEPedirEntrada() {
        try {
            List<String> meusGrupos = sessao.getServidor().listarGruposDoUsuario(sessao.getNome());
            List<String> todosUsuarios = sessao.getServidor().listarUsuariosRegistrados(); // dummy fallback

            System.out.println("Grupos disponíveis:");
            Set<String> allGrupos = new HashSet<>();

            for (String user : todosUsuarios) {
                allGrupos.addAll(sessao.getServidor().listarGruposDoUsuario(user));
            }

            allGrupos.removeAll(meusGrupos); // só grupos em que ainda não está

            for (String g : allGrupos) {
                System.out.println("- " + g);
            }

            System.out.print("Digite o nome do grupo para pedir entrada: ");
            String nomeGrupo = sc.nextLine();

            boolean sucesso = sessao.getServidor().pedirEntradaGrupo(nomeGrupo, sessao.getNome());
            System.out.println(sucesso ? "Pedido enviado ao administrador!" : "Erro ao solicitar entrada.");

        } catch (Exception e) {
            System.err.println("Erro ao listar grupos: " + e.getMessage());
        }
    }

    private void listarGruposDoUsuario() {
        try {
            List<String> grupos = sessao.getServidor().listarGruposDoUsuario(sessao.getNome());
            System.out.println("Você participa dos grupos:");
            for (String g : grupos) {
                System.out.println("- " + g);
            }
        } catch (Exception e) {
            System.err.println("Erro ao buscar seus grupos: " + e.getMessage());
        }
    }

    private void mostrarUsuariosOnline() {
        try {
            List<String> online = sessao.getServidor().listarUsuariosOnline();
            System.out.println("Usuários online:");
            for (String u : online) {
                if (u.equals(sessao.getNome())) {
                    System.out.println("- " + u + " (você)");
                } else {
                    System.out.println("- " + u);
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao listar usuários: " + e.getMessage());
        }
    }

    private void processarPedidosPendentes() {
        try {
            List<String> meusGrupos = sessao.getServidor().listarGruposDoUsuario(sessao.getNome());

            for (String grupo : meusGrupos) {
                Set<String> pendentes = sessao.getServidor().listarPedidosPendentes(grupo, sessao.getNome());
                if (pendentes.isEmpty()) continue;

                System.out.println("\nGrupo: " + grupo);
                for (String p : pendentes) {
                    System.out.print("Aceitar pedido de " + p + "? (s/n): ");
                    String op = sc.nextLine();
                    if (op.equalsIgnoreCase("s")) {
                        sessao.getServidor().aprovarEntrada(grupo, sessao.getNome(), p);
                        System.out.println(p + " foi adicionado ao grupo.");
                    } else {
                        sessao.getServidor().recusarEntrada(grupo, sessao.getNome(), p);
                        System.out.println(p + " foi recusado.");
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Erro ao processar pedidos: " + e.getMessage());
        }
    }

    private void enviarMensagemPrivada() {
        try {
            System.out.print("Digite o nome do destinatário: ");
            String para = sc.nextLine();

            System.out.print("Digite a mensagem: ");
            String conteudo = sc.nextLine();

            sessao.getServidor().enviarMensagemPrivada(sessao.getNome(), para, conteudo);
            System.out.println("Mensagem enviada com sucesso!");
        } catch (Exception e) {
            System.err.println("Erro ao enviar mensagem privada: " + e.getMessage());
        }
    }

    private void enviarMensagemGrupo() {
        try {
            System.out.print("Digite o nome do grupo: ");
            String grupo = sc.nextLine();

            System.out.print("Digite a mensagem: ");
            String conteudo = sc.nextLine();

            sessao.getServidor().enviarMensagemGrupo(sessao.getNome(), grupo, conteudo);
            System.out.println("Mensagem enviada para o grupo!");
        } catch (Exception e) {
            System.err.println("Erro ao enviar mensagem para o grupo: " + e.getMessage());
        }
    }

    private void enviarArquivoPrivado() {
        try {
            System.out.print("Para quem deseja enviar o arquivo? ");
            String destinatario = sc.nextLine();
            System.out.print("Caminho do arquivo: ");
            String caminho = sc.nextLine();

            File file = new File(caminho);
            if (!file.exists()) {
                System.out.println("Arquivo não encontrado.");
                return;
            }

            byte[] dados = Files.readAllBytes(file.toPath());
            String nomeArquivo = file.getName();

            sessao.getServidor().enviarArquivo(sessao.getNome(), destinatario, nomeArquivo, dados);
            System.out.println("Arquivo '" + nomeArquivo + "' enviado para " + destinatario + ".");

        } catch (Exception e) {
            System.err.println("Erro ao enviar arquivo: " + e.getMessage());
        }
    }

    private void banirUsuarioDoGrupo() {
        try {
            System.out.print("Grupo: ");
            String grupo = sc.nextLine();
            System.out.print("Usuário a ser removido: ");
            String alvo = sc.nextLine();
            boolean sucesso = sessao.getServidor().banirUsuarioDoGrupo(grupo, sessao.getNome(), alvo);
            System.out.println(sucesso ? "Usuário removido do grupo." : "Erro ao remover (não é admin ou grupo/usuário inválido).");
        } catch (Exception e) {
            System.err.println("Erro ao remover usuário do grupo: " + e.getMessage());
        }
    }

    private void sairDeGrupo() {
        try {
            List<String> grupos = sessao.getServidor().listarGruposDoUsuario(sessao.getNome());
            if (grupos.isEmpty()) {
                System.out.println("Você não participa de nenhum grupo.");
                return;
            }

            System.out.println("Grupos que você participa:");
            for (String g : grupos) {
                System.out.println("- " + g);
            }

            System.out.print("Digite o nome do grupo que deseja sair: ");
            String grupo = sc.nextLine();

            if (!grupos.contains(grupo)) {
                System.out.println("Você não faz parte deste grupo.");
                return;
            }

            // Verifica se o usuário é o último membro
            int qtdMembros = sessao.getServidor().listarGruposDoUsuario(grupo).size(); // Esse metodo está errado — precisamos criar um auxiliar no servidor.

            // Correção: Criar um novo metodo no servidor para consultar quantidade de membros
            int membrosNoGrupo = sessao.getServidor().getQuantidadeMembrosDoGrupo(grupo);

            if (membrosNoGrupo == 1) {
                System.out.print("Você é o único membro do grupo. Sair irá deletá-lo. Confirmar? (s/n): ");
                String confirmar = sc.nextLine();
                if (!confirmar.equalsIgnoreCase("s")) {
                    System.out.println("Operação cancelada.");
                    return;
                }
            }

            boolean sucesso = sessao.getServidor().sairDoGrupo(grupo, sessao.getNome());
            System.out.println(sucesso ? "Você saiu do grupo com sucesso!" : "Erro ao sair do grupo.");

        } catch (Exception e) {
            System.err.println("Erro ao sair do grupo: " + e.getMessage());
        }
    }

    private void solicitarBanimento() {
        try {
            System.out.print("Digite o nome do usuário que deseja banir: ");
            String alvo = sc.nextLine();

            boolean sucesso = sessao.getServidor().solicitarBanimento(sessao.getNome(), alvo);
            if (alvo.equalsIgnoreCase("admin")) {
                System.out.println("Você não pode solicitar o banimento do administrador.");
                return;
            }
            if (sessao.getNome().equalsIgnoreCase("admin")) {
                System.out.println(sucesso ? "Usuário banido imediatamente." : "Erro ao banir o usuário.");
                return;
            }
            System.out.println(sucesso ? "Solicitação enviada ao administrador." : "Erro ao solicitar banimento.");
        } catch (Exception e) {
            System.err.println("Erro ao solicitar banimento: " + e.getMessage());
        }
    }

    private void processarSolicitacoesBanimento() {
        try {
            if (!sessao.getNome().equals("admin")) {
                System.out.println("Acesso negado. Apenas o admin pode ver solicitações.");
                return;
            }

            Map<String, Set<String>> pedidos = sessao.getServidor().listarPedidosBanimento(sessao.getNome());

            if (pedidos.isEmpty()) {
                System.out.println("Nenhuma solicitação de banimento pendente.");
                return;
            }

            for (String alvo : pedidos.keySet()) {
                System.out.println("\nUsuário: " + alvo);
                System.out.println("Solicitado por: " + String.join(", ", pedidos.get(alvo)));
                System.out.print("Deseja banir este usuário? (s/n): ");
                String op = sc.nextLine();
                boolean aprovar = op.equalsIgnoreCase("s");

                boolean resultado = sessao.getServidor().processarBanimento(sessao.getNome(), alvo, aprovar);
                System.out.println(resultado ? (aprovar ? "Usuário banido." : "Solicitação rejeitada.") : "Erro ao processar.");
            }

        } catch (Exception e) {
            System.err.println("Erro ao processar solicitações: " + e.getMessage());
        }
    }

}