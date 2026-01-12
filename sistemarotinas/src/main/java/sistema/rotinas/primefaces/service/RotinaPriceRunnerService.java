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

import sistema.rotinas.primefaces.enums.EtapaArquivoEnum;
import sistema.rotinas.primefaces.enums.OrigemExecucaoEnum;
import sistema.rotinas.primefaces.enums.StatusExecucaoEnum;
import sistema.rotinas.primefaces.enums.TipoRotinaEnum;
import sistema.rotinas.primefaces.model.ArquivosPrice;
import sistema.rotinas.primefaces.model.ArquivosPricePattern;
import sistema.rotinas.primefaces.model.Loja;
import sistema.rotinas.primefaces.model.rotina.RotinaExecucao;
import sistema.rotinas.primefaces.model.rotina.RotinaExecucaoArquivo;
import sistema.rotinas.primefaces.model.rotina.RotinaExecucaoArquivoEtapa;
import sistema.rotinas.primefaces.model.rotina.RotinaExecucaoLoja;
import sistema.rotinas.primefaces.repository.RotinaExecucaoArquivoEtapaRepository;
import sistema.rotinas.primefaces.repository.RotinaExecucaoArquivoRepository;
import sistema.rotinas.primefaces.service.interfaces.IArquivosPricePatternService;
import sistema.rotinas.primefaces.service.interfaces.IArquivosPriceService;
import sistema.rotinas.primefaces.service.interfaces.ILojaService;
import sistema.rotinas.primefaces.service.interfaces.IRotinaExecucaoService;
import sistema.rotinas.primefaces.service.interfaces.IRotinaPriceRunnerService;

@Service
public class RotinaPriceRunnerService implements IRotinaPriceRunnerService {

    /**
     * ✅ Logger dedicado (para usar appender específico no logback)
     * logback: <logger name="ROTINA_PRICE" .../>
     */
    private static final Logger LOG = LoggerFactory.getLogger("ROTINA_PRICE");

    /**
     * ✅ Limite seguro para gravar erro no banco (evita Data truncation).
     * Ajuste se sua coluna for maior/menor.
     */
    private static final int DB_ERR_MAX = 2000;

    @Autowired private IRotinaExecucaoService execucaoService;
    @Autowired private ILojaService lojaService;
    @Autowired private IArquivosPriceService arquivosPriceService;
    @Autowired private IArquivosPricePatternService patternService;

    @Autowired private RotinaExecucaoArquivoRepository execArquivoRepo;

    // ✅ repo de etapas
    @Autowired private RotinaExecucaoArquivoEtapaRepository etapaRepo;

    @Autowired private PriceTransferService priceTransferService;

    // ✅ notificação (dispara após finalizar execução)
    @Autowired private NotificacaoRotinaService notificacaoRotinaService;

