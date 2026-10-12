import protocol.ProtocolPack;
import persistence.ClientPersistence;
import java.io.IOException;
import java.io.ObjectInputStream;

class MessageReceiver implements Runnable {
    private final ObjectInputStream entrada;
    private final ClientPersistence bancoLocal;
    private final LocalUIBridge interfaceWeb;

    public MessageReceiver(ObjectInputStream entrada, ClientPersistence bancoLocal, LocalUIBridge interfaceWeb) {
        this.entrada = entrada;
        this.bancoLocal = bancoLocal;
        this.interfaceWeb = interfaceWeb;
    }

    @Override
    public void run() {
        try {
            ProtocolPack pacoteServidor;
            while ((pacoteServidor = (ProtocolPack) entrada.readObject()) != null) {

                if (pacoteServidor.getEvent() == ProtocolPack.Evento.ENTREGA_MENSAGEM) {
                    System.out.println("\n[" + pacoteServidor.getSender() + " -> Você]: " + pacoteServidor.getText());
                    bancoLocal.salvarMensagem(pacoteServidor);
                }

                interfaceWeb.enviarParaHTML(pacoteServidor.getEvent().name(), pacoteServidor.getSender(), pacoteServidor.getText());

                System.out.print("> ");
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("\nConexão com o servidor foi encerrada.");
        }
    }
}