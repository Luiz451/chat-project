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

    private Evento evento;
    private String remetente;
    private String destinatario;
    private long timestamp;
    private String texto;
    private boolean sucesso;

    public ProtocolPack(Evento evento, String remetente, String destinatario, long timestamp, String texto, boolean sucesso) {
        this.evento = evento;
        this.remetente = remetente;
        this.destinatario = destinatario;
        this.texto = texto;
        this.timestamp = System.currentTimeMillis();
    }

    public Evento getEvento() {
        return evento;
    }

    public String getRemetente() {
        return remetente;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public String getTexto() {
        return texto;
    }

    public boolean isSucesso() {
        return sucesso;
    }

    public void setSucesso(boolean sucesso) {
        this.sucesso = sucesso;
    }
}