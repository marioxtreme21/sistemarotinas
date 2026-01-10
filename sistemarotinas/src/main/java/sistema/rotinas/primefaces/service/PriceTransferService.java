package sistema.rotinas.primefaces.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sistema.rotinas.primefaces.dto.PriceTestResult;
import sistema.rotinas.primefaces.dto.SftpDownloadInfo;
import sistema.rotinas.primefaces.model.ArquivosPrice;
import sistema.rotinas.primefaces.model.ArquivosPricePattern;
import sistema.rotinas.primefaces.model.Loja;
import sistema.rotinas.primefaces.model.LojaRemoteConfig;
import sistema.rotinas.primefaces.service.interfaces.IArquivosPricePatternService;
import sistema.rotinas.primefaces.service.interfaces.IArquivosPriceService;
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
import java.util.Date;
import java.util.List;

@Service
public class PriceTransferService {

    /**
     * ✅ Logger dedicado da Rotina PRICE (para appender próprio no logback)
     */
    private static final Logger LOG = LoggerFactory.getLogger("ROTINA_PRICE");

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @Value("${price.retention-days:7}")
    private int retentionDays;

    @Autowired private SftpDownloadService sftpDownloadService;
    @Autowired private FsCopyService fsCopyService;
    @Autowired private SmbCopyService smbCopyService;

    @Autowired private IArquivosPricePatternService patternService;
    @Autowired private IArquivosPriceService arquivosPriceService;

    /**
     * ✅ Assinatura principal: testar por ID
     * ✅ Importante para Scheduler: mantém sessão aberta para resolver lazy (Loja/RemoteConfig)
     */
    @Transactional(readOnly = true)
    public PriceTestResult testar(Long priceId) {
        if (priceId == null) {
            throw new IllegalArgumentException("Informe o ID do cadastro PRICE para testar.");
        }

        ArquivosPrice cfg = arquivosPriceService.findById(priceId);
        if (cfg == null) {
            throw new IllegalArgumentException("Cadastro PRICE não encontrado (id=" + priceId + ").");
        }

        return testar(cfg);
    }

