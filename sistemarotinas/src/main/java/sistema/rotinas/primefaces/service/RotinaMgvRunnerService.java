package sistema.rotinas.primefaces.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sistema.rotinas.primefaces.dto.MgvTestResult;
import sistema.rotinas.primefaces.dto.MgvTestResult.ArquivoInfo;
import sistema.rotinas.primefaces.enums.EtapaArquivoEnum;
import sistema.rotinas.primefaces.enums.OrigemExecucaoEnum;
import sistema.rotinas.primefaces.enums.StatusExecucaoEnum;
import sistema.rotinas.primefaces.enums.TipoRotinaEnum;
import sistema.rotinas.primefaces.model.ArquivosMgv;
import sistema.rotinas.primefaces.model.ArquivosMgvPattern;
import sistema.rotinas.primefaces.model.Loja;
import sistema.rotinas.primefaces.model.rotina.RotinaExecucao;
import sistema.rotinas.primefaces.model.rotina.RotinaExecucaoArquivo;
import sistema.rotinas.primefaces.model.rotina.RotinaExecucaoArquivoEtapa;
import sistema.rotinas.primefaces.model.rotina.RotinaExecucaoLoja;
import sistema.rotinas.primefaces.repository.RotinaExecucaoArquivoEtapaRepository;
import sistema.rotinas.primefaces.repository.RotinaExecucaoArquivoRepository;
import sistema.rotinas.primefaces.service.interfaces.IArquivosMgvPatternService;
import sistema.rotinas.primefaces.service.interfaces.IArquivosMgvService;
import sistema.rotinas.primefaces.service.interfaces.ILojaService;
import sistema.rotinas.primefaces.service.interfaces.IRotinaExecucaoService;
import sistema.rotinas.primefaces.service.interfaces.IRotinaMgvRunnerService;

@Service
public class RotinaMgvRunnerService implements IRotinaMgvRunnerService {

    /**
     * ✅ Logger dedicado
     * logback: <logger name="ROTINA_MGV" .../>
     */
    private static final Logger LOG = LoggerFactory.getLogger("ROTINA_MGV");

    /**
     * ✅ Limite seguro para gravar erro no banco (evita Data truncation).
     * Ajuste se sua coluna for maior/menor.
     */
    private static final int DB_ERR_MAX = 2000;

    @Autowired private IRotinaExecucaoService execucaoService;
    @Autowired private ILojaService lojaService;

    @Autowired private IArquivosMgvService arquivosMgvService;
    @Autowired private IArquivosMgvPatternService patternService;

    @Autowired private RotinaExecucaoArquivoRepository execArquivoRepo;
    @Autowired private RotinaExecucaoArquivoEtapaRepository etapaRepo;

    @Autowired private MgvTransferService mgvTransferService;

    @Autowired private NotificacaoRotinaService notificacaoRotinaService;

    @Override
    public Long executar(List<Long> lojaIds, OrigemExecucaoEnum origem, String solicitante) {

        LOG.info("[MGV][RUNNER] Iniciando. origem={} solicitante={} lojasSelecionadas={}",
                origem, nz(solicitante), (lojaIds == null || lojaIds.isEmpty()) ? "TODAS" : lojaIds);

        RotinaExecucao execucao = execucaoService.iniciarExecucao(TipoRotinaEnum.MGV, origem, solicitante);
        Long execucaoId = execucao != null ? execucao.getExecucaoId() : null;

        LOG.info("[MGV][RUNNER] Execução iniciada. execucaoId={}", execucaoId);

        List<StatusExecucaoEnum> statusLojas = new ArrayList<>();
        int lojasIgnoradasSemConfig = 0;

        try {
            List<Loja> lojasBase = resolverLojas(lojaIds);

            LOG.info("[MGV][RUNNER] Lojas base resolvidas. execucaoId={} totalBase={}",
                    execucaoId, (lojasBase != null ? lojasBase.size() : 0));

            if (lojasBase == null || lojasBase.isEmpty()) {
                LOG.warn("[MGV][RUNNER] Nenhuma loja encontrada para executar. execucaoId={}", execucaoId);
                execucaoService.finalizarExecucao(execucaoId, StatusExecucaoEnum.FALHA,
                        "Nenhuma loja encontrada para executar.", "Lista de lojas vazia.");
                return execucaoId;
            }

            // ✅ executa somente lojas com configuração ArquivosMgv válida
            List<Loja> lojas = new ArrayList<>();
            for (Loja loja : lojasBase) {
                ArquivosMgv cfg = obterCfgMgvDaLoja(loja);
                boolean cfgOk = (cfg != null && cfg.getMgvId() != null);
                if (cfgOk) {
                    lojas.add(loja);
                } else {
                    lojasIgnoradasSemConfig++;
                    LOG.warn("[MGV][RUNNER] Loja ignorada (sem configuração ArquivosMgv). execucaoId={} lojaId={} codLojaRms={} nome={}",
                            execucaoId,
                            safeLojaId(loja),
                            safeCodLoja(loja),
                            safeNomeLoja(loja));
                }
            }

            if (lojas.isEmpty()) {
                String detalhe = "Nenhuma loja com configuração ArquivosMgv válida. Ignoradas=" + lojasIgnoradasSemConfig;
                LOG.warn("[MGV][RUNNER] Nenhuma loja com configuração MGV para executar. execucaoId={} {}",
                        execucaoId, detalhe);

                execucaoService.finalizarExecucao(execucaoId, StatusExecucaoEnum.FALHA,
                        "Nenhuma loja com configuração MGV para executar.", detalhe);
                return execucaoId;
            }

            LOG.info("[MGV][RUNNER] Lojas elegíveis. execucaoId={} totalElegiveis={} ignoradasSemConfig={}",
                    execucaoId, lojas.size(), lojasIgnoradasSemConfig);

            for (Loja loja : lojas) {
                StatusExecucaoEnum statusLoja = executarLoja(execucao, loja);
                statusLojas.add(statusLoja);
            }

            StatusExecucaoEnum statusFinal = calcularStatusFinal(statusLojas);
            String resumo = montarResumo(statusFinal, statusLojas, lojasIgnoradasSemConfig);

            LOG.info("[MGV][RUNNER] Finalizando execução. execucaoId={} statusFinal={} resumo={}",
                    execucaoId, statusFinal, resumo);

            execucaoService.finalizarExecucao(execucaoId, statusFinal, resumo, null);

        } catch (Exception e) {

            LOG.error("[MGV][RUNNER] Falha geral. execucaoId={} msg={}", execucaoId, e.getMessage(), e);

            execucaoService.finalizarExecucao(execucaoId, StatusExecucaoEnum.FALHA,
                    "Falha ao executar rotina MGV", stackTrace(e));

        } finally {
            // ✅ Dispara e-mail após concluir (não pode derrubar a execução)
            try {
                if (notificacaoRotinaService != null && execucaoId != null) {
                    LOG.info("[MGV][RUNNER] Disparando notificação por e-mail. execucaoId={}", execucaoId);
                    notificacaoRotinaService.notificarFinalizacaoRotinaMgv(execucaoId);
                } else {
                    LOG.warn("[MGV][RUNNER] Notificação não disparada (service null ou execucaoId null). execucaoId={}", execucaoId);
                }
            } catch (Exception ex) {
                LOG.warn("[MGV][RUNNER] Falha ao enviar e-mail de notificação. execucaoId={} msg={}",
                        execucaoId, ex.getMessage(), ex);
            }
        }

        return execucaoId;
    }

