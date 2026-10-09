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
                // Mantemos UTF-8 aqui, mas o PrintWriter do cliente do seu amigo 
                // usará o padrão do sistema dele (o que funcionará sem problemas de leitura/escrita básica)
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
                    } else {
                        saida.println("ERRO;Parametros insuficientes para REGISTRAR");
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
                        } else {
                            saida.println("RESPOSTA_LOGIN;FALHA;Credenciais invalidas");
                        }
                    } else {
                        saida.println("ERRO;Parametros insuficientes para LOGIN");
                    }
                    break;

                // Novo comando "Pull" para o cliente consultar quem está online/offline sob demanda
                case "LISTAR_USUARIOS":
                    if (usuarioAtual != null) {
                        StringBuilder resposta = new StringBuilder("RESPOSTA_LISTA_USUARIOS");
                        for (String user : usuariosCadastrados.keySet()) {
                            String status = sessoesAtivas.containsKey(user) ? "ONLINE" : "OFFLINE";
                            resposta.append(";").append(user).append(":").append(status);
                        }
                        saida.println(resposta.toString());
                    } else {
                        saida.println("ERRO;Necessario efetuar login");
                    }
                    break;

                    case "MSG":
                        if (partes.length >= 3 && usuarioAtual != null) {
                            String destinatario = partes[1];
                            String texto = partes[2];
                            String timestamp = LocalDateTime.now().toString();
                            
                            String pacoteMensagem = String.format("ENTREGA_MSG;%s;%s;%s;%s", usuarioAtual, destinatario, timestamp, texto);
                            
                            // Confirma para quem enviou que a solicitação foi processada
                            saida.println("RESPOSTA_MSG;SUCESSO");
    
                            ClientHandler handlerDestino = sessoesAtivas.get(destinatario);
                            if (handlerDestino != null) {
                                // Como o cliente do seu colega já tem thread de escuta, 
                                // podemos enviar a mensagem DIRETAMENTE em tempo real!
                                handlerDestino.saida.println(pacoteMensagem);
                            } else {
                                // Se estiver offline, guarda na fila para quando ele logar
                                filaOffline.computeIfAbsent(destinatario, k -> new ConcurrentLinkedQueue<>()).add(pacoteMensagem);
                                System.out.println("Destinatário " + destinatario + " offline. Mensagem guardada na fila.");
                            }
                        } else {
                            saida.println("ERRO;Parametros invalidos ou nao autenticado");
                        }
                        break;
                
                case "BUSCAR_MENSAGENS":
                    if (usuarioAtual != null) {
                        Queue<String> mensagensPendentes = filaOffline.remove(usuarioAtual);
                        if (mensagensPendentes != null && !mensagensPendentes.isEmpty()) {
                            StringBuilder sb = new StringBuilder("RESPOSTA_MENSAGENS");
                            for (String msg : mensagensPendentes) {
                                sb.append("|").append(msg);
                            }
                            saida.println(sb.toString());
                        } else {
                            saida.println("RESPOSTA_MENSAGENS;VAZIO");
                        }
                    } else {
                        saida.println("ERRO;Necessario efetuar login");
                    }
                    break;

                default:
                    saida.println("ERRO;Comando desconhecido");
                    break;
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