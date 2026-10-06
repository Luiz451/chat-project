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

    public void setEvento(Evento evento) {
        this.evento = evento;
    }

    public String getRemetente() {
        return remetente;
    }

    public void setRemetente(String remetente) {
        this.remetente = remetente;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(String destinatario) {
        this.destinatario = destinatario;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public boolean isSucesso() {
        return sucesso;
    }

    public void setSucesso(boolean sucesso) {
        this.sucesso = sucesso;
    }
}