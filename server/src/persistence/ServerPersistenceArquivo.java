package persistence;

import protocol.ProtocolPack;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerPersistenceArquivo implements ServerPersistence {
    private static final String DIRETORIO_HISTORICO = "historico_servidor";
    private static final String DIRETORIO_OFFLINE = "fila_offline";
    private static final String ARQUIVO_USUARIOS = "usuarios_cadastrados.txt";

    public ServerPersistenceArquivo() {
        File dirHistorico = new File(DIRETORIO_HISTORICO);
        if (!dirHistorico.exists()) {
            dirHistorico.mkdir();
        }

        File dirOffline = new File(DIRETORIO_OFFLINE);
        if (!dirOffline.exists()) {
            dirOffline.mkdir();
        }
    }

    private String gerarNomeArquivoHistorico(String usuarioA, String usuarioB) {
        if (usuarioA.compareTo(usuarioB) < 0) {
            return DIRETORIO_HISTORICO + File.separator + usuarioA + "_" + usuarioB + ".txt";
        } else {
            return DIRETORIO_HISTORICO + File.separator + usuarioB + "_" + usuarioA + ".txt";
        }
    }

    @Override
    public void salvarMensagem(ProtocolPack message) {
        // Salva tanto no histórico geral da conversa (para consulta futura)...
        salvarNoHistoricoGeral(message);

        // ...quanto na fila offline do destinatário (caso precise ser entregue depois)
        String arquivoFila = DIRETORIO_OFFLINE + File.separator + message.getRecipient() + ".txt";
        try (PrintWriter out = new PrintWriter(new FileWriter(arquivoFila, true))) {
            String textoLimpo = message.getText().replace("\n", " ");
            out.println(message.getTimestamp() + "|" + message.getSender() + "|" + textoLimpo);
        } catch (IOException e) {
            System.err.println("Erro ao salvar mensagem pendente: " + e.getMessage());
        }
    }

    private void salvarNoHistoricoGeral(ProtocolPack message) {
        String nomeArquivo = gerarNomeArquivoHistorico(message.getSender(), message.getRecipient());
        try (PrintWriter out = new PrintWriter(new FileWriter(nomeArquivo, true))) {
            String textoLimpo = message.getText().replace("\n", " ");
            out.println(message.getTimestamp() + "|" + message.getSender() + "|" + message.getRecipient() + "|" + textoLimpo);
        } catch (IOException e) {
            System.err.println("Erro ao gravar no histórico do servidor: " + e.getMessage());
        }
    }

    @Override
    public List<ProtocolPack> listarHistorico(String user1, String user2) {
        List<ProtocolPack> historico = new ArrayList<>();
        File arquivo = new File(gerarNomeArquivoHistorico(user1, user2));

        if (!arquivo.exists()) return historico;

        try (BufferedReader br = new BufferedReader(new FileReader(arquivo))) {
            String linha;
            while ((linha = br.readLine()) != null) {
                String[] partes = linha.split("\\|", 4);
                if (partes.length == 4) {
                    ProtocolPack pacote = new ProtocolPack(
                            ProtocolPack.Evento.ENTREGA_MENSAGEM,
                            partes[1], // remetente
                            partes[2], // destinatário
                            Long.parseLong(partes[0]), // timestamp
                            partes[3], // texto
                            true
                    );
                    historico.add(pacote);
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler o arquivo de histórico: " + e.getMessage());
        }
        return historico;
    }

    @Override
    public List<ProtocolPack> buscarMensagensPendentes(String username) {
        List<ProtocolPack> fila = new ArrayList<>();
        File arquivo = new File(DIRETORIO_OFFLINE + File.separator + username + ".txt");

        if (!arquivo.exists()) return fila;

        try (BufferedReader br = new BufferedReader(new FileReader(arquivo))) {
            String linha;
            while ((linha = br.readLine()) != null) {
                String[] partes = linha.split("\\|", 3);
                if (partes.length == 3) {
                    ProtocolPack pack = new ProtocolPack(
                            ProtocolPack.Evento.ENTREGA_MENSAGEM,
                            partes[1], // remetente
                            username,  // destinatario
                            Long.parseLong(partes[0]), // timestamp
                            partes[2], // texto
                            true
                    );
                    fila.add(pack);
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao ler fila offline: " + e.getMessage());
        }
        return fila;
    }

    @Override
    public void marcarComoEntregues(String username) {
        File arquivo = new File(DIRETORIO_OFFLINE + File.separator + username + ".txt");
        if (arquivo.exists()) {
            arquivo.delete(); // Esvazia a fila apagando o arquivo temporário
        }
    }

    // --- NOVOS MÉTODOS PARA PERSISTÊNCIA DE USUÁRIOS ---

    @Override
    public void salvarUsuario(String username, String senha) {
        try (PrintWriter out = new PrintWriter(new FileWriter(ARQUIVO_USUARIOS, true))) {
            out.println(username + "|" + senha);
        } catch (IOException e) {
            System.err.println("Erro ao salvar usuário no arquivo: " + e.getMessage());
        }
    }

    @Override
    public Map<String, String> carregarUsuarios() {
        Map<String, String> usuarios = new ConcurrentHashMap<>();
        File arquivo = new File(ARQUIVO_USUARIOS);

        if (!arquivo.exists()) return usuarios;

        try (BufferedReader br = new BufferedReader(new FileReader(arquivo))) {
            String linha;
            while ((linha = br.readLine()) != null) {
                String[] partes = linha.split("\\|", 2);
                if (partes.length == 2) {
                    usuarios.put(partes[0], partes[1]);
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao carregar usuários cadastrados: " + e.getMessage());
        }
        return usuarios;
    }
}