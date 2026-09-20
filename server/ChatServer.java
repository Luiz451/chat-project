import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ChatServer {
    public static void main(String[] args) {
        int porta = 12345; 

        
        try (ServerSocket servidorSocket = new ServerSocket(porta)) {
            System.out.println("Servidor TCP iniciado na porta " + porta + "...");
            System.out.println("Aguardando conexão do cliente...");

            
            Socket clienteSocket = servidorSocket.accept();
            System.out.println("Cliente conectado com sucesso: " + clienteSocket.getInetAddress());

            
            BufferedReader entrada = new BufferedReader(
                new InputStreamReader(
                    clienteSocket.getInputStream(),
                    StandardCharsets.UTF_8
                )
            );

          
            PrintWriter saida = new PrintWriter(
                clienteSocket.getOutputStream(),
                true,
                StandardCharsets.UTF_8
            );

       
            String mensagemCliente = entrada.readLine();
            System.out.println("Mensagem recebida do cliente: " + mensagemCliente);

          
            saida.println("Olá, cliente! Mensagem recebida com sucesso pelo servidor.");

            System.out.println("Comunicação encerrada.");

        } catch (IOException e) {
            System.out.println("Ocorreu um erro no servidor: " + e.getMessage());
        }
    }
}