    @Override
    public Long executar(List<Long> lojaIds, OrigemExecucaoEnum origem, String solicitante) {

        LOG.info("[PRICE][RUNNER] Iniciando. origem={} solicitante={} lojasSelecionadas={}",
                origem, nz(solicitante), (lojaIds == null || lojaIds.isEmpty()) ? "TODAS" : lojaIds);

        RotinaExecucao execucao = execucaoService.iniciarExecucao(TipoRotinaEnum.PRICE, origem, solicitante);
        Long execucaoId = execucao != null ? execucao.getExecucaoId() : null;

        LOG.info("[PRICE][RUNNER] Execução iniciada. execucaoId={}", execucaoId);

        List<StatusExecucaoEnum> statusLojas = new ArrayList<>();
        int lojasIgnoradasSemConfig = 0;

        try {
            List<Loja> lojasBase = resolverLojas(lojaIds);

            LOG.info("[PRICE][RUNNER] Lojas base resolvidas. execucaoId={} totalBase={}",
                    execucaoId, (lojasBase != null ? lojasBase.size() : 0));

            if (lojasBase == null || lojasBase.isEmpty()) {
                LOG.warn("[PRICE][RUNNER] Nenhuma loja encontrada para executar. execucaoId={}", execucaoId);
                execucaoService.finalizarExecucao(execucaoId, StatusExecucaoEnum.FALHA,
                        "Nenhuma loja encontrada para executar.", "Lista de lojas vazia.");
                return execucaoId;
            }

            // ✅ executa somente lojas com configuração ArquivosPrice válida
            List<Loja> lojas = new ArrayList<>();
            for (Loja loja : lojasBase) {
                ArquivosPrice cfg = obterCfgPriceDaLoja(loja);
                boolean cfgOk = (cfg != null && cfg.getPriceId() != null);
                if (cfgOk) {
                    lojas.add(loja);
                } else {
                    lojasIgnoradasSemConfig++;
                    LOG.warn("[PRICE][RUNNER] Loja ignorada (sem configuração ArquivosPrice). execucaoId={} lojaId={} codLojaRms={} nome={}",
                            execucaoId,
                            safeLojaId(loja),
                            safeCodLoja(loja),
                            safeNomeLoja(loja));
                }
            }

            if (lojas.isEmpty()) {
                String detalhe = "Nenhuma loja com configuração ArquivosPrice válida. Ignoradas=" + lojasIgnoradasSemConfig;
                LOG.warn("[PRICE][RUNNER] Nenhuma loja com configuração PRICE para executar. execucaoId={} {}",
                        execucaoId, detalhe);

                execucaoService.finalizarExecucao(execucaoId, StatusExecucaoEnum.FALHA,
                        "Nenhuma loja com configuração PRICE para executar.", detalhe);
                return execucaoId;
            }

            LOG.info("[PRICE][RUNNER] Lojas elegíveis. execucaoId={} totalElegiveis={} ignoradasSemConfig={}",
                    execucaoId, lojas.size(), lojasIgnoradasSemConfig);

            for (Loja loja : lojas) {
                StatusExecucaoEnum statusLoja = executarLoja(execucao, loja);
                statusLojas.add(statusLoja);
            }

            StatusExecucaoEnum statusFinal = calcularStatusFinal(statusLojas);
            String resumo = montarResumo(statusFinal, statusLojas, lojasIgnoradasSemConfig);

            LOG.info("[PRICE][RUNNER] Finalizando execução. execucaoId={} statusFinal={} resumo={}",
                    execucaoId, statusFinal, resumo);

            execucaoService.finalizarExecucao(execucaoId, statusFinal, resumo, null);

        } catch (Exception e) {

            LOG.error("[PRICE][RUNNER] Falha geral. execucaoId={} msg={}", execucaoId, e.getMessage(), e);

            execucaoService.finalizarExecucao(execucaoId, StatusExecucaoEnum.FALHA,
                    "Falha ao executar rotina PRICE", stackTrace(e));

        } finally {
            // ✅ Dispara e-mail após concluir (não pode derrubar a execução)
            try {
                if (notificacaoRotinaService != null && execucaoId != null) {
                    LOG.info("[PRICE][RUNNER] Disparando notificação por e-mail. execucaoId={}", execucaoId);
                    notificacaoRotinaService.notificarFinalizacaoRotinaPrice(execucaoId);
                } else {
                    LOG.warn("[PRICE][RUNNER] Notificação não disparada (service null ou execucaoId null). execucaoId={}", execucaoId);
                }
            } catch (Exception ex) {
                LOG.warn("[PRICE][RUNNER] Falha ao enviar e-mail de notificação. execucaoId={} msg={}",
                        execucaoId, ex.getMessage(), ex);
            }
        }

        return execucaoId;
    }

