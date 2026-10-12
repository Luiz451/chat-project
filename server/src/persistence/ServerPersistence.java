package persistence;

import protocol.ProtocolPack;
import java.util.List;
import java.util.Map;

public interface ServerPersistence {
    void salvarMensagem(ProtocolPack message);
    List<ProtocolPack> listarHistorico(String user1, String user2);
    List<ProtocolPack> buscarMensagensPendentes(String username);
    void marcarComoEntregues(String username);

    void salvarUsuario(String username, String password);
    Map<String, String> carregarUsuarios();
}