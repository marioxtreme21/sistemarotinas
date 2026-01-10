package sistema.rotinas.primefaces.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sistema.rotinas.primefaces.dto.MgvTestResult;
import sistema.rotinas.primefaces.dto.SftpDownloadInfo;
import sistema.rotinas.primefaces.model.ArquivosMgv;
import sistema.rotinas.primefaces.model.ArquivosMgvPattern;
import sistema.rotinas.primefaces.model.Loja;
import sistema.rotinas.primefaces.model.LojaRemoteConfig;
import sistema.rotinas.primefaces.service.interfaces.IArquivosMgvPatternService;
import sistema.rotinas.primefaces.service.interfaces.IArquivosMgvService;
import sistema.rotinas.primefaces.util.PastaUploadUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class MgvTransferService {

    /**
     * ✅ Logger dedicado da Rotina MGV (para appender próprio no logback)
     */
    private static final Logger LOG = LoggerFactory.getLogger("ROTINA_MGV");

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @Value("${mgv.retention-days:7}")
    private int retentionDays;

    @Autowired private SftpDownloadService sftpDownloadService;
    @Autowired private FsCopyService fsCopyService;
    @Autowired private SmbCopyService smbCopyService;

    @Autowired private IArquivosMgvPatternService patternService;
    @Autowired private IArquivosMgvService arquivosMgvService;

    /**
     * ✅ Assinatura principal: testar por ID
     * ✅ Importante para Scheduler: mantém sessão aberta para resolver lazy (Loja/RemoteConfig)
     */
    @Transactional(readOnly = true)
    public MgvTestResult testar(Long mgvId) {
        if (mgvId == null) {
            throw new IllegalArgumentException("Informe o ID do cadastro MGV para testar.");
        }

        ArquivosMgv cfg = arquivosMgvService.findById(mgvId);
        if (cfg == null) {
            throw new IllegalArgumentException("Cadastro MGV não encontrado (id=" + mgvId + ").");
        }

        return testar(cfg);
    }

    /**
     * ✅ Baixa do SFTP para:
     * /uploads/rotinaalterados/mgv/LJ{cod}/YYYY-MM-DD/
     * e copia para SMB/FS conforme tipoDestino (ou conforme configurado).
     *
     * ✅ MGV: múltiplos arquivos (1 por pattern).
     * ✅ NÃO renomeia (sem _2, _3). Sobrescreve normal.
     */
    @Transactional(readOnly = true)
    public MgvTestResult testar(ArquivosMgv cfg) {

        long t0 = System.currentTimeMillis();
        MgvTestResult r = new MgvTestResult();

        if (cfg == null) throw new IllegalArgumentException("Config MGV não informada.");
        if (cfg.getMgvId() == null) throw new IllegalArgumentException("Salve a configuração MGV antes de testar.");
        if (cfg.getRemoteConfig() == null) throw new IllegalArgumentException("Config Remota (SFTP) não informada.");
        if (cfg.getLoja() == null) throw new IllegalArgumentException("Loja não informada.");

        Loja loja = cfg.getLoja();
        String cod = loja.getCodLojaRms();
        if (cod == null || cod.isBlank()) throw new IllegalArgumentException("codLojaRms não informado.");

        ZoneId zone = resolveZone(cfg);
        LocalDate hoje = LocalDate.now(zone);

        LojaRemoteConfig rc = cfg.getRemoteConfig();
        String remoteDir = juntarRemoto(rc.getBaseDirRemoto(), cfg.getSubpastaRemota());

        LOG.info("MGV transfer start | mgvId={} lojaId={} codLojaRms={} remoteHost={} remoteDir={} hoje={} tz={}",
                cfg.getMgvId(),
                safeLojaId(loja),
                cod,
                safeHost(rc),
                remoteDir,
                hoje,
                zone.getId()
        );

        // retenção (não derruba)
        try {
            PastaUploadUtil.limparMgvLojaPorRetencao(cod, retentionDays);
            LOG.debug("Retenção aplicada | codLojaRms={} retentionDays={}", cod, retentionDays);
        } catch (Exception e) {
            LOG.warn("Falha ao aplicar retenção (ignorada) | codLojaRms={} msg={}", cod, e.getMessage(), e);
            r.addMsg("⚠ Falha ao aplicar retenção: " + e.getMessage());
        }

        // pasta do dia
        Path pastaDia;
        try {
            pastaDia = PastaUploadUtil.pastaMgvLojaDia(cod, hoje);
            LOG.debug("Pasta do dia | codLojaRms={} pastaDia={}", cod, pastaDia);
        } catch (Exception e) {
            LOG.error("Falha ao preparar pasta do dia | codLojaRms={} msg={}", cod, e.getMessage(), e);
            r.addMsg("FALHA: não consegui preparar pasta do dia: " + e.getMessage());
            r.setSftpOk(false);
            r.setDownloadOk(false);
            return r;
        }

        // patterns
        List<ArquivosMgvPattern> pats = patternService.listarPorMgv(cfg.getMgvId());
        if (pats == null || pats.isEmpty()) {
            throw new IllegalArgumentException("Nenhum pattern cadastrado para essa loja (MGV).");
        }
        List<String> patterns = pats.stream().map(ArquivosMgvPattern::getPattern).toList();

        LOG.info("Patterns carregados | mgvId={} codLojaRms={} total={}", cfg.getMgvId(), cod, patterns.size());
        LOG.debug("Patterns detalhe | mgvId={} codLojaRms={} patterns={}", cfg.getMgvId(), cod, patterns);

        // =========================
        // 1) DOWNLOAD SFTP (1 por pattern)
        // =========================
        List<String> arquivosRemotos;
        List<Path> arquivosLocais;
        List<SftpDownloadInfo> infosDownload;

        try {
            DownloadBatch batch = baixarBatchSftp(rc, remoteDir, patterns, pastaDia);

            arquivosRemotos = batch.arquivosRemotos;
            arquivosLocais = batch.arquivosLocais;
            infosDownload = batch.infos;

            r.setArquivosRemotos(arquivosRemotos);
            r.setArquivosLocais(arquivosLocais);

            r.setSftpOk(true);
            r.setDownloadOk(true);

            r.addMsg("Download OK: " + (arquivosRemotos != null ? arquivosRemotos.size() : 0) + " arquivo(s)");

            LOG.info("Download OK | mgvId={} codLojaRms={} totalArquivos={}",
                    cfg.getMgvId(), cod, (arquivosRemotos != null ? arquivosRemotos.size() : 0));

            if (arquivosLocais == null || arquivosLocais.isEmpty()) {
                r.setDownloadOk(false);
                r.addMsg("⚠ Download retornou OK mas não trouxe arquivos locais.");
                LOG.warn("Sem arquivos locais após download | mgvId={} codLojaRms={}", cfg.getMgvId(), cod);
                return r;
            }

            boolean algumInexistente = arquivosLocais.stream().anyMatch(p -> p == null || !Files.exists(p));
            if (algumInexistente) {
                r.setDownloadOk(false);
                r.addMsg("⚠ Alguns arquivos baixados não foram encontrados localmente (ver logs).");
                LOG.warn("Alguns arquivos locais não encontrados após download | mgvId={} codLojaRms={}", cfg.getMgvId(), cod);
            }

        } catch (Exception e) {
            r.setSftpOk(false);
            r.setDownloadOk(false);

            r.addMsg("FALHA no download SFTP: " + e.getMessage());

            LOG.error("Falha no download SFTP | mgvId={} codLojaRms={} remoteHost={} remoteDir={} msg={}",
                    cfg.getMgvId(), cod, safeHost(rc), remoteDir, e.getMessage(), e);

            return r;
        }

        // =========================
        // 2) lastModified + valida data (por arquivo)
        // =========================
        try {
            if (infosDownload != null && !infosDownload.isEmpty()) {

                // ✅ usa mtime vindo do download (sem reconectar)
                for (SftpDownloadInfo inf : infosDownload) {
                    if (inf == null || inf.nomeArquivo() == null) continue;

                    LocalDateTime lastMod = null;
                    if (inf.mtimeEpochSeconds() != null) {
                        lastMod = LocalDateTime.ofInstant(Instant.ofEpochSecond(inf.mtimeEpochSeconds()), zone);
                    }

                    if (lastMod != null) {
                        boolean okData = lastMod.toLocalDate().isEqual(hoje);
                        r.addArquivoInfo(inf.nomeArquivo(), lastMod, okData);

                        String fmt = lastMod.format(FMT);
                        if (okData) {
                            r.addMsg("Data OK: " + inf.nomeArquivo() + " | " + fmt);
                            LOG.info("Data OK | mgvId={} codLojaRms={} arquivo={} lastModified={}",
                                    cfg.getMgvId(), cod, inf.nomeArquivo(), fmt);
                        } else {
                            r.addMsg("⚠ DESATUALIZADO: " + inf.nomeArquivo() + " | " + fmt + " | Execução: " + hoje);
                            LOG.warn("Arquivo desatualizado | mgvId={} codLojaRms={} arquivo={} lastModified={} execucao={}",
                                    cfg.getMgvId(), cod, inf.nomeArquivo(), fmt, hoje);
                        }
                    } else {
                        // fallback local (sem derrubar)
                        Path local = (arquivosLocais != null ? arquivosLocais.stream()
                                .filter(p -> p != null && p.getFileName() != null && p.getFileName().toString().equalsIgnoreCase(inf.nomeArquivo()))
                                .findFirst().orElse(null) : null);

                        LocalDateTime lmLocal = tentarObterLastModifiedLocal(local, zone);
                        if (lmLocal != null) {
                            boolean okData = lmLocal.toLocalDate().isEqual(hoje);
                            r.addArquivoInfo(inf.nomeArquivo(), lmLocal, okData);
                            r.addMsg((okData ? "Data OK (local): " : "⚠ DESATUALIZADO (local): ") + inf.nomeArquivo() + " | " + lmLocal.format(FMT));
                        } else {
                            r.addArquivoInfo(inf.nomeArquivo(), null, null);
                            r.addMsg("⚠ Sem lastModified: " + inf.nomeArquivo());
                            LOG.warn("Sem lastModified p/ arquivo | mgvId={} codLojaRms={} arquivo={}",
                                    cfg.getMgvId(), cod, inf.nomeArquivo());
                        }
                    }
                }

            } else if (arquivosRemotos != null && !arquivosRemotos.isEmpty()) {

                // fallback antigo (mantido): tenta remoto/local (pode reconectar dependendo do método)
                for (int i = 0; i < arquivosRemotos.size(); i++) {
                    String nomeRemoto = arquivosRemotos.get(i);
                    Path local = (arquivosLocais != null && i < arquivosLocais.size() ? arquivosLocais.get(i) : null);

                    LocalDateTime lastMod = tentarObterLastModifiedRemoto(rc, remoteDir, nomeRemoto, zone);
                    if (lastMod == null) {
                        lastMod = tentarObterLastModifiedLocal(local, zone);
                    }

                    if (lastMod != null) {
                        boolean okData = lastMod.toLocalDate().isEqual(hoje);
                        r.addArquivoInfo(nomeRemoto, lastMod, okData);

                        String fmt = lastMod.format(FMT);
                        if (okData) {
                            r.addMsg("Data OK: " + nomeRemoto + " | " + fmt);
                            LOG.info("Data OK | mgvId={} codLojaRms={} arquivo={} lastModified={}",
                                    cfg.getMgvId(), cod, nomeRemoto, fmt);
                        } else {
                            r.addMsg("⚠ DESATUALIZADO: " + nomeRemoto + " | " + fmt + " | Execução: " + hoje);
                            LOG.warn("Arquivo desatualizado | mgvId={} codLojaRms={} arquivo={} lastModified={} execucao={}",
                                    cfg.getMgvId(), cod, nomeRemoto, fmt, hoje);
                        }
                    } else {
                        r.addMsg("⚠ Sem lastModified: " + nomeRemoto);
                        LOG.warn("Sem lastModified p/ arquivo | mgvId={} codLojaRms={} arquivo={}",
                                cfg.getMgvId(), cod, nomeRemoto);
                    }
                }

            } else {
                r.addMsg("⚠ Sem arquivos para validar lastModified.");
            }

        } catch (Exception e) {
            r.addMsg("⚠ Falha ao validar lastModified: " + e.getMessage());
            LOG.warn("Falha ao validar lastModified (ignorada) | mgvId={} codLojaRms={} msg={}",
                    cfg.getMgvId(), cod, e.getMessage(), e);
        }

        // =========================
        // 3) FS copy (se configurado) - copia TODOS
        // =========================
        if (cfg.getCaminhoFsDestino() != null && !cfg.getCaminhoFsDestino().isBlank()) {
            try {
                LOG.info("FS copy start | mgvId={} codLojaRms={} destinoFs={} totalArquivos={}",
                        cfg.getMgvId(), cod, cfg.getCaminhoFsDestino(), safeSize(arquivosLocais));

                int copiados = 0;
                if (arquivosLocais != null) {
                    for (Path p : arquivosLocais) {
                        if (p == null || !Files.exists(p)) continue;
                        fsCopyService.copiarParaFs(p, cfg.getCaminhoFsDestino());
                        copiados++;
                    }
                }

                r.setFsOk(true);
                r.addMsg("FS OK: " + cfg.getCaminhoFsDestino() + " | copiados=" + copiados);

                LOG.info("FS OK | mgvId={} codLojaRms={} destinoFs={} copiados={}",
                        cfg.getMgvId(), cod, cfg.getCaminhoFsDestino(), copiados);

            } catch (Exception e) {
                r.setFsOk(false);
                r.addMsg("FS FALHOU: " + e.getMessage());

                LOG.warn("FS FALHOU | mgvId={} codLojaRms={} destinoFs={} msg={}",
                        cfg.getMgvId(), cod, cfg.getCaminhoFsDestino(), e.getMessage(), e);
            }
        } else {
            r.addMsg("FS: não configurado (pulado).");
            LOG.debug("FS pulado (não configurado) | mgvId={} codLojaRms={}", cfg.getMgvId(), cod);
        }

        // =========================
        // 4) SMB copy (se configurado) - copia TODOS
        // =========================
        boolean smbTemMinimo =
                cfg.getSmbServidor() != null && !cfg.getSmbServidor().isBlank() &&
                cfg.getSmbCompartilhamento() != null && !cfg.getSmbCompartilhamento().isBlank() &&
                cfg.getSmbUsuario() != null && !cfg.getSmbUsuario().isBlank();

        if (smbTemMinimo) {
            try {
                String share = cfg.getSmbServidor() + "\\" + cfg.getSmbCompartilhamento();

                LOG.info("SMB copy start | mgvId={} codLojaRms={} share={} subpasta={} totalArquivos={}",
                        cfg.getMgvId(), cod, share, nz(cfg.getSmbSubpasta()), safeSize(arquivosLocais));

                int copiados = 0;
                if (arquivosLocais != null) {
                    for (Path p : arquivosLocais) {
                        if (p == null || !Files.exists(p)) continue;
                        smbCopyService.copiarParaSmb(
                                p,
                                cfg.getSmbServidor(),
                                cfg.getSmbCompartilhamento(),
                                cfg.getSmbSubpasta(),
                                cfg.getSmbDominio(),
                                cfg.getSmbUsuario(),
                                cfg.getSmbSenha()
                        );
                        copiados++;
                    }
                }

                r.setSmbOk(true);
                r.addMsg("SMB OK: " + share + " | copiados=" + copiados);

                LOG.info("SMB OK | mgvId={} codLojaRms={} share={} copiados={}",
                        cfg.getMgvId(), cod, share, copiados);

            } catch (Exception e) {
                r.setSmbOk(false);
                r.addMsg("SMB FALHOU: " + e.getMessage());

                LOG.warn("SMB FALHOU | mgvId={} codLojaRms={} servidor={} share={} msg={}",
                        cfg.getMgvId(),
                        cod,
                        nz(cfg.getSmbServidor()),
                        nz(cfg.getSmbCompartilhamento()),
                        e.getMessage(),
                        e);
            }
        } else {
            r.addMsg("SMB: não configurado (pulado).");
            LOG.debug("SMB pulado (não configurado) | mgvId={} codLojaRms={}", cfg.getMgvId(), cod);
        }

        long elapsed = System.currentTimeMillis() - t0;
        LOG.info("MGV transfer end | mgvId={} codLojaRms={} sftpOk={} downloadOk={} fsOk={} smbOk={} tempoMs={}",
                cfg.getMgvId(),
                cod,
                r.isSftpOk(),
                r.isDownloadOk(),
                r.isFsOk(),
                r.isSmbOk(),
                elapsed
        );

        r.addMsg("Tempo total: " + fmtDuracaoMs(elapsed));
        return r;
    }

    // =========================
    // ✅ Aliases de compatibilidade
    // =========================
    public MgvTestResult testarTransfer(Long mgvId) { return testar(mgvId); }
    public MgvTestResult testarTransfer(ArquivosMgv cfg) { return testar(cfg); }
    public MgvTestResult testarTransferencia(Long mgvId) { return testar(mgvId); }
    public MgvTestResult testarTransferencia(ArquivosMgv cfg) { return testar(cfg); }

    // =========================================================
    // ✅ Helpers: download batch, lastModified remoto/local e timezone
    // =========================================================

    private static class DownloadBatch {
        final List<String> arquivosRemotos;
        final List<Path> arquivosLocais;
        final List<SftpDownloadInfo> infos;
        DownloadBatch(List<String> arquivosRemotos, List<Path> arquivosLocais, List<SftpDownloadInfo> infos) {
            this.arquivosRemotos = arquivosRemotos;
            this.arquivosLocais = arquivosLocais;
            this.infos = infos;
        }
    }

    /**
     * ✅ AJUSTE:
     * - Tenta o batch novo (1 conexão por loja):
     *   sftpDownloadService.baixarArquivosMaisRecentesPorPattern(...)
     * - Se falhar por qualquer motivo, cai no seu fallback antigo (1 conexão por pattern).
     */
    private DownloadBatch baixarBatchSftp(LojaRemoteConfig rc, String remoteDir, List<String> patterns, Path pastaDia) throws Exception {

        // 1) ✅ batch novo (1 conexão por loja)
        try {
            List<SftpDownloadInfo> infos = sftpDownloadService.baixarArquivosMaisRecentesPorPattern(rc, remoteDir, patterns, pastaDia);
            List<String> nomes = infos.stream().map(SftpDownloadInfo::nomeArquivo).toList();
            List<Path> locais = nomes.stream().map(pastaDia::resolve).toList();
            return new DownloadBatch(nomes, locais, infos);
        } catch (Exception e) {
            LOG.warn("Batch novo falhou; usando fallback por pattern (1 conexão por pattern). msg={}", e.getMessage());
        }

        // 2) fallback antigo: 1 por pattern (SEM renomear)
        List<String> remotos = new ArrayList<>();
        List<Path> locais = new ArrayList<>();
        List<SftpDownloadInfo> infosOut = new ArrayList<>();

        int idx = 0;
        for (String pattern : patterns) {
            idx++;

            // usa o método novo (info), mas conexão é por pattern nesse fallback
            SftpDownloadInfo info = sftpDownloadService.baixarArquivoMaisRecenteQueCaseInfo(
                    rc, remoteDir, List.of(pattern), pastaDia
            );

            String nomeRemoto = (info != null ? info.nomeArquivo() : null);
            if (nomeRemoto == null) continue;

            Path local = pastaDia.resolve(nomeRemoto);

            remotos.add(nomeRemoto);
            locais.add(local);
            infosOut.add(info);

            LOG.debug("Fallback download por pattern | {} / {} | pattern={} remoto={} local={}",
                    idx, patterns.size(), pattern, nomeRemoto, local);
        }

        return new DownloadBatch(remotos, locais, infosOut);
    }

    private ZoneId resolveZone(ArquivosMgv cfg) {
        try {
            String tz = (cfg != null ? cfg.getTimezone() : null);
            if (tz != null && !tz.isBlank()) {
                return ZoneId.of(tz.trim());
            }
        } catch (Exception ignore) { }
        return ZoneId.systemDefault();
    }

    private LocalDateTime tentarObterLastModifiedLocal(Path arquivoLocal, ZoneId zone) {
        try {
            if (arquivoLocal == null || !Files.exists(arquivoLocal)) return null;
            FileTime ft = Files.getLastModifiedTime(arquivoLocal);
            if (ft == null) return null;
            return LocalDateTime.ofInstant(ft.toInstant(), zone);
        } catch (Exception e) {
            LOG.debug("Falha ao ler lastModified local | msg={}", e.getMessage());
            return null;
        }
    }

    /**
     * Tenta obter lastModified do remoto via métodos opcionais no SftpDownloadService.
     * Se não existir método compatível, retorna null (e o fluxo faz fallback no local).
     */
    private LocalDateTime tentarObterLastModifiedRemoto(LojaRemoteConfig rc, String remoteDir, String nomeArquivo, ZoneId zone) {
        Object v = null;

        // tentativas (3 args)
        v = tryInvokeSftp("obterLastModifiedRemoto", new Class<?>[]{LojaRemoteConfig.class, String.class, String.class}, new Object[]{rc, remoteDir, nomeArquivo});
        if (v == null) v = tryInvokeSftp("getLastModifiedRemoto",   new Class<?>[]{LojaRemoteConfig.class, String.class, String.class}, new Object[]{rc, remoteDir, nomeArquivo});
        if (v == null) v = tryInvokeSftp("remoteLastModified",      new Class<?>[]{LojaRemoteConfig.class, String.class, String.class}, new Object[]{rc, remoteDir, nomeArquivo});

        // tentativas (2 args) com fullPath
        String full = (remoteDir != null ? remoteDir : "") + (remoteDir != null && remoteDir.endsWith("/") ? "" : "/") + nomeArquivo;
        if (v == null) v = tryInvokeSftp("obterLastModifiedRemoto", new Class<?>[]{LojaRemoteConfig.class, String.class}, new Object[]{rc, full});
        if (v == null) v = tryInvokeSftp("getLastModifiedRemoto",   new Class<?>[]{LojaRemoteConfig.class, String.class}, new Object[]{rc, full});
        if (v == null) v = tryInvokeSftp("statLastModified",        new Class<?>[]{LojaRemoteConfig.class, String.class}, new Object[]{rc, full});

        LocalDateTime out = converterParaLocalDateTime(v, zone);
        if (out != null) {
            LOG.debug("LastModified remoto obtido | remoteHost={} remoteFile={} lastModified={}",
                    safeHost(rc), full, out.format(FMT));
        }
        return out;
    }

    private Object tryInvokeSftp(String method, Class<?>[] paramTypes, Object[] args) {
        try {
            var m = sftpDownloadService.getClass().getMethod(method, paramTypes);
            return m.invoke(sftpDownloadService, args);
        } catch (NoSuchMethodException nsme) {
            return null;
        } catch (Exception e) {
            LOG.debug("Falha ao chamar {} | msg={}", method, e.getMessage());
            return null;
        }
    }

    private LocalDateTime converterParaLocalDateTime(Object v, ZoneId zone) {
        if (v == null) return null;

        try {
            if (v instanceof LocalDateTime ldt) return ldt;
            if (v instanceof Date d) return LocalDateTime.ofInstant(d.toInstant(), zone);
            if (v instanceof Instant i) return LocalDateTime.ofInstant(i, zone);
            if (v instanceof Long epochMillis) return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), zone);
        } catch (Exception ignore) { }

        return null;
    }

    private String juntarRemoto(String base, String sub) {
        String b = (base == null ? "" : base.trim());
        String s = (sub == null ? "" : sub.trim());

        if (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        if (s.startsWith("/")) s = s.substring(1);

        if (b.isEmpty()) return "/" + s;
        if (s.isEmpty()) return b;
        return b + "/" + s;
    }

    // =========================
    // Helpers pequenos p/ log
    // =========================
    private static String nz(String v) {
        return (v == null || v.isBlank()) ? "-" : v;
    }

    private static int safeSize(List<?> v) {
        return v == null ? 0 : v.size();
    }

    private static Object safeLojaId(Loja l) {
        try { return (l != null ? l.getLojaId() : null); } catch (Exception e) { return null; }
    }

    private static String safeHost(LojaRemoteConfig rc) {
        try { return (rc != null ? rc.getHostRemoto() : null); } catch (Exception e) { return null; }
    }

    private static String fmtDuracaoMs(long ms) {
        if (ms < 0) ms = 0;
        Duration d = Duration.ofMillis(ms);
        long h = d.toHours();
        int m = d.toMinutesPart();
        int s = d.toSecondsPart();
        return String.format("%02d:%02d:%02d", h, m, s);
    }
}