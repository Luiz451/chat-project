import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private static final String ENDERECO_SERVIDOR = "127.0.0.1";
    private static final int PORTA = 12345;

    public static void main(String[] args) {
        System.out.println("Tentando conectar ao servidor...");

        try (Socket socket = new Socket(ENDERECO_SERVIDOR, PORTA);
             PrintWriter saida = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner teclado = new Scanner(System.in)) {

            System.out.println("Conectado com sucesso! Digite sua mensagem (ou 'sair' para encerrar):");

            while (true) {
                System.out.print("> ");
                String mensagem = teclado.nextLine();

                if ("sair".equalsIgnoreCase(mensagem)) {
                    break;
                }

                saida.println(mensagem);
                String resposta = entrada.readLine();
                System.out.println("Servidor respondeu: " + resposta);
            }

        } catch (IOException e) {
            System.err.println("Erro na conexão com o servidor: " + e.getMessage());
        }
    }
}