package sistema.rotinas.primefaces.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

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

            LOG.info("[MGV][LOJA] Config encontrada. execucaoId={} execucaoLojaId={} mgvId={} tipoDestino={} moverRemotoAposCopia={} dirProcessed={}",
                    execucaoId,
                    execucaoLojaId,
                    cfg.getMgvId(),
                    (cfg.getTipoDestino() != null ? cfg.getTipoDestino().name() : null),
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
     * ✅ AJUSTE PRINCIPAL:
     * Se o result for MgvTestResult, preenche por índice:
     * - nome do arquivo (por arquivo)
     * - lastModified (por arquivo)
     * - origemAtualizada (por arquivo)
     *
     * Assim o e-mail deixa de mostrar "Data: -".
     */
    private void preencherResultadoNosRegistros(List<RotinaExecucaoArquivo> regs,
                                               RotinaExecucao execucao,
                                               ArquivosMgv cfg,
                                               Object result) {

        boolean remoteOk;
        boolean downloadOk;
        boolean fsOk;
        boolean smbOk;

        boolean destinoEhFs = (cfg.getTipoDestino() == ArquivosMgv.TipoDestino.FS);
        boolean destinoEhSmb = (cfg.getTipoDestino() == ArquivosMgv.TipoDestino.SMB);

        boolean fsConfigurado = cfg.getCaminhoFsDestino() != null && !cfg.getCaminhoFsDestino().isBlank();
        boolean smbConfigurado =
                cfg.getSmbServidor() != null && !cfg.getSmbServidor().isBlank() &&
                cfg.getSmbCompartilhamento() != null && !cfg.getSmbCompartilhamento().isBlank() &&
                cfg.getSmbUsuario() != null && !cfg.getSmbUsuario().isBlank();

        // ✅ caso típico agora
        if (result instanceof MgvTestResult r) {

            remoteOk = r.isSftpOk();
            downloadOk = r.isDownloadOk();
            fsOk = r.isFsOk();
            smbOk = r.isSmbOk();

            StatusExecucaoEnum statusPrincipal = calcularStatusPrincipal(downloadOk, destinoEhFs, destinoEhSmb, fsConfigurado, smbConfigurado, fsOk, smbOk);

            LocalDate execDate = (execucao.getInicioEm() != null ? execucao.getInicioEm().toLocalDate() : LocalDate.now());
            LocalDateTime fim = LocalDateTime.now();

            List<ArquivoInfo> infos = (r.getArquivosInfo() != null ? r.getArquivosInfo() : new ArrayList<>());
            List<String> remotos = (r.getArquivosRemotos() != null ? r.getArquivosRemotos() : new ArrayList<>());
            List<Path> locais = (r.getArquivosLocais() != null ? r.getArquivosLocais() : new ArrayList<>());

            LOG.info("[MGV][RESULT] (typed) remoteOk={} downloadOk={} tipoDestino={} fsCfg={} fsOk={} smbCfg={} smbOk={} totalInfos={} totalRemotos={} totalLocais={} statusPrincipal={}",
                    remoteOk, downloadOk,
                    (cfg.getTipoDestino() != null ? cfg.getTipoDestino().name() : null),
                    fsConfigurado, fsOk, smbConfigurado, smbOk,
                    infos.size(), remotos.size(), locais.size(),
                    statusPrincipal);

            // ✅ Preenche 1:1 com os registros (patterns)
            for (int i = 0; i < regs.size(); i++) {
                RotinaExecucaoArquivo ra = regs.get(i);

                ra.setFimEm(fim);
                if (ra.getInicioEm() != null) {
                    ra.setTempoTotalMs(Duration.between(ra.getInicioEm(), fim).toMillis());
                }

                ra.setEtapa(EtapaArquivoEnum.VALIDACAO_ARQUIVOS);
                ra.setStatus(statusPrincipal);

                // tenta casar por índice: primeiro infos, depois listas
                ArquivoInfo info = (i < infos.size() ? infos.get(i) : null);

                String nomeArquivo = null;
                LocalDateTime lastMod = null;
                Boolean atualizado = null;

                if (info != null) {
                    nomeArquivo = info.getNomeArquivo();
                    lastMod = info.getLastModified();
                    atualizado = info.getAtualizado();
                } else if (i < remotos.size()) {
                    nomeArquivo = remotos.get(i);
                }

                Path localPath = (i < locais.size() ? locais.get(i) : null);

                // origem/destino/nome
                if (nomeArquivo != null && !nomeArquivo.isBlank()) {
                    ra.setOrigem(nomeArquivo);
                    if (ra.getNomeArquivo() == null || ra.getNomeArquivo().isBlank()) {
                        ra.setNomeArquivo(nomeArquivo);
                    }
                }

                if (localPath != null) {
                    ra.setDestino(String.valueOf(localPath));
                    if ((ra.getNomeArquivo() == null || ra.getNomeArquivo().isBlank())) {
                        ra.setNomeArquivo(localPath.getFileName().toString());
                    }
                }

                // lastModified por arquivo (✅ crucial pro e-mail)
                ra.setLastModifiedOrigem(lastMod);

                LocalDateTime lastModLocal = null;
                Long tamanhoLocal = null;
                try {
                    if (localPath != null && Files.exists(localPath)) {
                        tamanhoLocal = Files.size(localPath);
                        lastModLocal = LocalDateTime.ofInstant(
                                Files.getLastModifiedTime(localPath).toInstant(), ZoneId.systemDefault());
                    }
                } catch (Exception ignore) {}

                ra.setLastModifiedDestino(lastModLocal);

                // atualizado por arquivo
                if (atualizado == null && lastMod != null) {
                    atualizado = lastMod.toLocalDate().isEqual(execDate);
                }
                ra.setOrigemAtualizada(atualizado);

                if (Boolean.FALSE.equals(atualizado)) {
                    ra.setMensagem(mergeMsg(ra.getMensagem(),
                            "Arquivo desatualizado (lastModified remoto diferente do dia da execução)."));
                }

                // ===== Etapas detalhadas (mesma lógica p/ todos) =====

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
                        (nomeArquivo != null ? nomeArquivo : null),
                        (localPath != null ? String.valueOf(localPath) : null),
                        downloadOk ? "Download OK" : "Download falhou",
                        downloadOk ? null : "Download falhou",
                        null,
                        tamanhoLocal,
                        lastMod,
                        lastModLocal);

                if (destinoEhFs) {
                    if (!fsConfigurado) {
                        salvarEtapa(ra,
                                EtapaArquivoEnum.COPIA_DESTINO_MGV_FS,
                                StatusExecucaoEnum.FALHA,
                                (localPath != null ? String.valueOf(localPath) : null),
                                null,
                                "Destino FS selecionado, mas Caminho FS não está configurado.",
                                "Caminho FS destino vazio.",
                                tamanhoLocal,
                                null,
                                lastModLocal,
                                null);
                    } else {
                        salvarEtapa(ra,
                                EtapaArquivoEnum.COPIA_DESTINO_MGV_FS,
                                fsOk ? StatusExecucaoEnum.SUCESSO : StatusExecucaoEnum.FALHA,
                                (localPath != null ? String.valueOf(localPath) : null),
                                cfg.getCaminhoFsDestino(),
                                fsOk ? "Cópia FS OK" : "Cópia FS falhou",
                                fsOk ? null : "FS falhou",
                                tamanhoLocal,
                                null,
                                lastModLocal,
                                null);
                    }
                } else if (destinoEhSmb) {
                    if (!smbConfigurado) {
                        salvarEtapa(ra,
                                EtapaArquivoEnum.COPIA_DESTINO_MGV_SMB,
                                StatusExecucaoEnum.FALHA,
                                (localPath != null ? String.valueOf(localPath) : null),
                                null,
                                "Destino SMB selecionado, mas configuração SMB está incompleta.",
                                "SMB: servidor/compartilhamento/usuário obrigatórios.",
                                tamanhoLocal,
                                null,
                                lastModLocal,
                                null);
                    } else {
                        String smbDestino = "\\\\" + cfg.getSmbServidor() + "\\" + cfg.getSmbCompartilhamento();
                        salvarEtapa(ra,
                                EtapaArquivoEnum.COPIA_DESTINO_MGV_SMB,
                                smbOk ? StatusExecucaoEnum.SUCESSO : StatusExecucaoEnum.FALHA,
                                (localPath != null ? String.valueOf(localPath) : null),
                                smbDestino,
                                smbOk ? "Cópia SMB OK" : "Cópia SMB falhou",
                                smbOk ? null : "SMB falhou",
                                tamanhoLocal,
                                null,
                                lastModLocal,
                                null);
                    }
                }

                salvarEtapa(ra,
                        EtapaArquivoEnum.VALIDACAO_ARQUIVOS,
                        statusPrincipal,
                        (nomeArquivo != null ? nomeArquivo : null),
                        (localPath != null ? String.valueOf(localPath) : null),
                        Boolean.FALSE.equals(atualizado)
                                ? "Arquivo desatualizado (lastModified remoto diferente do dia da execução)."
                                : "Validação OK",
                        null,
                        null,
                        null,
                        lastMod,
                        lastModLocal);

                execArquivoRepo.save(ra);
            }

            return; // ✅ não cai no modo reflexivo
        }

        // =========================
        // fallback antigo (reflexivo)
        // =========================
        remoteOk = getBoolean(result, "isSftpOk", "getSftpOk", "isRemoteOk", "getRemoteOk");
        downloadOk = getBoolean(result, "isDownloadOk", "getDownloadOk");
        fsOk = getBoolean(result, "isFsOk", "getFsOk");
        smbOk = getBoolean(result, "isSmbOk", "getSmbOk");

        Object remoto = getObject(result, "getArquivoRemoto", "getNomeArquivoRemoto", "getRemoteFile");
        Object local  = getObject(result, "getArquivoLocal", "getLocalFile", "getPathLocal");

        LocalDateTime lastModRemoto = extrairLastModified(result);
        LocalDate execDate = (execucao.getInicioEm() != null ? execucao.getInicioEm().toLocalDate() : LocalDate.now());

        Boolean atualizado = null;
        if (lastModRemoto != null) {
            atualizado = lastModRemoto.toLocalDate().isEqual(execDate);
        }

        StatusExecucaoEnum statusPrincipal = calcularStatusPrincipal(downloadOk, destinoEhFs, destinoEhSmb, fsConfigurado, smbConfigurado, fsOk, smbOk);

        LOG.info("[MGV][RESULT] (fallback) remoteOk={} downloadOk={} tipoDestino={} fsCfg={} fsOk={} smbCfg={} smbOk={} lastModRemoto={} atualizado={} remoto={} local={} statusPrincipal={}",
                remoteOk, downloadOk,
                (cfg.getTipoDestino() != null ? cfg.getTipoDestino().name() : null),
                fsConfigurado, fsOk, smbConfigurado, smbOk,
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

            if (destinoEhFs) {
                if (!fsConfigurado) {
                    salvarEtapa(ra, EtapaArquivoEnum.COPIA_DESTINO_MGV_FS, StatusExecucaoEnum.FALHA,
                            (local != null ? String.valueOf(local) : null),
                            null,
                            "Destino FS selecionado, mas Caminho FS não está configurado.",
                            "Caminho FS destino vazio.",
                            tamanhoLocal, null, lastModLocal, null);
                } else {
                    salvarEtapa(ra, EtapaArquivoEnum.COPIA_DESTINO_MGV_FS, fsOk ? StatusExecucaoEnum.SUCESSO : StatusExecucaoEnum.FALHA,
                            (local != null ? String.valueOf(local) : null),
                            cfg.getCaminhoFsDestino(),
                            fsOk ? "Cópia FS OK" : "Cópia FS falhou",
                            fsOk ? null : "FS falhou",
                            tamanhoLocal, null, lastModLocal, null);
                }
            } else if (destinoEhSmb) {
                if (!smbConfigurado) {
                    salvarEtapa(ra, EtapaArquivoEnum.COPIA_DESTINO_MGV_SMB, StatusExecucaoEnum.FALHA,
                            (local != null ? String.valueOf(local) : null),
                            null,
                            "Destino SMB selecionado, mas configuração SMB está incompleta.",
                            "SMB: servidor/compartilhamento/usuário obrigatórios.",
                            tamanhoLocal, null, lastModLocal, null);
                } else {
                    String smbDestino = "\\\\" + cfg.getSmbServidor() + "\\" + cfg.getSmbCompartilhamento();
                    salvarEtapa(ra, EtapaArquivoEnum.COPIA_DESTINO_MGV_SMB, smbOk ? StatusExecucaoEnum.SUCESSO : StatusExecucaoEnum.FALHA,
                            (local != null ? String.valueOf(local) : null),
                            smbDestino,
                            smbOk ? "Cópia SMB OK" : "Cópia SMB falhou",
                            smbOk ? null : "SMB falhou",
                            tamanhoLocal, null, lastModLocal, null);
                }
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

    private StatusExecucaoEnum calcularStatusPrincipal(boolean downloadOk,
                                                      boolean destinoEhFs,
                                                      boolean destinoEhSmb,
                                                      boolean fsConfigurado,
                                                      boolean smbConfigurado,
                                                      boolean fsOk,
                                                      boolean smbOk) {

        if (!downloadOk) return StatusExecucaoEnum.FALHA;

        boolean falhaDestinoEfetivo = false;

        if (destinoEhFs) {
            if (!fsConfigurado) falhaDestinoEfetivo = true;
            else falhaDestinoEfetivo = !fsOk;
        } else if (destinoEhSmb) {
            if (!smbConfigurado) falhaDestinoEfetivo = true;
            else falhaDestinoEfetivo = !smbOk;
        }

        return falhaDestinoEfetivo ? StatusExecucaoEnum.FALHA_PARCIAL : StatusExecucaoEnum.SUCESSO;
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

    @SuppressWarnings("unchecked")
    private List<String> getStringList(Object target, String... getters) {
        Object v = getObject(target, getters);
        if (v instanceof List) {
            List<?> list = (List<?>) v;
            List<String> out = new ArrayList<>();
            for (Object o : list) {
                if (o != null) out.add(String.valueOf(o));
            }
            return out;
        }
        return new ArrayList<>();
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