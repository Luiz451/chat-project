import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import protocol.ProtocolPack;
import persistence.ServerPersistence;
import persistence.ServerPersistenceArquivo;

public class ChatServer {
    private static final int PORTA = 12345;
    private static final Map<String, ClientHandler> sessoesAtivas = new ConcurrentHashMap<>();
    private static final ServerPersistence bancoDeDados = new ServerPersistenceArquivo();

    // Inicia carregando os utilizadores do disco!
    private static final Map<String, String> usuariosCadastrados = new ConcurrentHashMap<>(bancoDeDados.carregarUsuarios());

    public static void main(String[] args) {
        try (ServerSocket servidorSocket = new ServerSocket(PORTA)) {
            System.out.println("Servidor TCP iniciado na porta " + PORTA + "...");
            while (true) {
                Socket clienteSocket = servidorSocket.accept();
                new Thread(new ClientHandler(clienteSocket)).start();
            }
        } catch (IOException e) {
            System.out.println("Erro crítico no servidor: " + e.getMessage());
        }
    }

    public static class ClientHandler implements Runnable {
        private Socket socket;
        private ObjectInputStream entrada;
        private ObjectOutputStream saida;
        private String usuarioAtual;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                saida = new ObjectOutputStream(socket.getOutputStream());
                saida.flush();
                entrada = new ObjectInputStream(socket.getInputStream());
                ProtocolPack pacoteRecebido;
                while ((pacoteRecebido = (ProtocolPack) entrada.readObject()) != null) {
                    processarPacote(pacoteRecebido);
                }
            } catch (IOException | ClassNotFoundException e) {
            } finally {
                desconectar();
            }
        }

        private void processarPacote(ProtocolPack pacote) {
            ProtocolPack resposta;
            switch (pacote.getEvent()) {
                case REGISTRO:
                    String newUser = pacote.getSender();
                    if (!usuariosCadastrados.containsKey(newUser)) {
                        usuariosCadastrados.put(newUser, pacote.getText());
                        bancoDeDados.salvarUsuario(newUser, pacote.getText()); // Grava fisicamente
                        enviarPacote(new ProtocolPack(ProtocolPack.Evento.RESPOSTA_REGISTRO, "Servidor", newUser, System.currentTimeMillis(), "Sucesso", true));
                        enviarListaContatosParaTodos();
                    }
                    break;
                case PEDIDO_ACESSO:
                    String loginUser = pacote.getSender();
                    if (usuariosCadastrados.containsKey(loginUser) && usuariosCadastrados.get(loginUser).equals(pacote.getText())) {
                        this.usuarioAtual = loginUser;
                        sessoesAtivas.put(loginUser, this);
                        enviarPacote(new ProtocolPack(ProtocolPack.Evento.RESPOSTA_ACESSO, "Servidor", loginUser, System.currentTimeMillis(), "Sucesso", true));
                        entregarMensagensPendentes();
                        enviarListaContatosParaTodos();
                    } else {
                        enviarPacote(new ProtocolPack(ProtocolPack.Evento.RESPOSTA_ACESSO, "Servidor", loginUser, System.currentTimeMillis(), "Falha", false));
                    }
                    break;
                case MENSAGEM:
                    if (usuarioAtual != null) {
                        String destinatario = pacote.getRecipient();
                        ProtocolPack pacoteEntrega = new ProtocolPack(ProtocolPack.Evento.ENTREGA_MENSAGEM, usuarioAtual, destinatario, pacote.getTimestamp(), pacote.getText(), true);
                        ClientHandler handlerDestino = sessoesAtivas.get(destinatario);
                        if (handlerDestino != null) {
                            handlerDestino.enviarPacote(pacoteEntrega);
                        } else {
                            bancoDeDados.salvarMensagem(pacoteEntrega);
                        }
                    }
                    break;
                case INICIO_DIGITACAO:
                case FIM_DIGITACAO:
                    if (usuarioAtual != null) {
                        ClientHandler handlerDestino = sessoesAtivas.get(pacote.getRecipient());
                        if (handlerDestino != null) {
                            handlerDestino.enviarPacote(new ProtocolPack(pacote.getEvent(), usuarioAtual, pacote.getRecipient(), System.currentTimeMillis(), "", true));
                        }
                    }
                    break;
                default:
                    break;
            }
        }

        private void enviarListaContatosParaTodos() {
            StringBuilder sb = new StringBuilder();
            for (String user : usuariosCadastrados.keySet()) {
                String status = sessoesAtivas.containsKey(user) ? "ONLINE" : "OFFLINE";
                sb.append(user).append(":").append(status).append(",");
            }
            ProtocolPack lista = new ProtocolPack(ProtocolPack.Evento.LISTA_CONTATOS, "Servidor", "Todos", System.currentTimeMillis(), sb.toString(), true);
            for (ClientHandler ch : sessoesAtivas.values()) {
                ch.enviarPacote(lista);
            }
        }

        private void entregarMensagensPendentes() {
            List<ProtocolPack> pendentes = bancoDeDados.buscarMensagensPendentes(usuarioAtual);
            if (pendentes != null && !pendentes.isEmpty()) {
                for (ProtocolPack msg : pendentes) { enviarPacote(msg); }
                bancoDeDados.marcarComoEntregues(usuarioAtual);
            }
        }

        public void enviarPacote(ProtocolPack pacote) {
            try { saida.writeObject(pacote); saida.flush(); } catch (IOException e) {}
        }

        private void desconectar() {
            if (usuarioAtual != null) {
                sessoesAtivas.remove(usuarioAtual);
                enviarListaContatosParaTodos();
            }
        }
    }
}