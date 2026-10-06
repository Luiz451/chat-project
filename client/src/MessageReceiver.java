import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

class MessageReceiver implements Runnable {
    private final Socket socket;

    public MessageReceiver(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String ServerMessage;

            while ((ServerMessage = input.readLine()) != null) {
                System.out.println("\n[Servidor]: " + ServerMessage);
                System.out.print("> ");
            }
        } catch (IOException e) {
            System.out.println("\nConexão com o servidor foi encerrada.");
        }
    }
}