    private StatusExecucaoEnum executarLoja(RotinaExecucao execucao, Loja loja) {

        Long execucaoId = execucao != null ? execucao.getExecucaoId() : null;

        LOG.info("[MGV][LOJA] Iniciando. execucaoId={} lojaId={} codLojaRms={} nome={}",
                execucaoId, safeLojaId(loja), safeCodLoja(loja), safeNomeLoja(loja));

        RotinaExecucaoLoja el = execucaoService.iniciarLoja(execucaoId, loja);
        Long execucaoLojaId = (el != null ? el.getExecucaoLojaId() : null);

        try {
            ArquivosMgv cfg = obterCfgMgvDaLoja(loja);

            if (cfg == null || cfg.getMgvId() == null) {
                LOG.warn("[MGV][LOJA] Configuração MGV ausente (inesperado). execucaoId={} execucaoLojaId={} lojaId={} codLojaRms={}",
                        execucaoId, execucaoLojaId, safeLojaId(loja), safeCodLoja(loja));

                execucaoService.finalizarLoja(execucaoLojaId, StatusExecucaoEnum.FALHA,
                        "Sem configuração MGV para a loja", "Cadastre em Cadastro → Arquivos MGV.");
                return StatusExecucaoEnum.FALHA;
            }

            LOG.info("[MGV][LOJA] Config encontrada. execucaoId={} execucaoLojaId={} mgvId={} moverRemotoAposCopia={} dirProcessed={}",
                    execucaoId,
                    execucaoLojaId,
                    cfg.getMgvId(),
                    cfg.getMoverRemotoAposCopia(),
                    cfg.getDirRemotoProcessed());

            List<RotinaExecucaoArquivo> registros = criarRegistrosArquivos(execucao, el, loja, cfg);

            LOG.info("[MGV][LOJA] Arquivos registrados. execucaoId={} execucaoLojaId={} totalArquivos={}",
                    execucaoId, execucaoLojaId, (registros != null ? registros.size() : 0));

            Object result = invocarTransferService(cfg);

            LOG.info("[MGV][LOJA] TransferService retornou. execucaoId={} execucaoLojaId={} resultClass={}",
                    execucaoId, execucaoLojaId, (result != null ? result.getClass().getName() : null));

            preencherResultadoNosRegistros(registros, execucao, cfg, result);

            StatusExecucaoEnum statusLoja = calcularStatusLoja(registros);
            String resumo = resumoLoja(registros, statusLoja);

            LOG.info("[MGV][LOJA] Finalizando. execucaoId={} execucaoLojaId={} status={} resumo={}",
                    execucaoId, execucaoLojaId, statusLoja, resumo);

            execucaoService.finalizarLoja(execucaoLojaId, statusLoja, resumo, null);
            return statusLoja;

        } catch (Exception e) {

            LOG.error("[MGV][LOJA] Falha. execucaoId={} execucaoLojaId={} lojaId={} codLojaRms={} msg={}",
                    execucaoId,
                    execucaoLojaId,
                    safeLojaId(loja),
                    safeCodLoja(loja),
                    e.getMessage(),
                    e);

            execucaoService.finalizarLoja(execucaoLojaId, StatusExecucaoEnum.FALHA,
                    "Falha na execução da loja", stackTrace(e));
            return StatusExecucaoEnum.FALHA;
        }
    }

