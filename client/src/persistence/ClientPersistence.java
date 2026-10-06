package persistence;

import protocol.ProtocolPack;

import java.util.List;

public interface ClientPersistence {
    void salvarMensagem(ProtocolPack message);

    List<ProtocolPack> listarHistorico(String user, String contact);
}