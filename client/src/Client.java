import protocol.ProtocolPack;
import persistence.ClientPersistence;
import persistence.ClientPersistenceArquivo;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Scanner;

public class Client {
    private static final String ENDERECO_SERVIDOR = "127.0.0.1";
    private static final int PORTA = 12345;
    private static String meuUsuario = "anônimo";

    private static ObjectOutputStream saida;
    private static ClientPersistence bancoLocal;
    private static LocalUIBridge interfaceWeb;

    public static void main(String[] args) {
        System.out.println("Iniciando Cliente...");
        Scanner teclado = new Scanner(System.in);

        try {
            Socket socket = new Socket(ENDERECO_SERVIDOR, PORTA);
            saida = new ObjectOutputStream(socket.getOutputStream());
            saida.flush();
            ObjectInputStream entrada = new ObjectInputStream(socket.getInputStream());

            bancoLocal = new ClientPersistenceArquivo();

            Client instanciaCliente = new Client();
            interfaceWeb = new LocalUIBridge(0, instanciaCliente);
            interfaceWeb.start();

            try { Thread.sleep(300); } catch (InterruptedException e) {}

            int portaAutomatica = interfaceWeb.getPort();
            System.out.println("Ponte WebSocket iniciada automaticamente na porta " + portaAutomatica);

            try {
                File htmlFile = new File("view/pages/index.html");
                if (htmlFile.exists()) {
                    String url = htmlFile.toURI().toString() + "?port=" + portaAutomatica;
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
                    System.out.println("Navegador aberto automaticamente!");
                } else {
                    System.out.println("HTML não encontrado automaticamente. Abre o ficheiro view/pages/index.html?port=" + portaAutomatica + " no teu navegador.");
                }
            } catch (Exception e) {
                System.out.println("Aviso: Não foi possível abrir o navegador. Faz a abertura manualmente.");
            }

            System.out.println("Conectado ao Servidor Central!");

            Thread threadRecepcao = new Thread(new MessageReceiver(entrada, bancoLocal, interfaceWeb));
            threadRecepcao.start();

            while (true) {
                if ("sair".equalsIgnoreCase(teclado.nextLine().trim())) {
                    System.out.println("Cliente Encerrado!");
                    System.exit(0);
                }
            }

        } catch (IOException e) {
            System.err.println("Erro na conexão: " + e.getMessage());
        }
    }

    public void processarComandoDaInterface(String input) {
        try {
            if (input.startsWith("CARREGAR_HISTORICO;")) {
                String contato = input.split(";", 2)[1].trim();
                List<ProtocolPack> historico = bancoLocal.listarHistorico(meuUsuario, contato);

                for (ProtocolPack p : historico) {
                    interfaceWeb.enviarParaHTML(p.getSender(), p.getText());
                }
                return;
            }

            String[] partes = input.split(";", 3);
            if (partes.length >= 2) {
                ProtocolPack.Evento evento = ProtocolPack.Evento.valueOf(partes[0].trim().toUpperCase());
                String destinoOuUsuario = partes[1].trim();
                String texto = partes.length > 2 ? partes[2].trim() : "";

                if (evento == ProtocolPack.Evento.REGISTRO || evento == ProtocolPack.Evento.PEDIDO_ACESSO) {
                    meuUsuario = destinoOuUsuario;
                }

                ProtocolPack pacote = new ProtocolPack(
                        evento, meuUsuario, destinoOuUsuario, System.currentTimeMillis(), texto, true
                );

                saida.writeObject(pacote);
                saida.flush();

                if (evento == ProtocolPack.Evento.MENSAGEM) {
                    bancoLocal.salvarMensagem(pacote);
                }
            }
        } catch (Exception e) {
            System.out.println("Erro ao processar comando da web: " + e.getMessage());
        }
    }
}