    private List<RotinaExecucaoArquivo> criarRegistrosArquivos(RotinaExecucao execucao,
                                                              RotinaExecucaoLoja el,
                                                              Loja loja,
                                                              ArquivosMgv cfg) {

        List<RotinaExecucaoArquivo> regs = new ArrayList<>();

        List<ArquivosMgvPattern> patterns = patternService.listarPorMgv(cfg.getMgvId());
        if (patterns == null) patterns = new ArrayList<>();

        LOG.debug("[MGV][ARQ] Patterns carregados. execucaoId={} execucaoLojaId={} mgvId={} totalPatterns={}",
                (execucao != null ? execucao.getExecucaoId() : null),
                (el != null ? el.getExecucaoLojaId() : null),
                cfg.getMgvId(),
                patterns.size());

        for (ArquivosMgvPattern p : patterns) {

            RotinaExecucaoArquivo ra = new RotinaExecucaoArquivo();
            ra.setExecucao(execucao);
            ra.setExecucaoLoja(el);
            ra.setLoja(loja);
            if (loja != null) ra.setCodLojaRms(loja.getCodLojaRms());

            ra.setPatternEsperado(p.getPattern());
            ra.setRequired(Boolean.TRUE.equals(p.getRequired()));

            ra.setEtapa(EtapaArquivoEnum.CONEXAO_REMOTA_SFTP_CONSINCO);
            ra.setStatus(StatusExecucaoEnum.EM_ANDAMENTO);

            ra.setInicioEm(LocalDateTime.now());

            regs.add(execArquivoRepo.save(ra));
        }

        return regs;
    }

    private Object invocarTransferService(ArquivosMgv cfg) throws Exception {
        Long mgvId = cfg.getMgvId();

        LOG.info("[MGV][XFER] Invocando MgvTransferService. mgvId={}", mgvId);

        Object r;

        r = tryInvoke(mgvTransferService, "testar", new Class<?>[]{ Long.class }, new Object[]{ mgvId });
        if (r != null) return r;

        r = tryInvoke(mgvTransferService, "testarTransfer", new Class<?>[]{ Long.class }, new Object[]{ mgvId });
        if (r != null) return r;

        r = tryInvoke(mgvTransferService, "testarTransferencia", new Class<?>[]{ Long.class }, new Object[]{ mgvId });
        if (r != null) return r;

        r = tryInvoke(mgvTransferService, "testar", new Class<?>[]{ ArquivosMgv.class }, new Object[]{ cfg });
        if (r != null) return r;

        r = tryInvoke(mgvTransferService, "testarTransfer", new Class<?>[]{ ArquivosMgv.class }, new Object[]{ cfg });
        if (r != null) return r;

        throw new IllegalStateException(
                "Não encontrei método compatível no MgvTransferService (testar/testarTransfer/testarTransferencia)."
        );
    }

    private Object tryInvoke(Object target, String method, Class<?>[] paramTypes, Object[] args) {
        try {
            var m = target.getClass().getMethod(method, paramTypes);
            return m.invoke(target, args);
        } catch (NoSuchMethodException nsme) {
            return null;
        } catch (Exception e) {
            Throwable c = (e.getCause() != null ? e.getCause() : e);
            if (c instanceof RuntimeException) throw (RuntimeException) c;
            throw new RuntimeException(c);
        }
    }

