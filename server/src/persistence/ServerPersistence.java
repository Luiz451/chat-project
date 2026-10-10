package persistence;

import protocol.ProtocolPack;
import java.util.List;

public interface ServerPersistence {

    // Salva uma mensagem que passou pelo servidor (para histórico geral ou entrega futura)
    void salvarMensagem(ProtocolPack message);

    // Lista o histórico entre dois usuários na perspectiva do servidor
    List<ProtocolPack> listarHistorico(String user1, String user2);

   
    List<ProtocolPack> buscarMensagensPendentes(String username);


    void marcarComoEntregues(String username);
}