package sistema.rotinas.primefaces.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import sistema.rotinas.primefaces.dto.PriceTestResult;
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
import java.time.LocalDate;
import java.util.List;

@Service
public class PriceTransferService {

    private static final Logger log = LoggerFactory.getLogger(PriceTransferService.class);

    @Value("${price.retention-days:7}")
    private int retentionDays;

    @Autowired private SftpDownloadService sftpDownloadService;
    @Autowired private FsCopyService fsCopyService;
    @Autowired private SmbCopyService smbCopyService;

    @Autowired private IArquivosPricePatternService patternService;
    @Autowired private IArquivosPriceService arquivosPriceService;

    /**
     * ✅ Assinatura principal para o Bean: testar por ID
     */
    public PriceTestResult testar(Long priceId) {
        long ini = System.currentTimeMillis();
        log.info("[PRICE-TEST] testar(id) início - priceId={}", priceId);

        if (priceId == null) {
            throw new IllegalArgumentException("Informe o ID do cadastro PRICE para testar.");
        }

        ArquivosPrice cfg = arquivosPriceService.findById(priceId);
        if (cfg == null) {
            throw new IllegalArgumentException("Cadastro PRICE não encontrado (id=" + priceId + ").");
        }

        PriceTestResult r = testar(cfg);

        log.info("[PRICE-TEST] testar(id) fim - priceId={} tempoTotal={} ms",
                priceId, (System.currentTimeMillis() - ini));
        return r;
    }