    /**
     * ✅ AJUSTE:
     * - Consome MgvTestResult tipado
     * - Usa ArquivoInfo por arquivo (lastModified, atualizado, origem/destino e status FS/SMB por arquivo)
     * - Grava etapas FS e SMB independentemente de "tipoDestino" (porque o transfer copia se estiver configurado)
     */
    private void preencherResultadoNosRegistros(List<RotinaExecucaoArquivo> regs,
                                               RotinaExecucao execucao,
                                               ArquivosMgv cfg,
                                               Object result) {

        // ✅ preferencial (novo)
        if (result instanceof MgvTestResult r) {

            boolean remoteOk = r.isSftpOk();
            boolean downloadOk = r.isDownloadOk();

            boolean fsUsado = cfg.getCaminhoFsDestino() != null && !cfg.getCaminhoFsDestino().isBlank();
            boolean smbUsado =
                    cfg.getSmbServidor() != null && !cfg.getSmbServidor().isBlank() &&
                    cfg.getSmbCompartilhamento() != null && !cfg.getSmbCompartilhamento().isBlank() &&
                    cfg.getSmbUsuario() != null && !cfg.getSmbUsuario().isBlank();

            LocalDate execDate = (execucao.getInicioEm() != null ? execucao.getInicioEm().toLocalDate() : LocalDate.now());
            LocalDateTime fim = LocalDateTime.now();

            List<ArquivoInfo> infos = (r.getArquivosInfo() != null ? r.getArquivosInfo() : new ArrayList<>());
            List<String> remotos = (r.getArquivosRemotos() != null ? r.getArquivosRemotos() : new ArrayList<>());
            List<Path> locais = (r.getArquivosLocais() != null ? r.getArquivosLocais() : new ArrayList<>());

            LOG.info("[MGV][RESULT] (typed) remoteOk={} downloadOk={} fsUsado={} smbUsado={} totalRegs={} totalInfos={} totalRemotos={} totalLocais={} statusGeral={} detalheGeral={}",
                    remoteOk, downloadOk,
                    fsUsado, smbUsado,
                    (regs != null ? regs.size() : 0),
                    infos.size(), remotos.size(), locais.size(),
                    nz(r.getStatusGeral()),
                    nz(r.getDetalheGeral()));

            // controle para não reutilizar o mesmo info em mais de um registro
            Set<Integer> infosUsados = new HashSet<>();

            for (int i = 0; i < regs.size(); i++) {
                RotinaExecucaoArquivo ra = regs.get(i);

                ra.setFimEm(fim);
                if (ra.getInicioEm() != null) {
                    ra.setTempoTotalMs(Duration.between(ra.getInicioEm(), fim).toMillis());
                }

                // tenta casar ArquivoInfo pelo patternEsperado (glob/regex/equals) e, se não achar, usa por índice
                ArquivoInfo info = escolherInfoParaRegistro(ra, infos, infosUsados, i);

                String nomeArquivo = null;
                LocalDateTime lastModRemoto = null;
                Boolean atualizado = null;

                String origemRemota = null;
                String destinoLocalStr = null;

                String fsStatus = null;
                String smbStatus = null;
                String detalhe = null;

                if (info != null) {
                    nomeArquivo = info.getNomeArquivo();
                    lastModRemoto = info.getLastModified();
                    atualizado = info.getAtualizado();

                    origemRemota = info.getOrigemRemota();
                    destinoLocalStr = info.getDestinoLocal();

                    fsStatus = info.getFsStatus();
                    smbStatus = info.getSmbStatus();
                    detalhe = info.getDetalhe();
                }

                // fallback por índice se ainda não tem nome
                if ((nomeArquivo == null || nomeArquivo.isBlank()) && i < remotos.size()) {
                    nomeArquivo = remotos.get(i);
                }

                Path localPath = null;
                // se veio destinoLocal no info, usa ele
                if (destinoLocalStr != null && !destinoLocalStr.isBlank()) {
                    try { localPath = Path.of(destinoLocalStr); } catch (Exception ignore) {}
                }
                // senão tenta por índice
                if (localPath == null && i < locais.size()) {
                    localPath = locais.get(i);
                }
                // senão tenta achar pelo nome
                if (localPath == null && nomeArquivo != null) {
                    localPath = localizarLocalPorNome(nomeArquivo, locais);
                }

                // lastModified local + tamanho
                LocalDateTime lastModLocal = null;
                Long tamanhoLocal = null;
                try {
                    if (localPath != null && Files.exists(localPath)) {
                        tamanhoLocal = Files.size(localPath);
                        lastModLocal = LocalDateTime.ofInstant(
                                Files.getLastModifiedTime(localPath).toInstant(), ZoneId.systemDefault());
                    }
                } catch (Exception ignore) {}

                // se atualizado veio null, tenta derivar do lastModRemoto
                if (atualizado == null && lastModRemoto != null) {
                    atualizado = lastModRemoto.toLocalDate().isEqual(execDate);
                }

                // status FS/SMB por arquivo (se vier do info)
                boolean fsOkArquivo = statusOk(fsStatus);
                boolean smbOkArquivo = statusOk(smbStatus);

                // se não veio status por arquivo e o destino está configurado, cai no agregado
                if (fsUsado && (fsStatus == null || fsStatus.isBlank())) {
                    fsOkArquivo = r.isFsOk();
                }
                if (smbUsado && (smbStatus == null || smbStatus.isBlank())) {
                    smbOkArquivo = r.isSmbOk();
                }

                StatusExecucaoEnum statusPrincipal = calcularStatusPrincipal(downloadOk, fsUsado, smbUsado, fsOkArquivo, smbOkArquivo);

                // ======== Preenche registro base ========
                ra.setEtapa(EtapaArquivoEnum.VALIDACAO_ARQUIVOS);
                ra.setStatus(statusPrincipal);

                if (nomeArquivo != null && !nomeArquivo.isBlank()) {
                    // se o transfer te deu um caminho remoto completo, melhor
                    ra.setOrigem(origemRemota != null && !origemRemota.isBlank() ? origemRemota : nomeArquivo);

                    if (ra.getNomeArquivo() == null || ra.getNomeArquivo().isBlank()) {
                        ra.setNomeArquivo(nomeArquivo);
                    }
                }

                if (localPath != null) {
                    ra.setDestino(String.valueOf(localPath));
                    if (ra.getNomeArquivo() == null || ra.getNomeArquivo().isBlank()) {
                        ra.setNomeArquivo(localPath.getFileName() != null ? localPath.getFileName().toString() : String.valueOf(localPath));
                    }
                }

                ra.setLastModifiedOrigem(lastModRemoto);
                ra.setLastModifiedDestino(lastModLocal);
                ra.setOrigemAtualizada(atualizado);

                if (Boolean.FALSE.equals(atualizado)) {
                    ra.setMensagem(mergeMsg(ra.getMensagem(),
                            "Arquivo desatualizado (lastModified remoto diferente do dia da execução)."));
                }

                if (detalhe != null && !detalhe.isBlank()) {
                    ra.setMensagem(mergeMsg(ra.getMensagem(), detalhe));
                }

                // ======== Etapas detalhadas ========

                salvarEtapa(ra,
                        EtapaArquivoEnum.CONEXAO_REMOTA_SFTP_CONSINCO,
                        remoteOk ? StatusExecucaoEnum.SUCESSO : StatusExecucaoEnum.FALHA,
                        null,
                        null,
                        remoteOk ? "Conexão remota OK" : "Falha na conexão remota",
                        remoteOk ? null : "Conexão remota falhou",
                        null,
                        null,
                        null,
                        null);

                salvarEtapa(ra,
                        EtapaArquivoEnum.DOWNLOAD_REMOTO_SFTP_CONSINCO,
                        downloadOk ? StatusExecucaoEnum.SUCESSO : StatusExecucaoEnum.FALHA,
                        (origemRemota != null && !origemRemota.isBlank()) ? origemRemota : (nomeArquivo != null ? nomeArquivo : null),
                        (localPath != null ? String.valueOf(localPath) : null),
                        downloadOk ? "Download OK" : "Download falhou",
                        downloadOk ? null : "Download falhou",
                        null,
                        tamanhoLocal,
                        lastModRemoto,
                        lastModLocal);

                // FS
                if (fsUsado) {
                    salvarEtapa(ra,
                            EtapaArquivoEnum.COPIA_DESTINO_MGV_FS,
                            fsOkArquivo ? StatusExecucaoEnum.SUCESSO : StatusExecucaoEnum.FALHA,
                            (localPath != null ? String.valueOf(localPath) : null),
                            cfg.getCaminhoFsDestino(),
                            fsOkArquivo ? "Cópia FS OK" : "Cópia FS falhou",
                            fsOkArquivo ? null : (detalhe != null ? detalhe : "FS falhou"),
                            tamanhoLocal,
                            null,
                            lastModLocal,
                            null);
                } else {
                    salvarEtapa(ra,
                            EtapaArquivoEnum.COPIA_DESTINO_MGV_FS,
                            StatusExecucaoEnum.SUCESSO,
                            (localPath != null ? String.valueOf(localPath) : null),
                            null,
                            "FS não configurado (pulado).",
                            null,
                            null,
                            null,
                            null,
                            null);
                }

                // SMB
                if (smbUsado) {
                    String smbDestino = "\\\\" + cfg.getSmbServidor() + "\\" + cfg.getSmbCompartilhamento();

                    salvarEtapa(ra,
                            EtapaArquivoEnum.COPIA_DESTINO_MGV_SMB,
                            smbOkArquivo ? StatusExecucaoEnum.SUCESSO : StatusExecucaoEnum.FALHA,
                            (localPath != null ? String.valueOf(localPath) : null),
                            smbDestino,
                            smbOkArquivo ? "Cópia SMB OK" : "Cópia SMB falhou",
                            smbOkArquivo ? null : (detalhe != null ? detalhe : "SMB falhou"),
                            tamanhoLocal,
                            null,
                            lastModLocal,
                            null);
                } else {
                    salvarEtapa(ra,
                            EtapaArquivoEnum.COPIA_DESTINO_MGV_SMB,
                            StatusExecucaoEnum.SUCESSO,
                            (localPath != null ? String.valueOf(localPath) : null),
                            null,
                            "SMB não configurado (pulado).",
                            null,
                            null,
                            null,
                            null,
                            null);
                }

                salvarEtapa(ra,
                        EtapaArquivoEnum.VALIDACAO_ARQUIVOS,
                        statusPrincipal,
                        (origemRemota != null && !origemRemota.isBlank()) ? origemRemota : (nomeArquivo != null ? nomeArquivo : null),
                        (localPath != null ? String.valueOf(localPath) : null),
                        Boolean.FALSE.equals(atualizado)
                                ? "Arquivo desatualizado (lastModified remoto diferente do dia da execução)."
                                : "Validação OK",
                        null,
                        null,
                        null,
                        lastModRemoto,
                        lastModLocal);

                execArquivoRepo.save(ra);
            }

            return;
        }

        // =========================
        // fallback antigo (reflexivo)
        // =========================
        boolean remoteOk = getBoolean(result, "isSftpOk", "getSftpOk", "isRemoteOk", "getRemoteOk");
        boolean downloadOk = getBoolean(result, "isDownloadOk", "getDownloadOk");
        boolean fsOk = getBoolean(result, "isFsOk", "getFsOk");
        boolean smbOk = getBoolean(result, "isSmbOk", "getSmbOk");

        boolean fsUsado = cfg.getCaminhoFsDestino() != null && !cfg.getCaminhoFsDestino().isBlank();
        boolean smbUsado =
                cfg.getSmbServidor() != null && !cfg.getSmbServidor().isBlank() &&
                cfg.getSmbCompartilhamento() != null && !cfg.getSmbCompartilhamento().isBlank() &&
                cfg.getSmbUsuario() != null && !cfg.getSmbUsuario().isBlank();

        Object remoto = getObject(result, "getArquivoRemoto", "getNomeArquivoRemoto", "getRemoteFile");
        Object local  = getObject(result, "getArquivoLocal", "getLocalFile", "getPathLocal");

        LocalDateTime lastModRemoto = extrairLastModified(result);
        LocalDate execDate = (execucao.getInicioEm() != null ? execucao.getInicioEm().toLocalDate() : LocalDate.now());

        Boolean atualizado = null;
        if (lastModRemoto != null) {
            atualizado = lastModRemoto.toLocalDate().isEqual(execDate);
        }

        StatusExecucaoEnum statusPrincipal = calcularStatusPrincipal(downloadOk, fsUsado, smbUsado, fsOk, smbOk);

        LOG.info("[MGV][RESULT] (fallback) remoteOk={} downloadOk={} fsUsado={} fsOk={} smbUsado={} smbOk={} lastModRemoto={} atualizado={} remoto={} local={} statusPrincipal={}",
                remoteOk, downloadOk,
                fsUsado, fsOk, smbUsado, smbOk,
                lastModRemoto, atualizado,
                (remoto != null ? String.valueOf(remoto) : null),
                (local != null ? String.valueOf(local) : null),
                statusPrincipal);

        String msgData = (Boolean.FALSE.equals(atualizado))
                ? "Arquivo desatualizado (lastModified remoto diferente do dia da execução)."
                : null;

        LocalDateTime fim = LocalDateTime.now();

        Long tamanhoLocal = null;
        LocalDateTime lastModLocal = null;
        try {
            if (local instanceof Path) {
                Path p = (Path) local;
                if (Files.exists(p)) {
                    tamanhoLocal = Files.size(p);
                    lastModLocal = LocalDateTime.ofInstant(
                            Files.getLastModifiedTime(p).toInstant(), ZoneId.systemDefault());
                }
            } else if (local != null) {
                Path p = Path.of(String.valueOf(local));
                if (Files.exists(p)) {
                    tamanhoLocal = Files.size(p);
                    lastModLocal = LocalDateTime.ofInstant(
                            Files.getLastModifiedTime(p).toInstant(), ZoneId.systemDefault());
                }
            }
        } catch (Exception ignore) {}

        for (RotinaExecucaoArquivo ra : regs) {

            ra.setFimEm(fim);
            if (ra.getInicioEm() != null) {
                ra.setTempoTotalMs(Duration.between(ra.getInicioEm(), fim).toMillis());
            }

            ra.setEtapa(EtapaArquivoEnum.VALIDACAO_ARQUIVOS);
            ra.setStatus(statusPrincipal);

            if (remoto != null) {
                ra.setOrigem(String.valueOf(remoto));
                if (ra.getNomeArquivo() == null || ra.getNomeArquivo().isBlank()) {
                    ra.setNomeArquivo(String.valueOf(remoto));
                }
            }
            if (local != null) {
                ra.setDestino(String.valueOf(local));
                if (ra.getNomeArquivo() == null || ra.getNomeArquivo().isBlank()) {
                    ra.setNomeArquivo(String.valueOf(local));
                }
            }

            ra.setLastModifiedOrigem(lastModRemoto);
            ra.setLastModifiedDestino(lastModLocal);
            ra.setOrigemAtualizada(atualizado);

            if (msgData != null) {
                ra.setMensagem(mergeMsg(ra.getMensagem(), msgData));
            }

            salvarEtapa(ra,
                    EtapaArquivoEnum.CONEXAO_REMOTA_SFTP_CONSINCO,
                    remoteOk ? StatusExecucaoEnum.SUCESSO : StatusExecucaoEnum.FALHA,
                    null, null,
                    remoteOk ? "Conexão remota OK" : "Falha na conexão remota",
                    remoteOk ? null : "Conexão remota falhou",
                    null, null, null, null);

            salvarEtapa(ra,
                    EtapaArquivoEnum.DOWNLOAD_REMOTO_SFTP_CONSINCO,
                    downloadOk ? StatusExecucaoEnum.SUCESSO : StatusExecucaoEnum.FALHA,
                    (remoto != null ? String.valueOf(remoto) : null),
                    (local != null ? String.valueOf(local) : null),
                    downloadOk ? "Download OK" : "Download falhou",
                    downloadOk ? null : "Download falhou",
                    null, tamanhoLocal, lastModRemoto, lastModLocal);

            if (fsUsado) {
                salvarEtapa(ra,
                        EtapaArquivoEnum.COPIA_DESTINO_MGV_FS,
                        fsOk ? StatusExecucaoEnum.SUCESSO : StatusExecucaoEnum.FALHA,
                        (local != null ? String.valueOf(local) : null),
                        cfg.getCaminhoFsDestino(),
                        fsOk ? "Cópia FS OK" : "Cópia FS falhou",
                        fsOk ? null : "FS falhou",
                        tamanhoLocal, null, lastModLocal, null);
            } else {
                salvarEtapa(ra,
                        EtapaArquivoEnum.COPIA_DESTINO_MGV_FS,
                        StatusExecucaoEnum.SUCESSO,
                        (local != null ? String.valueOf(local) : null),
                        null,
                        "FS não configurado (pulado).",
                        null,
                        null, null, null, null);
            }

            if (smbUsado) {
                String smbDestino = "\\\\" + cfg.getSmbServidor() + "\\" + cfg.getSmbCompartilhamento();
                salvarEtapa(ra,
                        EtapaArquivoEnum.COPIA_DESTINO_MGV_SMB,
                        smbOk ? StatusExecucaoEnum.SUCESSO : StatusExecucaoEnum.FALHA,
                        (local != null ? String.valueOf(local) : null),
                        smbDestino,
                        smbOk ? "Cópia SMB OK" : "Cópia SMB falhou",
                        smbOk ? null : "SMB falhou",
                        tamanhoLocal, null, lastModLocal, null);
            } else {
                salvarEtapa(ra,
                        EtapaArquivoEnum.COPIA_DESTINO_MGV_SMB,
                        StatusExecucaoEnum.SUCESSO,
                        (local != null ? String.valueOf(local) : null),
                        null,
                        "SMB não configurado (pulado).",
                        null,
                        null, null, null, null);
            }

            salvarEtapa(ra,
                    EtapaArquivoEnum.VALIDACAO_ARQUIVOS,
                    statusPrincipal,
                    (remoto != null ? String.valueOf(remoto) : null),
                    (local != null ? String.valueOf(local) : null),
                    msgData != null ? msgData : "Validação OK",
                    null,
                    null, null, lastModRemoto, lastModLocal);

            execArquivoRepo.save(ra);
        }
    }

