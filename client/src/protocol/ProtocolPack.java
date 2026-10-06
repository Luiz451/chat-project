package protocol;

public class ProtocolPack {
    public static final long serialVersionUID = 1L;

    public enum Evento {
        REGISTRO, RESPOSTA_REGISTRO,
        PEDIDO_ACESSO, RESPOSTA_ACESSO,
        LISTA_CONTATOS, MENSAGEM, ENTREGA_MENSAGEM,
        INICIO_DIGITACAO, FIM_DIGITACAO, AVISO_DIGITACAO,
        MUDANCA_PRESENCA, FILA_OFFLINE
    }

    private Evento event;
    private String sender;
    private String recipient;
    private long timestamp;
    private String text;
    private boolean sucess;

    public ProtocolPack(Evento event, String sender, String recipient, long timestamp, String text, boolean sucess) {
        this.event = event;
        this.sender = sender;
        this.recipient = recipient;
        this.sucess = sucess;
        this.text = text;
        this.timestamp = System.currentTimeMillis();
    }

    public Evento getEvent() {
        return event;
    }

    public void setEvent(Evento event) {
        this.event = event;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isSucess() {
        return sucess;
    }

    public void setSucess(boolean sucess) {
        this.sucess = sucess;
    }
}