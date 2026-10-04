import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ChatServer {
    private static final int PORTA = 12345;
    
   
    private static final Map<String, String> usuariosCadastrados = new ConcurrentHashMap<>();
    
    private static final Map<String, ClientHandler> sessoesAtivas = new ConcurrentHashMap<>();
   
    private static final Map<String, Queue<String>> filaOffline = new ConcurrentHashMap<>();

    public static void main(String[] args) {


        try (ServerSocket servidorSocket = new ServerSocket(PORTA)) {
            System.out.println("Servidor TCP iniciado na porta " + PORTA + "...");

            while (true) {
             
                Socket clienteSocket = servidorSocket.accept();
                System.out.println("Nova conexão estabelecida de: " + clienteSocket.getInetAddress());

                
                ClientHandler clientHandler = new ClientHandler(clienteSocket);
                new Thread(clientHandler).start();
            }

        } catch (IOException e) {
            System.out.println("Erro crítico no servidor principal: " + e.getMessage());
        }
    }

    public static class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader entrada;
        private PrintWriter saida;
        private String usuarioAtual;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                entrada = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
                );
                saida = new PrintWriter(
                    socket.getOutputStream(), true, StandardCharsets.UTF_8
                );

                String mensagemRecebida;
                while ((mensagemRecebida = entrada.readLine()) != null) {
                    processarMensagemProtocolo(mensagemRecebida);
                }

            } catch (IOException e) {
                System.out.println("Conexão perdida com o cliente: " + (usuarioAtual != null ? usuarioAtual : "anónimo"));
            } finally {
                desconectar();
            }
        }

        private void processarMensagemProtocolo(String rawMessage) {
            String[] partes = rawMessage.split(";", 4);
            String comando = partes[0];

            switch (comando) {
                case "REGISTRAR":
                    if (partes.length >= 3) {
                        String user = partes[1];
                        String pass = partes[2];
                        if (usuariosCadastrados.containsKey(user)) {
                            saida.println("RESPOSTA_REGISTRO;FALHA;Utilizador ja existe");
                        } else {
                            usuariosCadastrados.put(user, pass);
                            saida.println("RESPOSTA_REGISTRO;SUCESSO");
                        }
                    }
                    break;

                case "LOGIN":
                    if (partes.length >= 3) {
                        String user = partes[1];
                        String pass = partes[2];
                        if (usuariosCadastrados.containsKey(user) && usuariosCadastrados.get(user).equals(pass)) {
                            this.usuarioAtual = user;
                            sessoesAtivas.put(user, this);
                            saida.println("RESPOSTA_LOGIN;SUCESSO");
                            System.out.println("Utilizador autenticado com sucesso: " + user);

                            entregarMensagensOffline(user);
                        } else {
                            saida.println("RESPOSTA_LOGIN;FALHA;Credenciais invalidas");
                        }
                    }
                    break;

                case "MSG":
                    if (partes.length >= 3 && usuarioAtual != null) {
                        String destinatario = partes[1];
                        String texto = partes[2];
                        String timestamp = LocalDateTime.now().toString();
                        
                        String pacoteMensagem = String.format("ENTREGA_MSG;%s;%s;%s;%s", usuarioAtual, destinatario, timestamp, texto);
                        
                        ClientHandler handlerDestino = sessoesAtivas.get(destinatario);
                        if (handlerDestino != null) {
                            handlerDestino.saida.println(pacoteMensagem);
                        } else {
                            filaOffline.computeIfAbsent(destinatario, k -> new ConcurrentLinkedQueue<>()).add(pacoteMensagem);
                            System.out.println("Destinatário " + destinatario + " offline. Mensagem guardada na fila.");
                        }
                    }
                    break;

                case "DIGITANDO":
                    if (partes.length >= 3 && usuarioAtual != null) {
                        String destinatario = partes[1];
                        String status = partes[2];
                        ClientHandler handlerDestino = sessoesAtivas.get(destinatario);
                        if (handlerDestino != null) {
                            handlerDestino.saida.println("AVISO_DIGITACAO;" + usuarioAtual + ";" + status);
                        }
                    }
                    break;

                default:
                    saida.println("ERRO;Comando desconhecido");
                    break;
            }
        }

        private void entregarMensagensOffline(String usuario) {
            Queue<String> mensagensPendentes = filaOffline.remove(usuario);
            if (mensagensPendentes != null) {
                for (String msg : mensagensPendentes) {
                    saida.println(msg);
                }
                System.out.println("Entregues " + mensagensPendentes.size() + " mensagens offline para " + usuario);
            }
        }

        private void desconectar() {
            if (usuarioAtual != null) {
                sessoesAtivas.remove(usuarioAtual);
                System.out.println("Utilizador desconectado: " + usuarioAtual);
            }
            try {
                if (socket != null) socket.close();
            } catch (IOException e) {
                System.out.println("Erro ao fechar socket do cliente: " + e.getMessage());
            }
        }
    }
}