    // =========================
    // Matching de ArquivoInfo por pattern
    // =========================

    private static ArquivoInfo escolherInfoParaRegistro(RotinaExecucaoArquivo ra,
                                                       List<ArquivoInfo> infos,
                                                       Set<Integer> usados,
                                                       int idxFallback) {
        if (infos == null || infos.isEmpty()) return null;

        String pattern = (ra != null ? ra.getPatternEsperado() : null);

        // 1) tenta casar pelo patternEsperado
        if (pattern != null && !pattern.isBlank()) {
            for (int i = 0; i < infos.size(); i++) {
                if (usados.contains(i)) continue;
                ArquivoInfo inf = infos.get(i);
                if (inf == null || inf.getNomeArquivo() == null) continue;

                if (matchesPattern(pattern, inf.getNomeArquivo())) {
                    usados.add(i);
                    return inf;
                }
            }
        }

        // 2) fallback por índice
        if (idxFallback >= 0 && idxFallback < infos.size() && !usados.contains(idxFallback)) {
            usados.add(idxFallback);
            return infos.get(idxFallback);
        }

        // 3) pega o primeiro livre
        for (int i = 0; i < infos.size(); i++) {
            if (!usados.contains(i)) {
                usados.add(i);
                return infos.get(i);
            }
        }

        return null;
    }

