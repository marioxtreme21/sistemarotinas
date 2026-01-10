package sistema.rotinas.primefaces.service;

import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import sistema.rotinas.primefaces.dto.SftpDownloadInfo;
import sistema.rotinas.primefaces.model.LojaRemoteConfig;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;

@Service
public class SftpDownloadService {

    /**
     * ✅ Logger único da rotina PRICE (vai para o appender/arquivo da Rotina PRICE)
     */
    private static final Logger LOG = LoggerFactory.getLogger("ROTINA_PRICE");

    // =========================================================
    // ✅ MANTIDO: assinatura antiga (retorna apenas String)
    // =========================================================
    public String baixarArquivoMaisRecenteQueCase(
            LojaRemoteConfig cfg,
            String remoteDir,
            List<String> patterns,
            Path destinoLocal
    ) {
        SftpDownloadInfo info = baixarArquivoMaisRecenteQueCaseInfo(cfg, remoteDir, patterns, destinoLocal);
        return info != null ? info.nomeArquivo() : null;
    }

    // =========================================================
    // ✅ NOVO: assinatura principal (retorna nome + mtime remoto)
    // =========================================================
    public SftpDownloadInfo baixarArquivoMaisRecenteQueCaseInfo(
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
            LOG.info("[SFTP] Início download | host={} porta={} usuario={} remoteDir={} destinoLocal={}",
                    host, porta, usuario, remoteDir, destinoLocal);

            JSch jsch = new JSch();

            if (cfg.getCaminhoChavePrivada() != null && !cfg.getCaminhoChavePrivada().isBlank()) {
                String keyPath = cfg.getCaminhoChavePrivada().trim();
                LOG.info("[SFTP] Usando chave privada | path={}", keyPath);
                jsch.addIdentity(keyPath);
            }

            session = jsch.getSession(usuario, host, porta);
            session.setConfig("StrictHostKeyChecking", "no");

            if (cfg.getSenhaRemota() != null && !cfg.getSenhaRemota().isBlank()) {
                session.setPassword(cfg.getSenhaRemota());
                LOG.info("[SFTP] Autenticação por senha habilitada (senha não exibida)");
            } else {
                LOG.info("[SFTP] Autenticação sem senha (provável chave)");
            }

            int timeout = (cfg.getConnectTimeoutMs() != null ? cfg.getConnectTimeoutMs() : 15000);
            session.connect(timeout);

            Channel channel = session.openChannel("sftp");
            channel.connect(timeout);
            sftp = (ChannelSftp) channel;

            LOG.info("[SFTP] Conectado. Listando diretório remoto {}", remoteDir);

            @SuppressWarnings("unchecked")
            Vector<ChannelSftp.LsEntry> ls = sftp.ls(remoteDir);

            ChannelSftp.LsEntry escolhido = escolherEntryMaisRecente(ls, patterns);
            if (escolhido == null) {
                throw new IllegalArgumentException("Nenhum arquivo no remoto casa com os patterns informados.");
            }

            String nomeArquivo = escolhido.getFilename();
            int mtimeSegundos = (escolhido.getAttrs() != null ? escolhido.getAttrs().getMTime() : 0);
            Instant remoteInstant = (mtimeSegundos > 0 ? Instant.ofEpochSecond(mtimeSegundos) : null);

            LOG.info("[SFTP] Arquivo escolhido={} mtimeRemoto={}", nomeArquivo, remoteInstant);

            Files.createDirectories(destinoLocal);

            Path localFile = destinoLocal.resolve(nomeArquivo);
            String remotePath = remoteDir.endsWith("/") ? (remoteDir + nomeArquivo) : (remoteDir + "/" + nomeArquivo);

            LOG.info("[SFTP] Baixando {} -> {}", remotePath, localFile);

            try (OutputStream os = Files.newOutputStream(localFile)) {
                sftp.get(remotePath, os);
            }

            // ✅ Preserva lastModified remoto no arquivo local
            if (mtimeSegundos > 0) {
                FileTime ft = FileTime.from(Instant.ofEpochSecond(mtimeSegundos));
                Files.setLastModifiedTime(localFile, ft);
                LOG.info("[SFTP] LastModified preservado no arquivo local | {}", ft);
            } else {
                LOG.warn("[SFTP] mtime remoto veio 0 (attrs.getMTime). Mantendo timestamp do download.");
            }

            long ms = System.currentTimeMillis() - ini;
            LOG.info("[SFTP] Fim OK | arquivo={} tempoTotalMs={}", nomeArquivo, ms);

            return new SftpDownloadInfo(
                    nomeArquivo,
                    (mtimeSegundos > 0 ? (long) mtimeSegundos : null)
            );

        } catch (Exception e) {
            long ms = System.currentTimeMillis() - ini;
            LOG.error("[SFTP] Falha | tempoTotalMs={} msg={}", ms, e.getMessage(), e);
            throw new RuntimeException("Falha ao baixar via SFTP: " + e.getMessage(), e);
        } finally {
            if (sftp != null) try { sftp.disconnect(); } catch (Exception ignore) {}
            if (session != null) try { session.disconnect(); } catch (Exception ignore) {}
        }
    }

    // =========================================================
    // ✅ NOVO: Batch para MGV (1 conexão por loja; 1 arquivo por pattern)
    // =========================================================
    public List<SftpDownloadInfo> baixarArquivosMaisRecentesPorPattern(
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
            LOG.info("[SFTP][BATCH] Início | host={} porta={} usuario={} remoteDir={} totalPatterns={} destinoLocal={}",
                    host, porta, usuario, remoteDir, patterns.size(), destinoLocal);

            JSch jsch = new JSch();

            if (cfg.getCaminhoChavePrivada() != null && !cfg.getCaminhoChavePrivada().isBlank()) {
                String keyPath = cfg.getCaminhoChavePrivada().trim();
                LOG.info("[SFTP][BATCH] Usando chave privada | path={}", keyPath);
                jsch.addIdentity(keyPath);
            }

            session = jsch.getSession(usuario, host, porta);
            session.setConfig("StrictHostKeyChecking", "no");

            if (cfg.getSenhaRemota() != null && !cfg.getSenhaRemota().isBlank()) {
                session.setPassword(cfg.getSenhaRemota());
                LOG.info("[SFTP][BATCH] Autenticação por senha habilitada (senha não exibida)");
            }

            int timeout = (cfg.getConnectTimeoutMs() != null ? cfg.getConnectTimeoutMs() : 15000);
            session.connect(timeout);

            Channel channel = session.openChannel("sftp");
            channel.connect(timeout);
            sftp = (ChannelSftp) channel;

            Files.createDirectories(destinoLocal);

            @SuppressWarnings("unchecked")
            Vector<ChannelSftp.LsEntry> ls = sftp.ls(remoteDir);

            List<SftpDownloadInfo> out = new ArrayList<>();

            int idx = 0;
            for (String pattern : patterns) {
                idx++;
                if (pattern == null || pattern.isBlank()) continue;

                ChannelSftp.LsEntry escolhido = escolherEntryMaisRecente(ls, List.of(pattern));
                if (escolhido == null) {
                    LOG.warn("[SFTP][BATCH] Nenhum arquivo casou com pattern={} em {}", pattern, remoteDir);
                    continue;
                }

                String nomeArquivo = escolhido.getFilename();
                int mtimeSegundos = (escolhido.getAttrs() != null ? escolhido.getAttrs().getMTime() : 0);

                Path localFile = destinoLocal.resolve(nomeArquivo);
                String remotePath = remoteDir.endsWith("/") ? (remoteDir + nomeArquivo) : (remoteDir + "/" + nomeArquivo);

                LOG.info("[SFTP][BATCH] ({}/{}) Baixando | pattern={} remoto={} -> {}",
                        idx, patterns.size(), pattern, remotePath, localFile);

                try (OutputStream os = Files.newOutputStream(localFile)) {
                    sftp.get(remotePath, os);
                }

                if (mtimeSegundos > 0) {
                    Files.setLastModifiedTime(localFile, FileTime.from(Instant.ofEpochSecond(mtimeSegundos)));
                }

                out.add(new SftpDownloadInfo(
                        nomeArquivo,
                        (mtimeSegundos > 0 ? (long) mtimeSegundos : null)
                ));
            }

            long ms = System.currentTimeMillis() - ini;
            LOG.info("[SFTP][BATCH] Fim OK | totalBaixados={} tempoTotalMs={}", out.size(), ms);

            return out;

        } catch (Exception e) {
            long ms = System.currentTimeMillis() - ini;
            LOG.error("[SFTP][BATCH] Falha | tempoTotalMs={} msg={}", ms, e.getMessage(), e);
            throw new RuntimeException("Falha ao baixar batch via SFTP: " + e.getMessage(), e);
        } finally {
            if (sftp != null) try { sftp.disconnect(); } catch (Exception ignore) {}
            if (session != null) try { session.disconnect(); } catch (Exception ignore) {}
        }
    }

    /**
     * ✅ NOVO (opcional): retorna lastModified remoto em epochMillis (para o PriceTransferService consumir)
     * Assinatura com 3 args (cfg, remoteDir, nomeArquivo)
     */
    public Long getLastModifiedRemoto(LojaRemoteConfig cfg, String remoteDir, String nomeArquivo) {
        if (cfg == null) return null;
        if (remoteDir == null || remoteDir.isBlank()) return null;
        if (nomeArquivo == null || nomeArquivo.isBlank()) return null;

        String remotePath = remoteDir.endsWith("/") ? (remoteDir + nomeArquivo) : (remoteDir + "/" + nomeArquivo);
        return getLastModifiedRemoto(cfg, remotePath);
    }

    /**
     * ✅ NOVO (opcional): retorna lastModified remoto em epochMillis (cfg + remoteFullPath)
     */
    public Long getLastModifiedRemoto(LojaRemoteConfig cfg, String remoteFullPath) {
        if (cfg == null) return null;
        if (remoteFullPath == null || remoteFullPath.isBlank()) return null;

        Session session = null;
        ChannelSftp sftp = null;

        String host = cfg.getHostRemoto();
        Integer porta = (cfg.getPortaRemota() != null ? cfg.getPortaRemota() : 22);
        String usuario = cfg.getUsuarioRemoto();

        long ini = System.currentTimeMillis();

        try {
            JSch jsch = new JSch();
            if (cfg.getCaminhoChavePrivada() != null && !cfg.getCaminhoChavePrivada().isBlank()) {
                jsch.addIdentity(cfg.getCaminhoChavePrivada().trim());
            }

            session = jsch.getSession(usuario, host, porta);
            session.setConfig("StrictHostKeyChecking", "no");
            if (cfg.getSenhaRemota() != null && !cfg.getSenhaRemota().isBlank()) {
                session.setPassword(cfg.getSenhaRemota());
            }

            int timeout = (cfg.getConnectTimeoutMs() != null ? cfg.getConnectTimeoutMs() : 15000);
            session.connect(timeout);

            Channel channel = session.openChannel("sftp");
            channel.connect(timeout);
            sftp = (ChannelSftp) channel;

            SftpATTRS attrs = sftp.lstat(remoteFullPath);
            if (attrs == null) return null;

            int mtime = attrs.getMTime();
            if (mtime <= 0) return null;

            long epochMillis = Instant.ofEpochSecond(mtime).toEpochMilli();

            long ms = System.currentTimeMillis() - ini;
            LOG.debug("[SFTP] LastModified remoto obtido | remotePath={} epochMillis={} tempoTotalMs={}",
                    remoteFullPath, epochMillis, ms);

            return epochMillis;

        } catch (Exception e) {
            long ms = System.currentTimeMillis() - ini;
            LOG.debug("[SFTP] Falha ao obter lastModified remoto (ignorada) | remotePath={} tempoTotalMs={} msg={}",
                    remoteFullPath, ms, e.getMessage());
            return null;
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