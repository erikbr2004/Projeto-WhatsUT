# WhatsUT — Mensageiro Distribuído com Java RMI

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-1F8AC0?style=flat-square)](https://openjfx.io/)
[![RMI](https://img.shields.io/badge/Comunica%C3%A7%C3%A3o-Java%20RMI-B07219?style=flat-square)](https://docs.oracle.com/javase/tutorial/rmi/)
[![Maven](https://img.shields.io/badge/Build-Maven-C71A36?style=flat-square&logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![Arquitetura](https://img.shields.io/badge/Arquitetura-Cliente--Servidor-2E7D32?style=flat-square)](#arquitetura)
[![JUnit](https://img.shields.io/badge/Testes-JUnit%205-25A162?style=flat-square&logo=junit5&logoColor=white)](https://junit.org/junit5/)

Aplicação de mensagens instantâneas cliente-servidor construída sobre **Java RMI**, com
conversas privadas, grupos com moderação, transferência de arquivos e notificação em tempo
real via callbacks remotos. A interface é feita em JavaFX.

---

## Sobre o projeto

O objetivo foi implementar um sistema distribuído real — não uma simulação em um único
processo. Servidor e clientes são aplicações independentes que se comunicam por **invocação
remota de métodos**, e o desafio central foi fazer a comunicação funcionar nas duas
direções.

Em RMI, o fluxo natural é o cliente chamar métodos no servidor. Para que uma mensagem
enviada por um usuário apareça instantaneamente na tela de outro, o projeto inverte esse
fluxo: cada cliente também exporta um objeto remoto (`IClient`) e o registra no servidor ao
conectar. Quando alguém envia uma mensagem, o servidor percorre os destinatários online e
**chama métodos no cliente**, empurrando o conteúdo em vez de esperar por polling.

```java
public interface IClient extends Remote {
    void receberMensagem(Mensagem mensagem) throws RemoteException;
    void receberArquivo(String remetente, String nomeArquivo, byte[] dados) throws RemoteException;
}
```

Essa arquitetura bidirecional é o que dá ao sistema o comportamento de tempo real.

---

## Arquitetura

```text
┌─────────────────────┐                              ┌─────────────────────┐
│   Cliente (JavaFX)  │                              │   Cliente (JavaFX)  │
│                     │                              │                     │
│  ClienteService ────┼──── chamadas IServer ───────>│                     │
│  ClienteImpl  <─────┼──── callbacks IClient ───────┼───> ClienteImpl     │
└─────────────────────┘                              └─────────────────────┘
           │                                                    │
           └──────────────────┐              ┌──────────────────┘
                              v              v
                     ┌────────────────────────────────┐
                     │  RMI Registry (porta 1099)     │
                     │  bind: "WhatsUTServer"         │
                     ├────────────────────────────────┤
                     │  ServidorImpl                  │
                     │  - usuários registrados        │
                     │  - usuários online (IClient)   │
                     │  - grupos e pendências         │
                     ├────────────────────────────────┤
                     │  Persistência JSON (data/)     │
                     └────────────────────────────────┘
```

O `ServidorImpl` mantém o estado em memória para acesso rápido e o espelha em disco a cada
alteração. Todos os métodos que tocam estado compartilhado são `synchronized`, já que o RMI
atende cada chamada remota em uma thread própria — sem isso, dois usuários criando grupos
simultaneamente poderiam corromper a estrutura.

---

## Funcionalidades

### Usuários e autenticação
- Cadastro e login com senha armazenada como **hash SHA-256**, nunca em texto plano.
- Lista de usuários online atualizada conforme conexões e desconexões.
- Sessão de cliente isolada em `SessaoCliente`.

### Mensagens
- Conversas privadas entre dois usuários.
- Mensagens em grupo entregues a todos os membros conectados.
- Histórico persistido em JSON, recuperado ao reabrir a conversa.
- Entrega em tempo real por callback, sem polling.

### Grupos com moderação
- Criação de grupos com seleção de membros iniciais.
- **Solicitação de entrada** com aprovação ou recusa pelo administrador do grupo.
- Remoção de integrantes pelo administrador.
- **Política configurável de saída do administrador**: ao criar o grupo, define-se se ele
  será excluído (`deletar`) ou se a administração passa a outro membro (`transferir`).
- Grupos sem membros são removidos automaticamente.

### Transferência de arquivos
- Envio de arquivos em conversas privadas e em grupo, trafegando como `byte[]` sobre RMI.
- Recebimento automático na pasta `transferencias/` do cliente.

### Moderação global
- Banimento de usuários da aplicação, restrito ao administrador do sistema.
- Fluxo de **solicitação de banimento**: qualquer usuário pode solicitar, o administrador
  lista as pendências e aprova ou rejeita.
- Usuários banidos são removidos de todos os grupos e registrados em arquivo próprio.

---

## Estrutura do repositório

```text
src/main/java/whatsut/
├── App.java                    # Ponto de entrada do cliente JavaFX
├── interfaces/
│   ├── IServer.java            # Contrato remoto do servidor
│   └── IClient.java            # Contrato remoto de callback do cliente
├── server/
│   ├── MainServidor.java       # Cria o registry e publica o servidor
│   └── ServidorImpl.java       # Lógica de negócio e estado compartilhado
├── cliente/
│   ├── ClienteMain.java        # Cliente em modo console
│   ├── ClienteService.java     # Conexão RMI e operações do cliente
│   ├── ClienteImpl.java        # Objeto remoto que recebe os callbacks
│   └── SessaoCliente.java      # Estado da sessão do usuário logado
├── controllers/                # Controllers JavaFX (login, principal, grupos, banimento)
├── views/                      # Inicialização das telas
├── model/                      # Usuario, Grupo, Mensagem (Serializable)
└── util/
    ├── HashUtil.java           # Hash SHA-256 de senhas
    ├── UsuarioStorage.java     # Persistência de usuários e banidos
    ├── GrupoStorage.java       # Persistência de grupos
    └── MensagemStorage.java    # Persistência de histórico

src/main/resources/whatsut/     # Arquivos FXML e folhas de estilo
data/                           # Base JSON gerada em tempo de execução
transferencias/                 # Arquivos recebidos pelo cliente
```

---

## Como executar

Requisitos: **JDK 21** e Maven (ou o wrapper `mvnw` incluído).

### 1. Inicie o servidor

```bash
mvn compile exec:java -Dexec.mainClass=whatsut.server.MainServidor
```

O servidor cria o registry na porta `1099` e publica a instância sob o nome
`WhatsUTServer`. A confirmação aparece no console:

```text
Servidor WhatsUT ativo e registrado como 'WhatsUTServer'
```

### 2. Inicie o cliente

Interface gráfica (JavaFX):

```bash
mvn clean javafx:run
```

Cliente em modo console:

```bash
mvn compile exec:java -Dexec.mainClass=whatsut.cliente.ClienteMain
```

Abra quantas instâncias de cliente quiser para testar conversas simultâneas.

> O `pom.xml` configura o plugin JavaFX com o main class no formato modular
> (`whatsut.wut/whatsut.App`). Como o projeto não versiona um `module-info.java`, execute a
> interface pelo classpath sobrescrevendo a propriedade caso o comando acima falhe:
> `mvn clean javafx:run -Djavafx.mainClass=whatsut.App`.

---

## Persistência

O estado é gravado em arquivos JSON dentro de `data/`, criados automaticamente na primeira
execução:

```text
data/
├── usuarios.json               # Usuários registrados e hashes de senha
├── usuariosBanidos.json        # Usuários banidos da aplicação
├── grupos.json                 # Grupos, membros, pendências e administradores
├── mensagens/                  # Histórico das conversas privadas
│   └── privado_<a>_<b>.json
└── mensagensGrupos/            # Histórico das conversas em grupo
    └── grupo_<nome>.json
```

A escolha por JSON em vez de um banco relacional foi deliberada: mantém o projeto executável
sem nenhuma dependência externa de infraestrutura, e o foco do trabalho era a camada
distribuída, não a de armazenamento.

---

## Decisões técnicas e limitações conhecidas

- **SHA-256 sem salt** — adequado ao escopo acadêmico do trabalho, mas em produção o correto
  seria um algoritmo com fator de custo e salt por usuário, como bcrypt ou Argon2.
- **Escrita síncrona em arquivo** — cada alteração regrava o JSON correspondente. Simples e
  suficiente para a escala do projeto, mas não escala para volume alto de mensagens.
- **Administrador global fixo** — o usuário chamado `admin` concentra os privilégios de
  moderação da aplicação, sem um sistema de papéis mais granular.
- **RMI exige acessibilidade de rede nos dois sentidos**, já que o servidor invoca métodos
  nos clientes. Em execução local ou em rede confiável funciona diretamente; atravessar NAT
  exigiria configuração adicional.

---

## Tecnologias

`Java 21` · `Java RMI` · `JavaFX 21` · `FXML` · `Maven` · `org.json` · `ControlsFX` ·
`ValidatorFX` · `Ikonli` · `JUnit 5`

---

## Conceitos exercitados

- Sistemas distribuídos e invocação remota de métodos
- Comunicação bidirecional com callbacks remotos e o padrão Observer
- Concorrência e proteção de estado compartilhado em servidor multithread
- Serialização de objetos para trânsito em rede
- Arquitetura em camadas: interfaces remotas, serviço, modelo e persistência
- Separação entre lógica e apresentação com FXML e controllers
- Fundamentos de segurança: hash de credenciais e controle de permissões

---

## Autores
- Erik Barbosa de Castro
- Douglas Rezende Chagas
- Lucas Maues
- Diego Hatori Dallaqua