    private static boolean matchesPattern(String pattern, String filename) {
        if (pattern == null || filename == null) return false;

        String p = pattern.trim();
        String f = filename.trim();

        // equals simples
        if (!p.contains("*") && !p.contains("?") && !looksLikeRegex(p)) {
            return p.equalsIgnoreCase(f);
        }

        // glob
        if (p.contains("*") || p.contains("?")) {
            try {
                return FileSystems.getDefault()
                        .getPathMatcher("glob:" + p)
                        .matches(Paths.get(f));
            } catch (Exception ignore) { }
        }

        // regex
        try {
            return Pattern.compile(p, Pattern.CASE_INSENSITIVE).matcher(f).matches();
        } catch (Exception ignore) { }

        // fallback contains
        return f.toLowerCase().contains(p.toLowerCase());
    }

    private static boolean looksLikeRegex(String s) {
        if (s == null) return false;
        return s.contains(".*") || s.contains("^") || s.contains("$") || s.contains("\\d") || s.contains("[") || s.contains("(");
    }

    private static Path localizarLocalPorNome(String nomeArquivo, List<Path> locais) {
        if (nomeArquivo == null || locais == null || locais.isEmpty()) return null;

        for (Path p : locais) {
            try {
                if (p != null && p.getFileName() != null
                        && p.getFileName().toString().equalsIgnoreCase(nomeArquivo)) {
                    return p;
                }
            } catch (Exception ignore) { }
        }
        return null;
    }

