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

    private static final Map<String, String> usuariosCadastrados = new ConcurrentHashMap<>();

    private static final ServerPersistence bancoDeDados = new ServerPersistenceArquivo();

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
                System.out.println("Conexão perdida com: " + (usuarioAtual != null ? usuarioAtual : "anônimo"));
            } finally {
                desconectar();
            }
        }

        private void processarPacote(ProtocolPack pacote) {
            ProtocolPack resposta;

            switch (pacote.getEvent()) {
                case REGISTRO:
                    String newUser = pacote.getSender();
                    String newPass = pacote.getText();

                    if (usuariosCadastrados.containsKey(newUser)) {
                        resposta = new ProtocolPack(ProtocolPack.Evento.RESPOSTA_REGISTRO, "Servidor", newUser, System.currentTimeMillis(), "Falha: Utilizador já existe", false);
                    } else {
                        usuariosCadastrados.put(newUser, newPass);
                        resposta = new ProtocolPack(ProtocolPack.Evento.RESPOSTA_REGISTRO, "Servidor", newUser, System.currentTimeMillis(), "Sucesso", true);
                    }
                    enviarPacote(resposta);
                    break;

                case PEDIDO_ACESSO:
                    String loginUser = pacote.getSender();
                    String loginPass = pacote.getText();

                    if (usuariosCadastrados.containsKey(loginUser) && usuariosCadastrados.get(loginUser).equals(loginPass)) {
                        this.usuarioAtual = loginUser;
                        sessoesAtivas.put(loginUser, this);
                        resposta = new ProtocolPack(ProtocolPack.Evento.RESPOSTA_ACESSO, "Servidor", loginUser, System.currentTimeMillis(), "Sucesso", true);
                        enviarPacote(resposta);

                        entregarMensagensPendentes();
                    } else {
                        resposta = new ProtocolPack(ProtocolPack.Evento.RESPOSTA_ACESSO, "Servidor", loginUser, System.currentTimeMillis(), "Falha: Credenciais inválidas", false);
                        enviarPacote(resposta);
                    }
                    break;

                case MENSAGEM:
                    if (usuarioAtual != null) {
                        String destinatario = pacote.getRecipient();

                        ProtocolPack pacoteEntrega = new ProtocolPack(
                                ProtocolPack.Evento.ENTREGA_MENSAGEM,
                                usuarioAtual,
                                destinatario,
                                pacote.getTimestamp(),
                                pacote.getText(),
                                true
                        );

                        ClientHandler handlerDestino = sessoesAtivas.get(destinatario);
                        if (handlerDestino != null) {
                            handlerDestino.enviarPacote(pacoteEntrega);
                        } else {
                            bancoDeDados.salvarMensagem(pacoteEntrega);
                            System.out.println("Mensagem de " + usuarioAtual + " para " + destinatario + " guardada na fila.");
                        }
                    }
                    break;

                default:
                    break;
            }
        }

        private void entregarMensagensPendentes() {
            List<ProtocolPack> pendentes = bancoDeDados.buscarMensagensPendentes(usuarioAtual);
            if (pendentes != null && !pendentes.isEmpty()) {
                for (ProtocolPack msg : pendentes) {
                    enviarPacote(msg);
                }
                bancoDeDados.marcarComoEntregues(usuarioAtual);
            }
        }

        public void enviarPacote(ProtocolPack pacote) {
            try {
                saida.writeObject(pacote);
                saida.flush();
            } catch (IOException e) {
            }
        }

        private void desconectar() {
            if (usuarioAtual != null) {
                sessoesAtivas.remove(usuarioAtual);
            }
        }
    }
}