    private StatusExecucaoEnum executarLoja(RotinaExecucao execucao, Loja loja) {

        Long execucaoId = execucao != null ? execucao.getExecucaoId() : null;

        LOG.info("[PRICE][LOJA] Iniciando. execucaoId={} lojaId={} codLojaRms={} nome={}",
                execucaoId, safeLojaId(loja), safeCodLoja(loja), safeNomeLoja(loja));

        RotinaExecucaoLoja el = execucaoService.iniciarLoja(execucaoId, loja);
        Long execucaoLojaId = (el != null ? el.getExecucaoLojaId() : null);

        try {
            ArquivosPrice cfg = obterCfgPriceDaLoja(loja);

            if (cfg == null || cfg.getPriceId() == null) {
                LOG.warn("[PRICE][LOJA] Configuração PRICE ausente (inesperado). execucaoId={} execucaoLojaId={} lojaId={} codLojaRms={}",
                        execucaoId, execucaoLojaId, safeLojaId(loja), safeCodLoja(loja));

                execucaoService.finalizarLoja(execucaoLojaId, StatusExecucaoEnum.FALHA,
                        "Sem configuração PRICE para a loja", "Cadastre em Cadastro → Arquivos PRICE.");
                return StatusExecucaoEnum.FALHA;
            }

            LOG.info("[PRICE][LOJA] Config encontrada. execucaoId={} execucaoLojaId={} priceId={} tipoDestino={} moverRemotoAposCopia={} dirProcessed={}",
                    execucaoId,
                    execucaoLojaId,
                    cfg.getPriceId(),
                    (cfg.getTipoDestino() != null ? cfg.getTipoDestino().name() : null),
                    cfg.getMoverRemotoAposCopia(),
                    cfg.getDirRemotoProcessed());

            List<RotinaExecucaoArquivo> registros = criarRegistrosArquivos(execucao, el, loja, cfg);

            LOG.info("[PRICE][LOJA] Arquivos registrados. execucaoId={} execucaoLojaId={} totalArquivos={}",
                    execucaoId, execucaoLojaId, (registros != null ? registros.size() : 0));

            Object result = invocarTransferService(cfg);

            LOG.info("[PRICE][LOJA] TransferService retornou. execucaoId={} execucaoLojaId={} resultClass={}",
                    execucaoId, execucaoLojaId, (result != null ? result.getClass().getName() : null));

            preencherResultadoNosRegistros(registros, execucao, cfg, result);

            StatusExecucaoEnum statusLoja = calcularStatusLoja(registros);
            String resumo = resumoLoja(registros, statusLoja);

            LOG.info("[PRICE][LOJA] Finalizando. execucaoId={} execucaoLojaId={} status={} resumo={}",
                    execucaoId, execucaoLojaId, statusLoja, resumo);

            execucaoService.finalizarLoja(execucaoLojaId, statusLoja, resumo, null);
            return statusLoja;

        } catch (Exception e) {

            LOG.error("[PRICE][LOJA] Falha. execucaoId={} execucaoLojaId={} lojaId={} codLojaRms={} msg={}",
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
                                                              ArquivosPrice cfg) {

        List<RotinaExecucaoArquivo> regs = new ArrayList<>();

        List<ArquivosPricePattern> patterns = patternService.listarPorPrice(cfg.getPriceId());
        if (patterns == null) patterns = new ArrayList<>();

        LOG.debug("[PRICE][ARQ] Patterns carregados. execucaoId={} execucaoLojaId={} priceId={} totalPatterns={}",
                (execucao != null ? execucao.getExecucaoId() : null),
                (el != null ? el.getExecucaoLojaId() : null),
                cfg.getPriceId(),
                patterns.size());

        for (ArquivosPricePattern p : patterns) {
            RotinaExecucaoArquivo ra = new RotinaExecucaoArquivo();
            ra.setExecucao(execucao);
            ra.setExecucaoLoja(el);
            ra.setLoja(loja);
            ra.setCodLojaRms(loja.getCodLojaRms());

            ra.setPatternEsperado(p.getPattern());
            ra.setRequired(Boolean.TRUE.equals(p.getRequired()));

            ra.setEtapa(EtapaArquivoEnum.CONEXAO_REMOTA_SFTP_CONSINCO);
            ra.setStatus(StatusExecucaoEnum.EM_ANDAMENTO);

            ra.setInicioEm(LocalDateTime.now());
            regs.add(execArquivoRepo.save(ra));
        }

        // linha extra do .m1
        if (Boolean.TRUE.equals(cfg.getMsgCopyAtivo())) {
            RotinaExecucaoArquivo raM1 = new RotinaExecucaoArquivo();
            raM1.setExecucao(execucao);
            raM1.setExecucaoLoja(el);
            raM1.setLoja(loja);
            raM1.setCodLojaRms(loja.getCodLojaRms());

            raM1.setPatternEsperado("MessageFiles (.m1)");
            raM1.setNomeArquivo(cfg.getMsgFileNomeLocal());
            raM1.setRequired(false);

            raM1.setEtapa(EtapaArquivoEnum.COPIA_MESSAGEFILES_PRICE_LOJA);
            raM1.setStatus(StatusExecucaoEnum.EM_ANDAMENTO);

            raM1.setInicioEm(LocalDateTime.now());
            regs.add(execArquivoRepo.save(raM1));
        }

        return regs;
    }

    private Object invocarTransferService(ArquivosPrice cfg) throws Exception {
        Long priceId = cfg.getPriceId();

        LOG.info("[PRICE][XFER] Invocando PriceTransferService. priceId={}", priceId);

        Object r;
        r = tryInvoke(priceTransferService, "testar", new Class<?>[]{ Long.class }, new Object[]{ priceId });
        if (r != null) return r;

        r = tryInvoke(priceTransferService, "testarTransfer", new Class<?>[]{ Long.class }, new Object[]{ priceId });
        if (r != null) return r;

        r = tryInvoke(priceTransferService, "testarTransferencia", new Class<?>[]{ Long.class }, new Object[]{ priceId });
        if (r != null) return r;

        r = tryInvoke(priceTransferService, "testar", new Class<?>[]{ ArquivosPrice.class }, new Object[]{ cfg });
        if (r != null) return r;

        r = tryInvoke(priceTransferService, "testarTransfer", new Class<?>[]{ ArquivosPrice.class }, new Object[]{ cfg });
        if (r != null) return r;

        throw new IllegalStateException(
                "Não encontrei método compatível no PriceTransferService (testar/testarTransfer/testarTransferencia)."
        );
    }

    private Object tryInvoke(Object target, String method, Class<?>[] paramTypes, Object[] args) {
        try {
            var m = target.getClass().getMethod(method, paramTypes);
            return m.invoke(target, args);
        } catch (NoSuchMethodException nsme) {
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e.getCause() != null ? e.getCause() : e);
        }
    }

    private void preencherResultadoNosRegistros(List<RotinaExecucaoArquivo> regs,
                                               RotinaExecucao execucao,
                                               ArquivosPrice cfg,
                                               Object result) {

        boolean sftpOk = getBoolean(result, "isSftpOk", "getSftpOk");
        boolean downloadOk = getBoolean(result, "isDownloadOk", "getDownloadOk");

        boolean fsUsado = cfg.getCaminhoFsDestino() != null && !cfg.getCaminhoFsDestino().isBlank();

        boolean smbUsado =
                cfg.getSmbServidor() != null && !cfg.getSmbServidor().isBlank() &&
                cfg.getSmbCompartilhamento() != null && !cfg.getSmbCompartilhamento().isBlank() &&
                cfg.getSmbUsuario() != null && !cfg.getSmbUsuario().isBlank();

        boolean fsOk = getBoolean(result, "isFsOk", "getFsOk");
        boolean smbOk = getBoolean(result, "isSmbOk", "getSmbOk");

        Object remoto = getObject(result, "getArquivoRemoto", "getNomeArquivoRemoto", "getRemoteFile");
        Object local  = getObject(result, "getArquivoLocal", "getLocalFile", "getPathLocal");

        LocalDateTime lastModRemoto = extrairLastModified(result);
        LocalDate execDate = (execucao.getInicioEm() != null ? execucao.getInicioEm().toLocalDate() : LocalDate.now());

        Boolean atualizado = null;
        if (lastModRemoto != null) {
            atualizado = lastModRemoto.toLocalDate().isEqual(execDate);
        }

        StatusExecucaoEnum statusPrincipal;
        if (!downloadOk) {
            statusPrincipal = StatusExecucaoEnum.FALHA;
        } else {
            boolean falhaDestino = (fsUsado && !fsOk) || (smbUsado && !smbOk);
            statusPrincipal = falhaDestino ? StatusExecucaoEnum.FALHA_PARCIAL : StatusExecucaoEnum.SUCESSO;
        }

        LOG.info("[PRICE][RESULT] sftpOk={} downloadOk={} fsUsado={} fsOk={} smbUsado={} smbOk={} lastModRemoto={} atualizado={} remoto={} local={} statusPrincipal={}",
                sftpOk, downloadOk, fsUsado, fsOk, smbUsado, smbOk,
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

        List<String> mensagens = getStringList(result, "getMensagens", "getMessages", "getMsgs");

        if (mensagens != null && !mensagens.isEmpty()) {
            LOG.info("[PRICE][RESULT] mensagens({}): {}", mensagens.size(), mensagens);
        } else {
            LOG.info("[PRICE][RESULT] mensagens(0)");
        }

        // =========================
        // ✅ MSG estruturado (se o DTO tiver)
        // =========================
        String msgStatus = getString(result, "getMsgStatus");
        String msgOrigem = getString(result, "getMsgOrigem");
        String msgDestino = getString(result, "getMsgDestino");
        String msgDetalhe = getString(result, "getMsgDetalhe");
        boolean temMsgEstruturado = (msgStatus != null && !msgStatus.isBlank());

        // fallback legado (mensagens)
        boolean msgOk = mensagens.stream().anyMatch(m -> m != null && m.startsWith("MSG OK"));
        boolean msgFalhou = mensagens.stream().anyMatch(m -> m != null && m.startsWith("MSG FALHOU"));
        boolean msgDesativado = mensagens.stream().anyMatch(m -> m != null && (m.contains("msgCopyAtivo=false") || m.contains("cópia desativada")));
        boolean msgPulado = mensagens.stream().anyMatch(m -> m != null && m.startsWith("MSG:"));

        for (RotinaExecucaoArquivo ra : regs) {

            ra.setFimEm(fim);
            if (ra.getInicioEm() != null) {
                ra.setTempoTotalMs(Duration.between(ra.getInicioEm(), fim).toMillis());
            }

            ra.setEtapa(EtapaArquivoEnum.VALIDACAO_ARQUIVOS);

            boolean isM1 = "MessageFiles (.m1)".equalsIgnoreCase(ra.getPatternEsperado());

            if (isM1) {
                StatusExecucaoEnum stMsg;
                String msgInfo;
                String erroMsg = null;

                if (temMsgEstruturado) {

                    // preenche origem/destino do registro
                    if (msgOrigem != null && !msgOrigem.isBlank()) ra.setOrigem(msgOrigem);
                    if (msgDestino != null && !msgDestino.isBlank()) ra.setDestino(msgDestino);

                    if ("OK".equalsIgnoreCase(msgStatus) || "DESATIVADO".equalsIgnoreCase(msgStatus)) {
                        stMsg = StatusExecucaoEnum.SUCESSO;
                        msgInfo = "MSG " + msgStatus + (msgDetalhe != null && !msgDetalhe.isBlank() ? (": " + msgDetalhe) : ".");
                    } else if ("PULADO".equalsIgnoreCase(msgStatus)) {
                        stMsg = StatusExecucaoEnum.FALHA_PARCIAL;
                        msgInfo = "MSG PULADO" + (msgDetalhe != null && !msgDetalhe.isBlank() ? (": " + msgDetalhe) : ".");
                    } else if ("FALHOU".equalsIgnoreCase(msgStatus)) {
                        stMsg = StatusExecucaoEnum.FALHA_PARCIAL;
                        msgInfo = "MSG FALHOU" + (msgDetalhe != null && !msgDetalhe.isBlank() ? (": " + msgDetalhe) : ".");
                        erroMsg = msgDetalhe;
                    } else {
                        stMsg = StatusExecucaoEnum.FALHA_PARCIAL;
                        msgInfo = "MSG INDEFINIDO" + (msgDetalhe != null && !msgDetalhe.isBlank() ? (": " + msgDetalhe) : ".");
                    }

                } else {
                    // ✅ fallback antigo (não remove nada do que funcionava)
                    if (msgDesativado) {
                        stMsg = StatusExecucaoEnum.SUCESSO;
                        msgInfo = "MSG desativado (msgCopyAtivo=false).";
                    } else if (msgFalhou) {
                        stMsg = StatusExecucaoEnum.FALHA_PARCIAL;
                        msgInfo = "MSG falhou (ver mensagens).";
                    } else if (msgOk) {
                        stMsg = StatusExecucaoEnum.SUCESSO;
                        msgInfo = "MSG OK.";
                    } else if (msgPulado) {
                        stMsg = StatusExecucaoEnum.FALHA_PARCIAL;
                        msgInfo = "MSG pulado (config incompleta ou arquivo ausente).";
                    } else {
                        stMsg = StatusExecucaoEnum.FALHA_PARCIAL;
                        msgInfo = "MSG: sem confirmação explícita (ver logs).";
                    }
                }

                ra.setEtapa(EtapaArquivoEnum.COPIA_MESSAGEFILES_PRICE_LOJA);
                ra.setStatus(stMsg);
                ra.setMensagem(mergeMsg(ra.getMensagem(), msgInfo));

                salvarEtapa(ra,
                        EtapaArquivoEnum.COPIA_MESSAGEFILES_PRICE_LOJA,
                        stMsg,
                        (temMsgEstruturado ? msgOrigem : null),
                        (temMsgEstruturado ? msgDestino : null),
                        msgInfo,
                        erroMsg,
                        null,
                        null,
                        null,
                        null);

            } else {
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
                        sftpOk ? StatusExecucaoEnum.SUCESSO : StatusExecucaoEnum.FALHA,
                        null,
                        null,
                        sftpOk ? "Conexão SFTP OK" : "Falha na conexão SFTP",
                        sftpOk ? null : "SFTP falhou",
                        null,
                        null,
                        null,
                        null);

                salvarEtapa(ra,
                        EtapaArquivoEnum.DOWNLOAD_REMOTO_SFTP_CONSINCO,
                        downloadOk ? StatusExecucaoEnum.SUCESSO : StatusExecucaoEnum.FALHA,
                        (remoto != null ? String.valueOf(remoto) : null),
                        (local != null ? String.valueOf(local) : null),
                        downloadOk ? "Download OK" : "Download falhou",
                        downloadOk ? null : "Download falhou",
                        null,
                        tamanhoLocal,
                        lastModRemoto,
                        lastModLocal);

                if (fsUsado) {
                    salvarEtapa(ra,
                            EtapaArquivoEnum.COPIA_DESTINO_PRICE_LOJA,
                            fsOk ? StatusExecucaoEnum.SUCESSO : StatusExecucaoEnum.FALHA,
                            (local != null ? String.valueOf(local) : null),
                            cfg.getCaminhoFsDestino(),
                            fsOk ? "Cópia FS OK" : "Cópia FS falhou",
                            fsOk ? null : "FS falhou",
                            tamanhoLocal,
                            null,
                            lastModLocal,
                            null);
                } else {
                    salvarEtapa(ra,
                            EtapaArquivoEnum.COPIA_DESTINO_PRICE_LOJA,
                            StatusExecucaoEnum.SUCESSO,
                            (local != null ? String.valueOf(local) : null),
                            null,
                            "FS não configurado (pulado).",
                            null,
                            null,
                            null,
                            null,
                            null);
                }

                if (smbUsado) {
                    salvarEtapa(ra,
                            EtapaArquivoEnum.COPIA_DESTINO_PRICE_LOJA,
                            smbOk ? StatusExecucaoEnum.SUCESSO : StatusExecucaoEnum.FALHA,
                            (local != null ? String.valueOf(local) : null),
                            "\\\\" + cfg.getSmbServidor() + "\\" + cfg.getSmbCompartilhamento(),
                            smbOk ? "Cópia SMB OK" : "Cópia SMB falhou",
                            smbOk ? null : "SMB falhou",
                            tamanhoLocal,
                            null,
                            lastModLocal,
                            null);
                } else {
                    salvarEtapa(ra,
                            EtapaArquivoEnum.COPIA_DESTINO_PRICE_LOJA,
                            StatusExecucaoEnum.SUCESSO,
                            (local != null ? String.valueOf(local) : null),
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
                        (remoto != null ? String.valueOf(remoto) : null),
                        (local != null ? String.valueOf(local) : null),
                        msgData != null ? msgData : "Validação OK",
                        null,
                        null,
                        null,
                        lastModRemoto,
                        lastModLocal);
            }

            execArquivoRepo.save(ra);
        }
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

            // ✅ evita Data truncation no banco
            e.setErro(erro != null ? trunc(erro, DB_ERR_MAX) : null);

            etapaRepo.save(e);
        } catch (Exception ex) {
            LOG.debug("[PRICE][ETAPA] Falha ao salvar etapa (ignorada). execucaoArquivoId={} etapa={} msg={}",
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

    private String getString(Object target, String... getters) {
        Object v = getObject(target, getters);
        return (v != null ? String.valueOf(v) : null);
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

    private String montarResumo(StatusExecucaoEnum statusFinal, List<StatusExecucaoEnum> statusLojas) {
        return montarResumo(statusFinal, statusLojas, 0);
    }

    private String montarResumo(StatusExecucaoEnum statusFinal, List<StatusExecucaoEnum> statusLojas, int ignoradasSemConfig) {
        long ok = statusLojas.stream().filter(s -> s == StatusExecucaoEnum.SUCESSO).count();
        long falha = statusLojas.stream().filter(s -> s == StatusExecucaoEnum.FALHA).count();
        long parcial = statusLojas.stream().filter(s -> s == StatusExecucaoEnum.FALHA_PARCIAL).count();

        String base = "Execução PRICE: " + statusFinal + " | OK=" + ok + " | FALHA_PARCIAL=" + parcial + " | FALHA=" + falha;
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

    private ArquivosPrice obterCfgPriceDaLoja(Loja loja) {
        if (loja == null || loja.getLojaId() == null) return null;

        List<ArquivosPrice> all = arquivosPriceService.findAll();
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

    /**
     * ✅ Stack trace completa para persistir no banco (truncada)
     */
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