    private static boolean statusOk(String status) {
        if (status == null) return false;
        return "OK".equalsIgnoreCase(status) || "PULADO".equalsIgnoreCase(status);
    }

    private StatusExecucaoEnum calcularStatusPrincipal(boolean downloadOk,
                                                      boolean fsUsado,
                                                      boolean smbUsado,
                                                      boolean fsOkArquivo,
                                                      boolean smbOkArquivo) {

        if (!downloadOk) return StatusExecucaoEnum.FALHA;

        boolean falhaDestino = (fsUsado && !fsOkArquivo) || (smbUsado && !smbOkArquivo);
        return falhaDestino ? StatusExecucaoEnum.FALHA_PARCIAL : StatusExecucaoEnum.SUCESSO;
    }

    private void salvarEtapa(RotinaExecucaoArquivo ra,
                             EtapaArquivoEnum etapa,
                             StatusExecucaoEnum status,
                             String origem,
                             String destino,
                             String mensagem,
                             String erro,
                             Long tamOrigem,
                             Long tamDestino,
                             LocalDateTime lmOrigem,
                             LocalDateTime lmDestino) {

        try {
            RotinaExecucaoArquivoEtapa e = new RotinaExecucaoArquivoEtapa();
            e.setExecucaoArquivo(ra);
            e.setEtapa(etapa);
            e.setStatus(status != null ? status : StatusExecucaoEnum.FALHA);

            LocalDateTime agora = LocalDateTime.now();
            e.setInicioEm(agora);
            e.setFimEm(agora);
            e.setTempoTotalMs(0L);

            e.setOrigem(origem);
            e.setDestino(destino);

            e.setTamanhoOrigemBytes(tamOrigem);
            e.setTamanhoDestinoBytes(tamDestino);
            e.setLastModifiedOrigem(lmOrigem);
            e.setLastModifiedDestino(lmDestino);

            e.setMensagem(mensagem);
            e.setErro(erro != null ? trunc(erro, DB_ERR_MAX) : null);

            etapaRepo.save(e);
        } catch (Exception ex) {
            LOG.debug("[MGV][ETAPA] Falha ao salvar etapa (ignorada). execucaoArquivoId={} etapa={} msg={}",
                    (ra != null ? ra.getExecucaoArquivoId() : null),
                    etapa,
                    ex.getMessage());
        }
    }

    private static String mergeMsg(String a, String b) {
        if (a == null || a.isBlank()) return b;
        if (b == null || b.isBlank()) return a;
        return a + "\n" + b;
    }

    private boolean getBoolean(Object target, String... getters) {
        Object v = getObject(target, getters);
        return (v instanceof Boolean) ? (Boolean) v : false;
    }

