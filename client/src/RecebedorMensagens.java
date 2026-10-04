import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

class RecebedorMensagens implements Runnable {
    private final Socket socket;

    public RecebedorMensagens(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String mensagemServidor;

            while ((mensagemServidor = entrada.readLine()) != null) {
                System.out.println("\n[Servidor]: " + mensagemServidor);
                System.out.print("> ");
            }
        } catch (IOException e) {
            System.out.println("\nConexão com o servidor foi encerrada.");
        }
    }
}
