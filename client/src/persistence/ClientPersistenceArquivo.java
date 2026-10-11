package persistence;

import protocol.ProtocolPack;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ClientPersistenceArquivo implements ClientPersistence {
    private static final String DIRETORIO_HISTORICO = "historico_local";

    public ClientPersistenceArquivo() {
        File diretorio = new File(DIRETORIO_HISTORICO);
        if (!diretorio.exists()) {
            diretorio.mkdir();
        }
    }

    private String gerarNomeArquivo(String meuUsuario, String contato) {
        if (meuUsuario.compareTo(contato) < 0) {
            return DIRETORIO_HISTORICO + File.separator + meuUsuario + "_" + contato + ".txt";
        } else {
            return DIRETORIO_HISTORICO + File.separator + contato + "_" + meuUsuario + ".txt";
        }
    }

    @Override
    public void salvarMensagem(ProtocolPack message) {
        String nomeArquivo = gerarNomeArquivo(message.getSender(), message.getRecipient());

        try (PrintWriter out = new PrintWriter(new FileWriter(nomeArquivo, true))) {
            String textoLimpo = message.getText().replace("\n", " ");

            out.println(message.getTimestamp() + "|" + message.getSender() + "|" + message.getRecipient() + "|" + textoLimpo);
        } catch (IOException e) {
            System.err.println("Erro ao gravar mensagem no disco local: " + e.getMessage());
        }
    }

    @Override
    public List<ProtocolPack> listarHistorico(String user, String contact) {
        List<ProtocolPack> historico = new ArrayList<>();
        File arquivo = new File(gerarNomeArquivo(user, contact));

        if (!arquivo.exists()) return historico;

        try (BufferedReader br = new BufferedReader(new FileReader(arquivo))) {
            String linha;
            while ((linha = br.readLine()) != null) {
                String[] partes = linha.split("\\|", 4);
                if (partes.length == 4) {
                    ProtocolPack pacote = new ProtocolPack(
                            ProtocolPack.Evento.MENSAGEM,
                            partes[1],
                            partes[2],
                            Long.parseLong(partes[0]),
                            partes[3],
                            true
                    );
                    historico.add(pacote);
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler histórico local: " + e.getMessage());
        }
        return historico;
    }
}