    /**
     * ✅ Baixa do SFTP para:
     * /uploads/rotinaalterados/price/LJ{cod}/YYYY-MM-DD/
     * e tenta copiar para SMB e FS (se estiverem preenchidos).
     *
     * ✅ Valida data do arquivo via lastModified (remoto preferencial; fallback local)
     * ✅ Copia MSG (*.m1) (se ativo/configurado) sem derrubar o principal
     *
     * ✅ IMPORTANTE: falhas em FS/SMB/SFTP não derrubam (retorna PriceTestResult com flags/mensagens)
     * - Só lança exception para inconsistências de cadastro (cfg/loja/remoteConfig/patterns vazios).
     *
     * ✅ Importante para Scheduler: mantém sessão aberta para resolver lazy (Loja/RemoteConfig)
     */
    @Transactional(readOnly = true)
    public PriceTestResult testar(ArquivosPrice cfg) {

        long t0 = System.currentTimeMillis();

        PriceTestResult r = new PriceTestResult();

        if (cfg == null) throw new IllegalArgumentException("Config PRICE não informada.");
        if (cfg.getPriceId() == null) throw new IllegalArgumentException("Salve a configuração PRICE antes de testar.");
        if (cfg.getRemoteConfig() == null) throw new IllegalArgumentException("Config Remota (SFTP) não informada.");
        if (cfg.getLoja() == null) throw new IllegalArgumentException("Loja não informada.");

        Loja loja = cfg.getLoja();
        String cod = loja.getCodLojaRms();
        if (cod == null || cod.isBlank()) throw new IllegalArgumentException("codLojaRms não informado.");

        ZoneId zone = resolveZone(cfg);
        LocalDate hoje = LocalDate.now(zone);

        LojaRemoteConfig rc = cfg.getRemoteConfig();
        String remoteDir = juntarRemoto(rc.getBaseDirRemoto(), cfg.getSubpastaRemota());

        LOG.info("PRICE transfer start | priceId={} lojaId={} codLojaRms={} remoteHost={} remoteDir={} hoje={} tz={}",
                cfg.getPriceId(),
                safeLojaId(loja),
                cod,
                safeHost(rc),
                remoteDir,
                hoje,
                zone.getId()
        );

        // retenção (7 dias por padrão) - não derruba
        try {
            PastaUploadUtil.limparPriceLojaPorRetencao(cod, retentionDays);
            LOG.debug("Retenção aplicada | codLojaRms={} retentionDays={}", cod, retentionDays);
        } catch (Exception e) {
            LOG.warn("Falha ao aplicar retenção (ignorada) | codLojaRms={} msg={}", cod, e.getMessage(), e);
            r.addMsg("⚠ Falha ao aplicar retenção: " + e.getMessage());
        }

        // pasta do dia
        Path pastaDia;
        try {
            pastaDia = PastaUploadUtil.pastaPriceLojaDia(cod, hoje);
            LOG.debug("Pasta do dia | codLojaRms={} pastaDia={}", cod, pastaDia);
        } catch (Exception e) {
            LOG.error("Falha ao preparar pasta do dia | codLojaRms={} msg={}", cod, e.getMessage(), e);
            r.addMsg("FALHA: não consegui preparar pasta do dia: " + e.getMessage());
            r.setSftpOk(false);
            r.setDownloadOk(false);
            return r;
        }

        // patterns
        List<ArquivosPricePattern> pats = patternService.listarPorPrice(cfg.getPriceId());
        if (pats == null || pats.isEmpty()) {
            throw new IllegalArgumentException("Nenhum pattern cadastrado para essa loja.");
        }
        List<String> patterns = pats.stream().map(ArquivosPricePattern::getPattern).toList();

        LOG.info("Patterns carregados | priceId={} codLojaRms={} total={}", cfg.getPriceId(), cod, patterns.size());
        LOG.debug("Patterns detalhe | priceId={} codLojaRms={} patterns={}", cfg.getPriceId(), cod, patterns);

        // =========================
        // 1) DOWNLOAD SFTP
        // =========================
        String arquivoRemoto;
        Path arquivoLocal;

        try {
            // ✅ NOVO: baixa e já traz o mtime remoto (sem precisar reconectar depois)
            SftpDownloadInfo info = sftpDownloadService.baixarArquivoMaisRecenteQueCaseInfo(rc, remoteDir, patterns, pastaDia);

            arquivoRemoto = (info != null ? info.nomeArquivo() : null);
            if (arquivoRemoto == null || arquivoRemoto.isBlank()) {
                throw new IllegalArgumentException("Download retornou sem nome de arquivo.");
            }

            r.setArquivoRemoto(arquivoRemoto);

            arquivoLocal = pastaDia.resolve(arquivoRemoto);
            r.setArquivoLocal(arquivoLocal);

            // ✅ se vier mtime do remoto, já preenche o DTO do resultado
            if (info.mtimeEpochSeconds() != null) {
                LocalDateTime lm = LocalDateTime.ofInstant(Instant.ofEpochSecond(info.mtimeEpochSeconds()), zone);
                r.setLastModifiedRemoto(lm);
            }

            r.setSftpOk(true);
            r.setDownloadOk(true);

            r.addMsg("Download OK: " + arquivoRemoto);

            LOG.info("Download OK | priceId={} codLojaRms={} arquivoRemoto={} arquivoLocal={}",
                    cfg.getPriceId(), cod, arquivoRemoto, arquivoLocal);

            if (!Files.exists(arquivoLocal)) {
                r.setDownloadOk(false);
                r.addMsg("⚠ Download retornou OK mas arquivo local não foi encontrado: " + arquivoLocal);
                LOG.warn("Arquivo local não encontrado após download | priceId={} codLojaRms={} arquivoLocal={}",
                        cfg.getPriceId(), cod, arquivoLocal);
                return r;
            }

        } catch (Exception e) {
            r.setSftpOk(false);
            r.setDownloadOk(false);

            r.addMsg("FALHA no download SFTP: " + e.getMessage());

            LOG.error("Falha no download SFTP | priceId={} codLojaRms={} remoteHost={} remoteDir={} msg={}",
                    cfg.getPriceId(), cod, safeHost(rc), remoteDir, e.getMessage(), e);

            return r; // sem download não faz sentido tentar FS/SMB
        }

        // =========================
        // 2) lastModified + valida data
        // =========================
        try {
            // ✅ primeiro tenta usar o que já veio do download (sem reconectar)
            LocalDateTime lastModRemoto = r.getLastModifiedRemoto();

            // se não veio do download, tenta via métodos opcionais (pode reconectar)
            if (lastModRemoto == null) {
                lastModRemoto = tentarObterLastModifiedRemoto(rc, remoteDir, arquivoRemoto, zone);
            }

            // fallback: se não conseguir remoto, tenta pegar do arquivo local baixado
            if (lastModRemoto == null) {
                lastModRemoto = tentarObterLastModifiedLocal(arquivoLocal, zone);
            }

            r.setLastModifiedRemoto(lastModRemoto);

            Boolean arquivoAtualizado = null;
            if (lastModRemoto != null) {
                arquivoAtualizado = lastModRemoto.toLocalDate().isEqual(hoje);
            }
            r.setArquivoAtualizado(arquivoAtualizado);

            if (arquivoAtualizado != null) {
                String fmt = lastModRemoto.format(FMT);
                if (arquivoAtualizado) {
                    r.addMsg("Data OK (lastModified): " + fmt);
                    LOG.info("Data OK | priceId={} codLojaRms={} lastModified={}", cfg.getPriceId(), cod, fmt);
                } else {
                    r.addMsg("⚠ Arquivo DESATUALIZADO (lastModified): " + fmt + " | Execução: " + hoje);
                    LOG.warn("Arquivo desatualizado | priceId={} codLojaRms={} lastModified={} execucao={}",
                            cfg.getPriceId(), cod, fmt, hoje);
                }
            } else {
                r.addMsg("⚠ Não foi possível obter lastModified remoto (nem local) para validar a data.");
                LOG.warn("Sem lastModified para validação | priceId={} codLojaRms={}", cfg.getPriceId(), cod);
            }

        } catch (Exception e) {
            r.addMsg("⚠ Falha ao validar lastModified: " + e.getMessage());
            LOG.warn("Falha ao validar lastModified (ignorada) | priceId={} codLojaRms={} msg={}",
                    cfg.getPriceId(), cod, e.getMessage(), e);
        }

        // =========================
        // 3) FS copy (se configurado)
        // =========================
        if (cfg.getCaminhoFsDestino() != null && !cfg.getCaminhoFsDestino().isBlank()) {
            try {
                LOG.info("FS copy start | priceId={} codLojaRms={} destinoFs={}",
                        cfg.getPriceId(), cod, cfg.getCaminhoFsDestino());

                fsCopyService.copiarParaFs(arquivoLocal, cfg.getCaminhoFsDestino());

                r.setFsOk(true);
                r.addMsg("FS OK: " + cfg.getCaminhoFsDestino());

                LOG.info("FS OK | priceId={} codLojaRms={} destinoFs={}",
                        cfg.getPriceId(), cod, cfg.getCaminhoFsDestino());

            } catch (Exception e) {
                r.setFsOk(false);
                r.addMsg("FS FALHOU: " + e.getMessage());

                LOG.warn("FS FALHOU | priceId={} codLojaRms={} destinoFs={} msg={}",
                        cfg.getPriceId(), cod, cfg.getCaminhoFsDestino(), e.getMessage(), e);
            }
        } else {
            r.addMsg("FS: não configurado (pulado).");
            LOG.debug("FS pulado (não configurado) | priceId={} codLojaRms={}", cfg.getPriceId(), cod);
        }

        // =========================
        // 4) SMB copy (se configurado)
        // =========================
        boolean smbTemMinimo =
                cfg.getSmbServidor() != null && !cfg.getSmbServidor().isBlank() &&
                cfg.getSmbCompartilhamento() != null && !cfg.getSmbCompartilhamento().isBlank() &&
                cfg.getSmbUsuario() != null && !cfg.getSmbUsuario().isBlank();

        if (smbTemMinimo) {
            try {
                String share = cfg.getSmbServidor() + "\\" + cfg.getSmbCompartilhamento();

                LOG.info("SMB copy start | priceId={} codLojaRms={} share={} subpasta={}",
                        cfg.getPriceId(), cod, share, nz(cfg.getSmbSubpasta()));

                smbCopyService.copiarParaSmb(
                        arquivoLocal,
                        cfg.getSmbServidor(),
                        cfg.getSmbCompartilhamento(),
                        cfg.getSmbSubpasta(),
                        cfg.getSmbDominio(),
                        cfg.getSmbUsuario(),
                        cfg.getSmbSenha()
                );

                r.setSmbOk(true);
                r.addMsg("SMB OK: " + share);

                LOG.info("SMB OK | priceId={} codLojaRms={} share={}", cfg.getPriceId(), cod, share);

            } catch (Exception e) {
                r.setSmbOk(false);
                r.addMsg("SMB FALHOU: " + e.getMessage());

                LOG.warn("SMB FALHOU | priceId={} codLojaRms={} servidor={} share={} msg={}",
                        cfg.getPriceId(),
                        cod,
                        nz(cfg.getSmbServidor()),
                        nz(cfg.getSmbCompartilhamento()),
                        e.getMessage(),
                        e);
            }
        } else {
            r.addMsg("SMB: não configurado (pulado).");
            LOG.debug("SMB pulado (não configurado) | priceId={} codLojaRms={}", cfg.getPriceId(), cod);
        }

        // =========================
        // 5) MSG m1 (não derruba)
        // =========================
        executarCopiaMsgM1(cfg, pastaDia, r);

        long elapsed = System.currentTimeMillis() - t0;
        LOG.info("PRICE transfer end | priceId={} codLojaRms={} sftpOk={} downloadOk={} fsOk={} smbOk={} tempoMs={}",
                cfg.getPriceId(),
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

    /**
     * ✅ copia o arquivo MSG (ex.: stella_update.m1) do diretório base da loja para SMB PFI (MessageFiles)
     * Não derruba o principal.
     */
    private void executarCopiaMsgM1(ArquivosPrice cfg, Path pastaDia, PriceTestResult r) {
        long t0 = System.currentTimeMillis();

        try {
            boolean msgAtivo = Boolean.TRUE.equals(cfg.getMsgCopyAtivo());
            if (!msgAtivo) {
                r.addMsg("MSG: cópia desativada (msgCopyAtivo=false).");
                LOG.debug("MSG pulado (desativado) | priceId={} codLojaRms={}", cfg.getPriceId(), safeCod(cfg));
                return;
            }

            String msgNome = cfg.getMsgFileNomeLocal();
            String msgShare = cfg.getMsgSmbCompartilhamento();
            String msgSub = cfg.getMsgSmbSubpasta();

            if (msgNome == null || msgNome.isBlank()) {
                r.addMsg("MSG: msgFileNomeLocal não informado (pulado).");
                LOG.warn("MSG pulado (nome local vazio) | priceId={} codLojaRms={}", cfg.getPriceId(), safeCod(cfg));
                return;
            }
            if (msgShare == null || msgShare.isBlank()) {
                r.addMsg("MSG: msgSmbCompartilhamento não informado (pulado).");
                LOG.warn("MSG pulado (share vazio) | priceId={} codLojaRms={}", cfg.getPriceId(), safeCod(cfg));
                return;
            }
            if (msgSub == null || msgSub.isBlank()) {
                r.addMsg("MSG: msgSmbSubpasta não informado (pulado).");
                LOG.warn("MSG pulado (subpasta vazia) | priceId={} codLojaRms={}", cfg.getPriceId(), safeCod(cfg));
                return;
            }

            // pastaDia = .../LJ102/YYYY-MM-DD  -> base = .../LJ102
            Path pastaLojaBase = (pastaDia != null ? pastaDia.getParent() : null);
            if (pastaLojaBase == null) {
                r.addMsg("MSG: não consegui determinar a pasta base da loja (pulado).");
                LOG.warn("MSG pulado (pastaLojaBase null) | priceId={} codLojaRms={}", cfg.getPriceId(), safeCod(cfg));
                return;
            }

            Path arquivoMsgLocal = pastaLojaBase.resolve(msgNome);
            if (!Files.exists(arquivoMsgLocal)) {
                r.addMsg("MSG: arquivo não encontrado em " + arquivoMsgLocal + " (pulado).");
                LOG.warn("MSG arquivo não encontrado | priceId={} codLojaRms={} arquivoMsgLocal={}",
                        cfg.getPriceId(), safeCod(cfg), arquivoMsgLocal);
                return;
            }

            boolean smbMinimoCredenciais =
                    cfg.getSmbServidor() != null && !cfg.getSmbServidor().isBlank() &&
                    cfg.getSmbUsuario() != null && !cfg.getSmbUsuario().isBlank();

            if (!smbMinimoCredenciais) {
                r.addMsg("MSG: SMB sem credenciais mínimas (Servidor/Usuário). Não foi possível copiar.");
                LOG.warn("MSG SMB sem credenciais mínimas | priceId={} codLojaRms={} servidor={} usuario={}",
                        cfg.getPriceId(), safeCod(cfg), nz(cfg.getSmbServidor()), nz(cfg.getSmbUsuario()));
                return;
            }

            LOG.info("MSG copy start | priceId={} codLojaRms={} servidor={} share={} subpasta={} arquivoLocal={}",
                    cfg.getPriceId(),
                    safeCod(cfg),
                    nz(cfg.getSmbServidor()),
                    msgShare,
                    msgSub,
                    arquivoMsgLocal
            );

            smbCopyService.copiarParaSmb(
                    arquivoMsgLocal,
                    cfg.getSmbServidor(),
                    msgShare,
                    msgSub,
                    cfg.getSmbDominio(),
                    cfg.getSmbUsuario(),
                    cfg.getSmbSenha()
            );

            long elapsed = System.currentTimeMillis() - t0;
            r.addMsg("MSG OK: \\\\" + cfg.getSmbServidor() + "\\" + msgShare + "\\" + msgSub + " <- " + msgNome);

            LOG.info("MSG OK | priceId={} codLojaRms={} destino=\\\\{}\\{}\\{} tempoMs={}",
                    cfg.getPriceId(), safeCod(cfg), cfg.getSmbServidor(), msgShare, msgSub, elapsed);

        } catch (Exception e) {
            r.addMsg("MSG FALHOU: " + e.getMessage());
            LOG.warn("MSG FALHOU | priceId={} codLojaRms={} msg={}",
                    cfg != null ? cfg.getPriceId() : null,
                    safeCod(cfg),
                    e.getMessage(),
                    e);
        }
    }

    // =========================
    // ✅ Aliases de compatibilidade
    // =========================
    public PriceTestResult testarTransfer(Long priceId) { return testar(priceId); }
    public PriceTestResult testarTransfer(ArquivosPrice cfg) { return testar(cfg); }
    public PriceTestResult testarTransferencia(Long priceId) { return testar(priceId); }
    public PriceTestResult testarTransferencia(ArquivosPrice cfg) { return testar(cfg); }

    // =========================================================
    // ✅ Helpers: lastModified remoto/local e timezone
    // =========================================================
    private ZoneId resolveZone(ArquivosPrice cfg) {
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

        // tentativas de assinatura (3 args)
        v = tryInvokeSftp("obterLastModifiedRemoto", new Class<?>[]{LojaRemoteConfig.class, String.class, String.class}, new Object[]{rc, remoteDir, nomeArquivo});
        if (v == null) v = tryInvokeSftp("getLastModifiedRemoto",   new Class<?>[]{LojaRemoteConfig.class, String.class, String.class}, new Object[]{rc, remoteDir, nomeArquivo});
        if (v == null) v = tryInvokeSftp("remoteLastModified",      new Class<?>[]{LojaRemoteConfig.class, String.class, String.class}, new Object[]{rc, remoteDir, nomeArquivo});

        // tentativas de assinatura (2 args) com fullPath
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
            LOG.debug("Falha ao obter lastModified remoto via {} | msg={}", method, e.getMessage());
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

    private static Object safeLojaId(Loja l) {
        try { return (l != null ? l.getLojaId() : null); } catch (Exception e) { return null; }
    }

    private static String safeHost(LojaRemoteConfig rc) {
        try { return (rc != null ? rc.getHostRemoto() : null); } catch (Exception e) { return null; }
    }

    private static String safeCod(ArquivosPrice cfg) {
        try {
            if (cfg == null || cfg.getLoja() == null) return "-";
            String c = cfg.getLoja().getCodLojaRms();
            return (c == null || c.isBlank()) ? "-" : c;
        } catch (Exception e) {
            return "-";
        }
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