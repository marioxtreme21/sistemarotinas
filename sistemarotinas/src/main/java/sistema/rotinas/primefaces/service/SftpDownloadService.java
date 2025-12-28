package sistema.rotinas.primefaces.service;

import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import sistema.rotinas.primefaces.model.LojaRemoteConfig;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;

@Service
public class SftpDownloadService {

    private static final Logger log = LoggerFactory.getLogger(SftpDownloadService.class);

    public String baixarArquivoMaisRecenteQueCase(
            LojaRemoteConfig cfg,
            String remoteDir,
            List<String> patterns,
            Path destinoLocal
    ) {
        if (cfg == null) throw new IllegalArgumentException("Config remota é obrigatória.");
        if (cfg.getProtocolo() != LojaRemoteConfig.Protocolo.SFTP) {
            throw new IllegalArgumentException("Config remota não é SFTP.");
        }
        if (remoteDir == null || remoteDir.isBlank()) throw new IllegalArgumentException("remoteDir é obrigatório.");
        if (patterns == null || patterns.isEmpty()) throw new IllegalArgumentException("Informe ao menos 1 pattern.");
        if (destinoLocal == null) throw new IllegalArgumentException("destinoLocal é obrigatório.");

        Session session = null;
        ChannelSftp sftp = null;

        String host = cfg.getHostRemoto();
        Integer porta = (cfg.getPortaRemota() != null ? cfg.getPortaRemota() : 22);
        String usuario = cfg.getUsuarioRemoto();

        long ini = System.currentTimeMillis();

        try {
            log.info("[SFTP-DOWNLOAD] Início - host={} porta={} usuario={} remoteDir={} destinoLocal={}",
                    host, porta, usuario, remoteDir, destinoLocal);

            JSch jsch = new JSch();

            if (cfg.getCaminhoChavePrivada() != null && !cfg.getCaminhoChavePrivada().isBlank()) {
                String keyPath = cfg.getCaminhoChavePrivada().trim();
                log.info("[SFTP-DOWNLOAD] Usando chave privada - path={}", keyPath);
                jsch.addIdentity(keyPath);
            }

            session = jsch.getSession(usuario, host, porta);
            session.setConfig("StrictHostKeyChecking", "no");

            if (cfg.getSenhaRemota() != null && !cfg.getSenhaRemota().isBlank()) {
                session.setPassword(cfg.getSenhaRemota());
                log.info("[SFTP-DOWNLOAD] Autenticação por senha habilitada (senha não exibida)");
            } else {
                log.info("[SFTP-DOWNLOAD] Autenticação sem senha (provável chave)");
            }

            int timeout = (cfg.getConnectTimeoutMs() != null ? cfg.getConnectTimeoutMs() : 15000);
            session.connect(timeout);

            Channel channel = session.openChannel("sftp");
            channel.connect(timeout);
            sftp = (ChannelSftp) channel;

            log.info("[SFTP-DOWNLOAD] Conectado. Listando diretório remoto {}", remoteDir);

            @SuppressWarnings("unchecked")
            Vector<ChannelSftp.LsEntry> ls = sftp.ls(remoteDir);

            ChannelSftp.LsEntry escolhido = escolherEntryMaisRecente(ls, patterns);
            if (escolhido == null) {
                throw new IllegalArgumentException("Nenhum arquivo no remoto casa com os patterns informados.");
            }

            String nomeArquivo = escolhido.getFilename();
            int mtimeSegundos = (escolhido.getAttrs() != null ? escolhido.getAttrs().getMTime() : 0);
            Instant remoteInstant = (mtimeSegundos > 0 ? Instant.ofEpochSecond(mtimeSegundos) : null);

            log.info("[SFTP-DOWNLOAD] Arquivo escolhido={} mtimeRemoto={}", nomeArquivo, remoteInstant);

            Files.createDirectories(destinoLocal);

            Path localFile = destinoLocal.resolve(nomeArquivo);

            String remotePath = remoteDir.endsWith("/") ? (remoteDir + nomeArquivo) : (remoteDir + "/" + nomeArquivo);

            log.info("[SFTP-DOWNLOAD] Baixando {} -> {}", remotePath, localFile);

            try (OutputStream os = Files.newOutputStream(localFile)) {
                sftp.get(remotePath, os);
            }

            // ✅ Preservar data/hora do arquivo remoto no arquivo local (lastModified)
            if (mtimeSegundos > 0) {
                FileTime ft = FileTime.from(Instant.ofEpochSecond(mtimeSegundos));
                Files.setLastModifiedTime(localFile, ft);
                log.info("[SFTP-DOWNLOAD] LastModified preservado no arquivo local - {}", ft);
            } else {
                log.warn("[SFTP-DOWNLOAD] Não foi possível obter mtime remoto (attrs.getMTime veio 0). Mantendo timestamp do download.");
            }

            long ms = System.currentTimeMillis() - ini;
            log.info("[SFTP-DOWNLOAD] Fim OK - arquivo={} tempoTotal={} ms", nomeArquivo, ms);

            return nomeArquivo;

        } catch (Exception e) {
            long ms = System.currentTimeMillis() - ini;
            log.error("[SFTP-DOWNLOAD] Falha - tempoTotal={} ms - msg={}", ms, e.getMessage(), e);
            throw new RuntimeException("Falha ao baixar via SFTP: " + e.getMessage(), e);
        } finally {
            if (sftp != null) try { sftp.disconnect(); } catch (Exception ignore) {}
            if (session != null) try { session.disconnect(); } catch (Exception ignore) {}
        }
    }

    /**
     * Escolhe o arquivo mais recente (maior mtime) dentre os que casam com os patterns.
     */
    private ChannelSftp.LsEntry escolherEntryMaisRecente(Vector<ChannelSftp.LsEntry> ls, List<String> patterns) {
        if (ls == null) return null;

        List<Pattern> regs = patterns.stream()
                .filter(p -> p != null && !p.isBlank())
                .map(this::globToRegex)
                .map(Pattern::compile)
                .toList();

        return ls.stream()
                .filter(e -> e != null && e.getFilename() != null)
                .filter(e -> e.getAttrs() != null && !e.getAttrs().isDir())
                .filter(e -> regs.stream().anyMatch(r -> r.matcher(e.getFilename()).matches()))
                .max(Comparator
                        .comparingInt((ChannelSftp.LsEntry e) -> e.getAttrs().getMTime())
                        .thenComparing(ChannelSftp.LsEntry::getFilename, String.CASE_INSENSITIVE_ORDER)
                )
                .orElse(null);
    }

    private String globToRegex(String glob) {
        // bem simples: * -> .*, ? -> .
        String g = glob.trim();
        StringBuilder sb = new StringBuilder("^");
        for (int i = 0; i < g.length(); i++) {
            char c = g.charAt(i);
            switch (c) {
                case '*': sb.append(".*"); break;
                case '?': sb.append("."); break;
                case '.': sb.append("\\."); break;
                case '\\': sb.append("\\\\"); break;
                default:
                    if ("+()^$|{}[]".indexOf(c) >= 0) sb.append("\\");
                    sb.append(c);
            }
        }
        sb.append("$");
        return sb.toString();
    }
}
