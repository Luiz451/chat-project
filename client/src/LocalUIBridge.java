import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import java.net.InetSocketAddress;

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
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {}

    @Override
    public void onMessage(WebSocket conn, String message) {
        clienteJava.processarComandoDaInterface(message);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {}

    @Override
    public void onStart() {}

    public void enviarParaHTML(String evento, String remetente, String texto) {
        if (conexaoFrontend != null && conexaoFrontend.isOpen()) {
            conexaoFrontend.send(evento + "|" + remetente + "|" + texto);
        }
    }
}