    private Object getObject(Object target, String... getters) {
        if (target == null) return null;
        for (String g : getters) {
            try {
                var m = target.getClass().getMethod(g);
                return m.invoke(target);
            } catch (Exception ignore) { }
        }
        return null;
    }

    private LocalDateTime extrairLastModified(Object result) {
        Object v = getObject(
                result,
                "getLastModifiedRemoto",
                "getRemoteLastModified",
                "getLastModifiedOrigem",
                "getLastModifiedRemote",
                "getMtimeRemote",
                "getRemoteMtime"
        );

        if (v == null) return null;
        if (v instanceof LocalDateTime) return (LocalDateTime) v;

        if (v instanceof Date) {
            return LocalDateTime.ofInstant(((Date) v).toInstant(), ZoneId.systemDefault());
        }

        if (v instanceof Long) {
            return LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli((Long) v), ZoneId.systemDefault());
        }

        return null;
    }

    private StatusExecucaoEnum calcularStatusLoja(List<RotinaExecucaoArquivo> regs) {
        if (regs == null || regs.isEmpty()) return StatusExecucaoEnum.FALHA;

        boolean algumFalhou = regs.stream().anyMatch(r -> r.getStatus() == StatusExecucaoEnum.FALHA);
        boolean algumParcial = regs.stream().anyMatch(r -> r.getStatus() == StatusExecucaoEnum.FALHA_PARCIAL);

        if (algumFalhou) return StatusExecucaoEnum.FALHA;
        if (algumParcial) return StatusExecucaoEnum.FALHA_PARCIAL;
        return StatusExecucaoEnum.SUCESSO;
    }

    private StatusExecucaoEnum calcularStatusFinal(List<StatusExecucaoEnum> statusLojas) {
        if (statusLojas == null || statusLojas.isEmpty()) return StatusExecucaoEnum.FALHA;

        boolean allOk = statusLojas.stream().allMatch(s -> s == StatusExecucaoEnum.SUCESSO);
        if (allOk) return StatusExecucaoEnum.SUCESSO;

        boolean allFail = statusLojas.stream().allMatch(s -> s == StatusExecucaoEnum.FALHA);
        if (allFail) return StatusExecucaoEnum.FALHA;

        return StatusExecucaoEnum.FALHA_PARCIAL;
    }

    private String montarResumo(StatusExecucaoEnum statusFinal, List<StatusExecucaoEnum> statusLojas, int ignoradasSemConfig) {
        long ok = statusLojas.stream().filter(s -> s == StatusExecucaoEnum.SUCESSO).count();
        long falha = statusLojas.stream().filter(s -> s == StatusExecucaoEnum.FALHA).count();
        long parcial = statusLojas.stream().filter(s -> s == StatusExecucaoEnum.FALHA_PARCIAL).count();

        String base = "Execução MGV: " + statusFinal + " | OK=" + ok + " | FALHA_PARCIAL=" + parcial + " | FALHA=" + falha;
        if (ignoradasSemConfig > 0) {
            base += " | IGNORADAS_SEM_CONFIG=" + ignoradasSemConfig;
        }
        return base;
    }

    private String resumoLoja(List<RotinaExecucaoArquivo> regs, StatusExecucaoEnum statusLoja) {
        long desatualizados = regs.stream().filter(r -> Boolean.FALSE.equals(r.getOrigemAtualizada())).count();
        if (desatualizados > 0) {
            return "Status=" + statusLoja + " | Arquivos desatualizados=" + desatualizados;
        }
        return "Status=" + statusLoja;
    }

    private List<Loja> resolverLojas(List<Long> lojaIds) {
        if (lojaIds == null || lojaIds.isEmpty()) {
            return new ArrayList<>(lojaService.getAllLojas());
        }

        List<Loja> out = new ArrayList<>();
        for (Long id : lojaIds) {
            Loja l = lojaService.findById(id);
            if (l != null) out.add(l);
        }
        return out;
    }

    private ArquivosMgv obterCfgMgvDaLoja(Loja loja) {
        if (loja == null || loja.getLojaId() == null) return null;

        List<ArquivosMgv> all = arquivosMgvService.findAll();
        if (all == null) return null;

        return all.stream()
                .filter(x -> x != null && x.getLoja() != null
                        && Objects.equals(x.getLoja().getLojaId(), loja.getLojaId()))
                .findFirst()
                .orElse(null);
    }

    // =========================
    // Helpers safe (para log)
    // =========================
    private static String nz(String v) {
        return (v == null || v.isBlank()) ? "-" : v;
    }

    private static Object safeLojaId(Loja l) {
        try { return (l != null ? l.getLojaId() : null); } catch (Exception e) { return null; }
    }

    private static String safeCodLoja(Loja l) {
        try { return (l != null ? l.getCodLojaRms() : null); } catch (Exception e) { return null; }
    }

    private static String safeNomeLoja(Loja l) {
        try { return (l != null ? l.getNome() : null); } catch (Exception e) { return null; }
    }

    private static String trunc(String s, int max) {
        if (s == null) return null;
        if (max <= 0) return "";
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(0, max - 20)) + "\n... (truncado)";
    }

    private static String stackTrace(Throwable t) {
        if (t == null) return null;
        try {
            StringWriter sw = new StringWriter(4096);
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            pw.flush();
            return trunc(sw.toString(), DB_ERR_MAX);
        } catch (Exception e) {
            return trunc(String.valueOf(t.getMessage()), DB_ERR_MAX);
        }
    }
}