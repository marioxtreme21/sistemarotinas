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

    @Transactional(readOnly = true)
    public MgvTestResult testar(Long mgvId) {
        if (mgvId == null) throw new IllegalArgumentException("Informe o ID do cadastro MGV para testar.");

        ArquivosMgv cfg = arquivosMgvService.findById(mgvId);
        if (cfg == null) throw new IllegalArgumentException("Cadastro MGV não encontrado (id=" + mgvId + ").");

        return testar(cfg);
    }

    /**
     * ✅ Baixa do SFTP para:
     * /uploads/rotinaalterados/mgv/LJ{cod}/YYYY-MM-DD/
     *
     * ✅ MGV: múltiplos arquivos (1 por pattern).
     * ✅ NÃO renomeia. Sobrescreve normal.
     *
     * ✅ Ajustes alinhados ao PRICE:
     * - Usa mtime remoto vindo do download (sem reconectar) + fallback local
     * - FS/SMB por arquivo sem derrubar lote
     * - statusGeral + detalheGeral
     * - preenche MgvTestResult.ArquivoInfo (origem/destino + status FS/SMB)
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
            r.setStatusGeral("FALHOU");
            r.setDetalheGeral("Falha ao preparar pasta do dia.");
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
        // 1) DOWNLOAD SFTP (batch com fallback)
        // =========================
        List<SftpDownloadInfo> infosDownload;
        try {
            DownloadBatch batch = baixarBatchSftp(rc, remoteDir, patterns, pastaDia);

            List<String> arquivosRemotos = (batch != null && batch.arquivosRemotos != null) ? batch.arquivosRemotos : new ArrayList<>();
            List<Path> arquivosLocais  = (batch != null && batch.arquivosLocais  != null) ? batch.arquivosLocais  : new ArrayList<>();
            infosDownload = (batch != null && batch.infos != null) ? batch.infos : new ArrayList<>();

            r.setArquivosRemotos(arquivosRemotos);
            r.setArquivosLocais(arquivosLocais);

            r.setSftpOk(true);

            if (arquivosRemotos.isEmpty()) {
                r.setDownloadOk(false);
                r.setStatusGeral("FALHOU");
                r.setDetalheGeral("Download retornou vazio.");
                r.addMsg("FALHA: nenhum arquivo retornado no download.");
                LOG.warn("Download retornou vazio | mgvId={} codLojaRms={}", cfg.getMgvId(), cod);
                return r;
            }

            // valida existência local
            boolean algumInexistente = arquivosLocais.stream().anyMatch(p -> p == null || !Files.exists(p));
            if (algumInexistente) {
                r.setDownloadOk(false);
                r.addMsg("⚠ Alguns arquivos baixados não foram encontrados localmente (ver logs).");
                LOG.warn("Alguns arquivos locais não encontrados após download | mgvId={} codLojaRms={}", cfg.getMgvId(), cod);
            } else {
                r.setDownloadOk(true);
            }

            int esperados = patterns.size();
            int baixados = arquivosRemotos.size();
            if (baixados < esperados) {
                r.addMsg("⚠ Download parcial: baixados=" + baixados + " de esperados=" + esperados);
                LOG.warn("Download parcial | mgvId={} codLojaRms={} baixados={} esperados={}",
                        cfg.getMgvId(), cod, baixados, esperados);
            } else {
                r.addMsg("Download OK: " + baixados + " arquivo(s)");
            }

            // preenche origem/destino por arquivo
            for (int i = 0; i < arquivosRemotos.size(); i++) {
                String nome = arquivosRemotos.get(i);
                Path local = (i < arquivosLocais.size() ? arquivosLocais.get(i) : null);

                MgvTestResult.ArquivoInfo ai = r.getOrCreateArquivoInfo(nome);
                if (ai != null) {
                    ai.setOrigemRemota(remoteDir + (remoteDir.endsWith("/") ? "" : "/") + nome);
                    ai.setDestinoLocal(local != null ? String.valueOf(local) : null);
                }
            }

            LOG.info("Download concluído | mgvId={} codLojaRms={} baixados={} esperados={} downloadOk={}",
                    cfg.getMgvId(), cod, r.getArquivosRemotos().size(), patterns.size(), r.isDownloadOk());

        } catch (Exception e) {
            r.setSftpOk(false);
            r.setDownloadOk(false);
            r.setStatusGeral("FALHOU");
            r.setDetalheGeral("Falha no download SFTP.");
            r.addMsg("FALHA no download SFTP: " + e.getMessage());

            LOG.error("Falha no download SFTP | mgvId={} codLojaRms={} remoteHost={} remoteDir={} msg={}",
                    cfg.getMgvId(), cod, safeHost(rc), remoteDir, e.getMessage(), e);

            return r;
        }

        // =========================
        // 2) lastModified + valida data (por arquivo)
        // =========================
        int desatualizados = 0;
        int semLastModified = 0;

        try {
            // preferencial: mtime que veio do download
            for (SftpDownloadInfo inf : infosDownload) {
                if (inf == null || inf.nomeArquivo() == null || inf.nomeArquivo().isBlank()) continue;

                String nome = inf.nomeArquivo();
                MgvTestResult.ArquivoInfo ai = r.getOrCreateArquivoInfo(nome);

                LocalDateTime lastMod = null;

                if (inf.mtimeEpochSeconds() != null) {
                    lastMod = LocalDateTime.ofInstant(Instant.ofEpochSecond(inf.mtimeEpochSeconds()), zone);
                }

                // fallback local
                if (lastMod == null) {
                    Path local = localizarLocalPorNome(nome, r.getArquivosLocais());
                    lastMod = tentarObterLastModifiedLocal(local, zone);
                }

                if (ai != null) ai.setLastModified(lastMod);

                if (lastMod != null) {
                    boolean okData = lastMod.toLocalDate().isEqual(hoje);
                    if (ai != null) ai.setAtualizado(okData);

                    String fmt = lastMod.format(FMT);
                    if (okData) {
                        r.addMsg("Data OK: " + nome + " | " + fmt);
                        LOG.info("Data OK | mgvId={} codLojaRms={} arquivo={} lastModified={}",
                                cfg.getMgvId(), cod, nome, fmt);
                    } else {
                        desatualizados++;
                        r.addMsg("⚠ DESATUALIZADO: " + nome + " | " + fmt + " | Execução: " + hoje);
                        LOG.warn("Arquivo desatualizado | mgvId={} codLojaRms={} arquivo={} lastModified={} execucao={}",
                                cfg.getMgvId(), cod, nome, fmt, hoje);
                    }
                } else {
                    semLastModified++;
                    if (ai != null) ai.setAtualizado(null);
                    r.addMsg("⚠ Sem lastModified: " + nome);
                    LOG.warn("Sem lastModified p/ arquivo | mgvId={} codLojaRms={} arquivo={}",
                            cfg.getMgvId(), cod, nome);
                }
            }

        } catch (Exception e) {
            r.addMsg("⚠ Falha ao validar lastModified: " + e.getMessage());
            LOG.warn("Falha ao validar lastModified (ignorada) | mgvId={} codLojaRms={} msg={}",
                    cfg.getMgvId(), cod, e.getMessage(), e);
        }

        // =========================
        // 3) FS copy (se configurado) - copia TODOS (por arquivo)
        // =========================
        boolean fsConfigurado = cfg.getCaminhoFsDestino() != null && !cfg.getCaminhoFsDestino().isBlank();
        int fsCopiados = 0;
        int fsFalhas = 0;

        if (fsConfigurado) {
            LOG.info("FS copy start | mgvId={} codLojaRms={} destinoFs={} totalArquivos={}",
                    cfg.getMgvId(), cod, cfg.getCaminhoFsDestino(), safeSize(r.getArquivosLocais()));

            if (r.getArquivosLocais() != null) {
                for (Path p : r.getArquivosLocais()) {
                    if (p == null) continue;

                    String nome = (p.getFileName() != null ? p.getFileName().toString() : p.toString());
                    MgvTestResult.ArquivoInfo ai = r.getOrCreateArquivoInfo(nome);

                    if (!Files.exists(p)) {
                        fsFalhas++;
                        if (ai != null) {
                            ai.setFsStatus("FALHOU");
                            ai.setDetalhe(merge(ai.getDetalhe(), "FS: arquivo local não existe."));
                        }
                        continue;
                    }

                    try {
                        fsCopyService.copiarParaFs(p, cfg.getCaminhoFsDestino());
                        fsCopiados++;
                        if (ai != null) ai.setFsStatus("OK");
                    } catch (Exception ex) {
                        fsFalhas++;
                        if (ai != null) {
                            ai.setFsStatus("FALHOU");
                            ai.setDetalhe(merge(ai.getDetalhe(), "FS falhou: " + ex.getMessage()));
                        }
                        LOG.warn("FS falhou por arquivo | mgvId={} codLojaRms={} arquivo={} destinoFs={} msg={}",
                                cfg.getMgvId(), cod, p, cfg.getCaminhoFsDestino(), ex.getMessage());
                    }
                }
            }

            boolean fsOk = (fsFalhas == 0 && fsCopiados > 0);
            r.setFsOk(fsOk);

            if (fsOk) {
                r.addMsg("FS OK: " + cfg.getCaminhoFsDestino() + " | copiados=" + fsCopiados);
                LOG.info("FS OK | mgvId={} codLojaRms={} destinoFs={} copiados={}",
                        cfg.getMgvId(), cod, cfg.getCaminhoFsDestino(), fsCopiados);
            } else {
                r.addMsg("⚠ FS parcial/falhou: destino=" + cfg.getCaminhoFsDestino() + " | copiados=" + fsCopiados + " | falhas=" + fsFalhas);
                LOG.warn("FS parcial/falhou | mgvId={} codLojaRms={} destinoFs={} copiados={} falhas={}",
                        cfg.getMgvId(), cod, cfg.getCaminhoFsDestino(), fsCopiados, fsFalhas);
            }

        } else {
            r.addMsg("FS: não configurado (pulado).");
            r.setFsOk(false); // mantém semântico: não usado
            if (r.getArquivosRemotos() != null) {
                for (String nome : r.getArquivosRemotos()) {
                    MgvTestResult.ArquivoInfo ai = r.getOrCreateArquivoInfo(nome);
                    if (ai != null && ai.getFsStatus() == null) ai.setFsStatus("PULADO");
                }
            }
        }

        // =========================
        // 4) SMB copy (se configurado) - copia TODOS (por arquivo)
        // =========================
        boolean smbConfigurado =
                cfg.getSmbServidor() != null && !cfg.getSmbServidor().isBlank() &&
                cfg.getSmbCompartilhamento() != null && !cfg.getSmbCompartilhamento().isBlank() &&
                cfg.getSmbUsuario() != null && !cfg.getSmbUsuario().isBlank();

        int smbCopiados = 0;
        int smbFalhas = 0;

        if (smbConfigurado) {

            String share = cfg.getSmbServidor() + "\\" + cfg.getSmbCompartilhamento();

            LOG.info("SMB copy start | mgvId={} codLojaRms={} share={} subpasta={} totalArquivos={}",
                    cfg.getMgvId(), cod, share, nz(cfg.getSmbSubpasta()), safeSize(r.getArquivosLocais()));

            if (r.getArquivosLocais() != null) {
                for (Path p : r.getArquivosLocais()) {
                    if (p == null) continue;

                    String nome = (p.getFileName() != null ? p.getFileName().toString() : p.toString());
                    MgvTestResult.ArquivoInfo ai = r.getOrCreateArquivoInfo(nome);

                    if (!Files.exists(p)) {
                        smbFalhas++;
                        if (ai != null) {
                            ai.setSmbStatus("FALHOU");
                            ai.setDetalhe(merge(ai.getDetalhe(), "SMB: arquivo local não existe."));
                        }
                        continue;
                    }

                    try {
                        smbCopyService.copiarParaSmb(
                                p,
                                cfg.getSmbServidor(),
                                cfg.getSmbCompartilhamento(),
                                cfg.getSmbSubpasta(),
                                cfg.getSmbDominio(),
                                cfg.getSmbUsuario(),
                                cfg.getSmbSenha()
                        );
                        smbCopiados++;
                        if (ai != null) ai.setSmbStatus("OK");
                    } catch (Exception ex) {
                        smbFalhas++;
                        if (ai != null) {
                            ai.setSmbStatus("FALHOU");
                            ai.setDetalhe(merge(ai.getDetalhe(), "SMB falhou: " + ex.getMessage()));
                        }
                        LOG.warn("SMB falhou por arquivo | mgvId={} codLojaRms={} arquivo={} share={} msg={}",
                                cfg.getMgvId(), cod, p, share, ex.getMessage());
                    }
                }
            }

            boolean smbOk = (smbFalhas == 0 && smbCopiados > 0);
            r.setSmbOk(smbOk);

            if (smbOk) {
                r.addMsg("SMB OK: " + share + " | copiados=" + smbCopiados);
                LOG.info("SMB OK | mgvId={} codLojaRms={} share={} copiados={}",
                        cfg.getMgvId(), cod, share, smbCopiados);
            } else {
                r.addMsg("⚠ SMB parcial/falhou: share=" + share + " | copiados=" + smbCopiados + " | falhas=" + smbFalhas);
                LOG.warn("SMB parcial/falhou | mgvId={} codLojaRms={} share={} copiados={} falhas={}",
                        cfg.getMgvId(), cod, share, smbCopiados, smbFalhas);
            }

        } else {
            r.addMsg("SMB: não configurado (pulado).");
            r.setSmbOk(false);
            if (r.getArquivosRemotos() != null) {
                for (String nome : r.getArquivosRemotos()) {
                    MgvTestResult.ArquivoInfo ai = r.getOrCreateArquivoInfo(nome);
                    if (ai != null && ai.getSmbStatus() == null) ai.setSmbStatus("PULADO");
                }
            }
        }

        // =========================
        // 5) Status geral
        // =========================
        String statusGeral = calcularStatusGeral(
                r,
                patterns.size(),
                desatualizados,
                semLastModified,
                fsConfigurado,
                smbConfigurado
        );

        r.setStatusGeral(statusGeral);

        String detalhe = "baixados=" + safeSize(r.getArquivosRemotos())
                + " | esperados=" + patterns.size()
                + " | desatualizados=" + desatualizados
                + " | semLastModified=" + semLastModified
                + (fsConfigurado ? (" | fsOk=" + r.isFsOk() + " copiados=" + fsCopiados + " falhas=" + fsFalhas) : " | fs=pulado")
                + (smbConfigurado ? (" | smbOk=" + r.isSmbOk() + " copiados=" + smbCopiados + " falhas=" + smbFalhas) : " | smb=pulado");

        r.setDetalheGeral(detalhe);

        long elapsed = System.currentTimeMillis() - t0;
        LOG.info("MGV transfer end | mgvId={} codLojaRms={} statusGeral={} sftpOk={} downloadOk={} fsOk={} smbOk={} tempoMs={} detalhe={}",
                cfg.getMgvId(),
                cod,
                statusGeral,
                r.isSftpOk(),
                r.isDownloadOk(),
                r.isFsOk(),
                r.isSmbOk(),
                elapsed,
                detalhe
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
     * - Se falhar, cai no fallback por pattern.
     */
    private DownloadBatch baixarBatchSftp(LojaRemoteConfig rc, String remoteDir, List<String> patterns, Path pastaDia) throws Exception {

        // 1) batch novo (1 conexão por loja)
        try {
            List<SftpDownloadInfo> infos = sftpDownloadService.baixarArquivosMaisRecentesPorPattern(rc, remoteDir, patterns, pastaDia);
            if (infos == null) infos = new ArrayList<>();

            List<String> nomes = infos.stream()
                    .filter(x -> x != null && x.nomeArquivo() != null && !x.nomeArquivo().isBlank())
                    .map(SftpDownloadInfo::nomeArquivo)
                    .toList();

            List<Path> locais = nomes.stream().map(pastaDia::resolve).toList();
            return new DownloadBatch(nomes, locais, infos);

        } catch (Exception e) {
            LOG.warn("Batch novo falhou; usando fallback por pattern (1 conexão por pattern). msg={}", e.getMessage());
        }

        // 2) fallback: 1 por pattern
        List<String> remotos = new ArrayList<>();
        List<Path> locais = new ArrayList<>();
        List<SftpDownloadInfo> infosOut = new ArrayList<>();

        int idx = 0;
        for (String pattern : patterns) {
            idx++;

            SftpDownloadInfo info = sftpDownloadService.baixarArquivoMaisRecenteQueCaseInfo(
                    rc, remoteDir, List.of(pattern), pastaDia
            );

            String nomeRemoto = (info != null ? info.nomeArquivo() : null);
            if (nomeRemoto == null || nomeRemoto.isBlank()) {
                LOG.debug("Fallback download por pattern: nenhum arquivo | {} / {} | pattern={}",
                        idx, patterns.size(), pattern);
                continue;
            }

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
            if (tz != null && !tz.isBlank()) return ZoneId.of(tz.trim());
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

    private static Path localizarLocalPorNome(String nomeArquivo, List<Path> locais) {
        if (nomeArquivo == null || locais == null || locais.isEmpty()) return null;
        for (Path p : locais) {
            try {
                if (p != null && p.getFileName() != null && p.getFileName().toString().equalsIgnoreCase(nomeArquivo)) {
                    return p;
                }
            } catch (Exception ignore) { }
        }
        return null;
    }

    private static String calcularStatusGeral(MgvTestResult r,
                                             int totalEsperado,
                                             int desatualizados,
                                             int semLastModified,
                                             boolean fsConfigurado,
                                             boolean smbConfigurado) {
        if (r == null) return "INDEFINIDO";
        if (!r.isDownloadOk()) return "FALHOU";

        boolean parcialPorQtd = (safeSize(r.getArquivosRemotos()) < totalEsperado);
        boolean parcialPorValidacao = (desatualizados > 0) || (semLastModified > 0);
        boolean parcialPorDestino = (fsConfigurado && !r.isFsOk()) || (smbConfigurado && !r.isSmbOk());

        if (parcialPorQtd || parcialPorValidacao || parcialPorDestino) return "FALHA_PARCIAL";
        return "OK";
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
    private static String merge(String a, String b) {
        if (a == null || a.isBlank()) return b;
        if (b == null || b.isBlank()) return a;
        return a + " | " + b;
    }

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