    /**
     * ✅ Teste: baixa do SFTP (config remota do ArquivosPrice) para:
     * /uploads/rotinaalterados/price/LJ{cod}/YYYY-MM-DD/
     * e tenta copiar para SMB e FS (se estiverem preenchidos).
     *
     * IMPORTANTE: FS/SMB agora NÃO derrubam o teste se falharem.
     */
    public PriceTestResult testar(ArquivosPrice cfg) {
        long ini = System.currentTimeMillis();
        PriceTestResult r = new PriceTestResult();

        try {
            if (cfg == null) throw new IllegalArgumentException("Config PRICE não informada.");
            if (cfg.getPriceId() == null) throw new IllegalArgumentException("Salve a configuração PRICE antes de testar.");
            if (cfg.getRemoteConfig() == null) throw new IllegalArgumentException("Config Remota (SFTP) não informada.");
            if (cfg.getLoja() == null) throw new IllegalArgumentException("Loja não informada.");

            Loja loja = cfg.getLoja();
            String cod = loja.getCodLojaRms();
            if (cod == null || cod.isBlank()) throw new IllegalArgumentException("codLojaRms não informado.");

            LojaRemoteConfig rc = cfg.getRemoteConfig();

            log.info("[PRICE-TEST] início - priceId={} loja={} remoteHost={} remoteBaseDir={} subpastaRemota={}",
                    cfg.getPriceId(),
                    cod,
                    safe(rc.getHostRemoto()),
                    safe(rc.getBaseDirRemoto()),
                    safe(cfg.getSubpastaRemota()));

            // retenção
            log.info("[PRICE-TEST] limpeza por retenção - loja={} retentionDays={}", cod, retentionDays);
            PastaUploadUtil.limparPriceLojaPorRetencao(cod, retentionDays);

            // pasta do dia
            Path pastaDia = PastaUploadUtil.pastaPriceLojaDia(cod, LocalDate.now());
            log.info("[PRICE-TEST] pastaDia={}", pastaDia);

            // patterns
            List<ArquivosPricePattern> pats = patternService.listarPorPrice(cfg.getPriceId());
            if (pats == null || pats.isEmpty()) {
                throw new IllegalArgumentException("Nenhum pattern cadastrado para essa loja.");
            }
            List<String> patterns = pats.stream().map(ArquivosPricePattern::getPattern).toList();
            log.info("[PRICE-TEST] patterns({})={}", patterns.size(), patterns);

            // remote dir = baseDirRemoto + subpastaRemota (se houver)
            String remoteDir = juntarRemoto(rc.getBaseDirRemoto(), cfg.getSubpastaRemota());
            log.info("[PRICE-TEST] remoteDir={}", remoteDir);

            // =========================
            // 1) DOWNLOAD SFTP
            // =========================
            long t0 = System.currentTimeMillis();
            String arquivoRemoto = sftpDownloadService.baixarArquivoMaisRecenteQueCase(rc, remoteDir, patterns, pastaDia);
            long tDownload = System.currentTimeMillis() - t0;

            r.setArquivoRemoto(arquivoRemoto);

            Path arquivoLocal = pastaDia.resolve(arquivoRemoto);
            r.setArquivoLocal(arquivoLocal);

            r.setSftpOk(true);
            r.setDownloadOk(true);

            // audit do timestamp local (deve estar preservado pela SftpDownloadService)
            try {
                FileTime ft = Files.getLastModifiedTime(arquivoLocal);
                log.info("[PRICE-TEST] download OK - arquivo={} local={} lastModifiedLocal={} tempo={} ms",
                        arquivoRemoto, arquivoLocal, ft, tDownload);
                r.addMsg("Download OK: " + arquivoRemoto);
                r.addMsg("LastModified local (pós-download): " + ft);
            } catch (Exception exTime) {
                log.warn("[PRICE-TEST] download OK mas não consegui ler lastModified do arquivo local: {} - {}",
                        arquivoLocal, exTime.getMessage());
                r.addMsg("Download OK: " + arquivoRemoto);
                r.addMsg("Aviso: não foi possível ler o LastModified local do arquivo.");
            }

            // =========================
            // 2) COPY FS (não derruba o teste)
            // =========================
            if (cfg.getCaminhoFsDestino() != null && !cfg.getCaminhoFsDestino().isBlank()) {
                long t1 = System.currentTimeMillis();
                try {
                    log.info("[PRICE-TEST] FS - iniciando cópia - destino={}", cfg.getCaminhoFsDestino());
                    fsCopyService.copiarParaFs(arquivoLocal, cfg.getCaminhoFsDestino());
                    r.setFsOk(true);
                    r.addMsg("FS OK: " + cfg.getCaminhoFsDestino());
                    log.info("[PRICE-TEST] FS - OK - tempo={} ms", (System.currentTimeMillis() - t1));
                } catch (Exception e) {
                    r.setFsOk(false);
                    r.addMsg("FS FALHOU: " + safeMsg(e));
                    log.error("[PRICE-TEST] FS - FALHOU - destino={} msg={}",
                            cfg.getCaminhoFsDestino(), e.getMessage(), e);
                }
            } else {
                r.addMsg("FS: não configurado (pulado).");
                log.info("[PRICE-TEST] FS - não configurado (pulado)");
            }

            // =========================
            // 3) COPY SMB (não derruba o teste)
            // =========================
            boolean smbTemMinimo =
                    cfg.getSmbServidor() != null && !cfg.getSmbServidor().isBlank() &&
                    cfg.getSmbCompartilhamento() != null && !cfg.getSmbCompartilhamento().isBlank() &&
                    cfg.getSmbUsuario() != null && !cfg.getSmbUsuario().isBlank();

            if (smbTemMinimo) {
                long t2 = System.currentTimeMillis();
                try {
                    log.info("[PRICE-TEST] SMB - iniciando cópia - servidor={} share={} subpasta={} dominio={} usuario={}",
                            cfg.getSmbServidor(),
                            cfg.getSmbCompartilhamento(),
                            safe(cfg.getSmbSubpasta()),
                            safe(cfg.getSmbDominio()),
                            cfg.getSmbUsuario());

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
                    r.addMsg("SMB OK: " + cfg.getSmbServidor() + "\\" + cfg.getSmbCompartilhamento());
                    log.info("[PRICE-TEST] SMB - OK - tempo={} ms", (System.currentTimeMillis() - t2));

                } catch (Exception e) {
                    r.setSmbOk(false);
                    r.addMsg("SMB FALHOU: " + safeMsg(e));
                    log.error("[PRICE-TEST] SMB - FALHOU - servidor={} share={} msg={}",
                            cfg.getSmbServidor(), cfg.getSmbCompartilhamento(), e.getMessage(), e);
                }
            } else {
                r.addMsg("SMB: não configurado (pulado).");
                log.info("[PRICE-TEST] SMB - não configurado (pulado)");
            }

            return r;

        } finally {
            log.info("[PRICE-TEST] fim - priceId={} tempoTotal={} ms",
                    (cfg != null ? cfg.getPriceId() : null),
                    (System.currentTimeMillis() - ini));
        }
    }

    // =========================
    // ✅ Aliases de compatibilidade
    // =========================
    public PriceTestResult testarTransfer(Long priceId) { return testar(priceId); }
    public PriceTestResult testarTransfer(ArquivosPrice cfg) { return testar(cfg); }
    public PriceTestResult testarTransferencia(Long priceId) { return testar(priceId); }
    public PriceTestResult testarTransferencia(ArquivosPrice cfg) { return testar(cfg); }

    private String juntarRemoto(String base, String sub) {
        String b = (base == null ? "" : base.trim());
        String s = (sub == null ? "" : sub.trim());

        if (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        if (s.startsWith("/")) s = s.substring(1);

        if (b.isEmpty()) return "/" + s;
        if (s.isEmpty()) return b;
        return b + "/" + s;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String safeMsg(Throwable t) {
        if (t == null) return "";
        // InvocationTargetException às vezes vem com msg null — tenta cair na causa
        if (t.getMessage() != null && !t.getMessage().isBlank()) return t.getMessage();
        if (t.getCause() != null && t.getCause().getMessage() != null) return t.getCause().getMessage();
        return t.getClass().getSimpleName();
    }
}
