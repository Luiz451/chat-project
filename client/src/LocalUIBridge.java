import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import java.net.InetSocketAddress;

// Este é o mini-servidor que fala com o teu HTML
public class LocalUIBridge extends WebSocketServer {

    private Client clienteJava;
    private WebSocket conexaoFrontend;

    public LocalUIBridge(int porta, Client clienteJava) {
        super(new InetSocketAddress(porta));
        this.clienteJava = clienteJava;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        this.conexaoFrontend = conn;
        System.out.println("Interface HTML conectada ao Java!");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Interface HTML desconectada.");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // O HTML mandou algo (ex: "MENSAGEM;maria;Olá!")
        // Vamos repassar isso para o Client.java processar e enviar ao ChatServer
        clienteJava.processarComandoDaInterface(message);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("Erro no WebSocket local: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("Ponte WebSocket iniciada na porta " + getPort());
    }

    // Método para o Java empurrar mensagens para a tela do HTML
    public void enviarParaHTML(String remetente, String texto) {
        if (conexaoFrontend != null && conexaoFrontend.isOpen()) {
            conexaoFrontend.send(remetente + "|" + texto);
        }
    }
}