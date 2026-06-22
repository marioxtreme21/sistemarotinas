package sistema.rotinas.primefaces.service.loyalty;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import sistema.rotinas.primefaces.dto.loyalty.LoyaltyApiResponseDTO;
import sistema.rotinas.primefaces.dto.loyalty.LoyaltyCupomOrigemDTO;
import sistema.rotinas.primefaces.dto.loyalty.LoyaltyCupomPayloadDTO;
import sistema.rotinas.primefaces.enums.OrigemExecucaoEnum;
import sistema.rotinas.primefaces.enums.StatusExecucaoEnum;
import sistema.rotinas.primefaces.model.Loja;
import sistema.rotinas.primefaces.model.loyalty.RotinaExecucaoLoyalty;
import sistema.rotinas.primefaces.model.loyalty.RotinaExecucaoLoyaltyCupom;
import sistema.rotinas.primefaces.model.loyalty.RotinaExecucaoLoyaltyLote;
import sistema.rotinas.primefaces.repository.LojaRepository;
import sistema.rotinas.primefaces.repository.loyalty.LoyaltyVenda144Repository;
import sistema.rotinas.primefaces.repository.loyalty.RotinaExecucaoLoyaltyCupomRepository;
import sistema.rotinas.primefaces.repository.loyalty.RotinaExecucaoLoyaltyRepository;
import sistema.rotinas.primefaces.service.interfaces.loyalty.ILoyaltyExecucaoService;

@Service
public class LoyaltyExecucaoService implements ILoyaltyExecucaoService {

    private static final Logger LOG = LoggerFactory.getLogger("ROTINA_LOYALTY");
    private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final int PROGRESS_STEP_CUPONS_UNITARIO = 100;
    private static final long PROGRESS_STEP_SECONDS_UNITARIO = 15L;

    private static final int PROGRESS_STEP_CUPONS_LOTE = 5000;
    private static final long PROGRESS_STEP_SECONDS_LOTE = 60L;

    // Limites para trocar e-mail detalhado por resumido
    private static final int MAX_CUPONS_NOTIFICACAO_DETALHADA = 50000;
    private static final int MAX_LOTES_NOTIFICACAO_DETALHADA = 500;

    private final LojaRepository lojaRepository;
    private final LoyaltyVenda144Repository loyaltyVenda144Repository;
    private final RotinaExecucaoLoyaltyRepository execucaoRepository;
    private final RotinaExecucaoLoyaltyCupomRepository cupomRepository;
    private final LoyaltyApiClient loyaltyApiClient;
    private final ObjectMapper objectMapper;
    private final LoyaltyHashService loyaltyHashService;
    private final NotificacaoLoyaltyService notificacaoLoyaltyService;
    private final LoyaltyExecucaoPersistenceService execucaoPersistenceService;
    private final LoyaltyLotePersistenceService lotePersistenceService;
    private final LoyaltyCupomPersistenceService cupomPersistenceService;
    private final LoyaltyBatchProperties loyaltyBatchProperties;

    public LoyaltyExecucaoService(LojaRepository lojaRepository,
                                  LoyaltyVenda144Repository loyaltyVenda144Repository,
                                  RotinaExecucaoLoyaltyRepository execucaoRepository,
                                  RotinaExecucaoLoyaltyCupomRepository cupomRepository,
                                  LoyaltyApiClient loyaltyApiClient,
                                  ObjectMapper objectMapper,
                                  LoyaltyHashService loyaltyHashService,
                                  NotificacaoLoyaltyService notificacaoLoyaltyService,
                                  LoyaltyExecucaoPersistenceService execucaoPersistenceService,
                                  LoyaltyLotePersistenceService lotePersistenceService,
                                  LoyaltyCupomPersistenceService cupomPersistenceService,
                                  LoyaltyBatchProperties loyaltyBatchProperties) {
        this.lojaRepository = lojaRepository;
        this.loyaltyVenda144Repository = loyaltyVenda144Repository;
        this.execucaoRepository = execucaoRepository;
        this.cupomRepository = cupomRepository;
        this.loyaltyApiClient = loyaltyApiClient;
        this.objectMapper = objectMapper;
        this.loyaltyHashService = loyaltyHashService;
        this.notificacaoLoyaltyService = notificacaoLoyaltyService;
        this.execucaoPersistenceService = execucaoPersistenceService;
        this.lotePersistenceService = lotePersistenceService;
        this.cupomPersistenceService = cupomPersistenceService;
        this.loyaltyBatchProperties = loyaltyBatchProperties;
    }

    @Override
    public RotinaExecucaoLoyalty executarCargaNormal(List<Long> lojaIds,
                                                     LocalDate dataInicial,
                                                     LocalDate dataFinal,
                                                     boolean selecionarTodas,
                                                     OrigemExecucaoEnum origemExecucao) {
        return executarCarga(lojaIds, dataInicial, dataFinal, selecionarTodas, origemExecucao, false);
    }

    @Override
    public RotinaExecucaoLoyalty executarCargaEmLotes(List<Long> lojaIds,
                                                      LocalDate dataInicial,
                                                      LocalDate dataFinal,
                                                      boolean selecionarTodas,
                                                      OrigemExecucaoEnum origemExecucao) {
        return executarCarga(lojaIds, dataInicial, dataFinal, selecionarTodas, origemExecucao, true);
    }

    private RotinaExecucaoLoyalty executarCarga(List<Long> lojaIds,
                                                LocalDate dataInicial,
                                                LocalDate dataFinal,
                                                boolean selecionarTodas,
                                                OrigemExecucaoEnum origemExecucao,
                                                boolean modoLote) {

        validarPeriodo(dataInicial, dataFinal);

        LocalDateTime inicio = LocalDateTime.now();
        String modo = modoLote ? "LOTE" : "UNITARIO";

        LOG.info("LOYALTY execução iniciada | modo={} origem={} selecionarTodas={} dataInicial={} dataFinal={} lojaIds={}",
                modo, origemExecucao, selecionarTodas, dataInicial, dataFinal, lojaIds);

        if (modoLote) {
            LOG.info("LOYALTY configuração lote | maxCuponsPorLote={}", loyaltyBatchProperties.getMaxCupons());
        }

        RotinaExecucaoLoyalty execucao = execucaoPersistenceService.criarExecucao(
                origemExecucao,
                selecionarTodas,
                dataInicial,
                dataFinal,
                inicio
        );

        LOG.info("LOYALTY execução registrada | execucaoId={}", execucao.getExecucaoLoyaltyId());

        List<Loja> lojas = carregarLojasElegiveis(lojaIds, selecionarTodas);

        LOG.info("LOYALTY lojas elegíveis carregadas | execucaoId={} totalLojas={}",
                execucao.getExecucaoLoyaltyId(), lojas.size());

        int totalLotes = 0;
        int totalConsultados = 0;
        int totalEnviados = 0;
        int totalFalhas = 0;

        try {
            for (Loja loja : lojas) {
                Integer numeroLojaOrigem = parseLojaOrigem(loja.getCodLojaEconect());

                LOG.info("LOYALTY processando loja | execucaoId={} lojaId={} nomeLoja={} codLojaRms={} codLojaEconect={} codLojaOrigem={}",
                        execucao.getExecucaoLoyaltyId(),
                        loja.getLojaId(),
                        loja.getNome(),
                        loja.getCodLojaRms(),
                        loja.getCodLojaEconect(),
                        numeroLojaOrigem);

                if (numeroLojaOrigem == null) {
                    totalFalhas++;
                    LOG.warn("LOYALTY loja ignorada por codLojaEconect inválido | execucaoId={} lojaId={} nomeLoja={} codLojaRms={} codLojaEconect={}",
                            execucao.getExecucaoLoyaltyId(),
                            loja.getLojaId(),
                            loja.getNome(),
                            loja.getCodLojaRms(),
                            loja.getCodLojaEconect());
                    continue;
                }

                for (LocalDate data = dataInicial; !data.isAfter(dataFinal); data = data.plusDays(1)) {
                    totalLotes++;

                    LOG.info("LOYALTY iniciando lote | execucaoId={} lojaId={} nomeLoja={} codLojaRms={} codLojaEconect={} dataMovimento={} modo={}",
                            execucao.getExecucaoLoyaltyId(),
                            loja.getLojaId(),
                            loja.getNome(),
                            loja.getCodLojaRms(),
                            loja.getCodLojaEconect(),
                            data,
                            modo);

                    RotinaExecucaoLoyaltyLote lote = lotePersistenceService.criarLote(
                            execucao.getExecucaoLoyaltyId(),
                            loja.getLojaId(),
                            data,
                            LocalDateTime.now()
                    );

                    LOG.debug("LOYALTY lote criado | execucaoId={} loteId={} lojaId={} nomeLoja={} codLojaRms={} codLojaEconect={} dataMovimento={}",
                            execucao.getExecucaoLoyaltyId(),
                            lote.getLoteId(),
                            loja.getLojaId(),
                            loja.getNome(),
                            loja.getCodLojaRms(),
                            loja.getCodLojaEconect(),
                            data);

                    List<LoyaltyCupomOrigemDTO> vendas =
                            loyaltyVenda144Repository.buscarVendasPorDataELoja(data, numeroLojaOrigem);

                    LOG.info("LOYALTY vendas consultadas | execucaoId={} loteId={} lojaId={} nomeLoja={} codLojaRms={} codLojaEconect={} dataMovimento={} qtd={}",
                            execucao.getExecucaoLoyaltyId(),
                            lote.getLoteId(),
                            loja.getLojaId(),
                            loja.getNome(),
                            loja.getCodLojaRms(),
                            loja.getCodLojaEconect(),
                            data,
                            vendas.size());

                    ProcessamentoLoteResult resultado = modoLote
                            ? processarEmLotes(execucao, lote, loja, data, vendas)
                            : processarCupomACupom(execucao, lote, loja, data, vendas);

                    LocalDateTime fimLote = LocalDateTime.now();
                    long tempoTotalMsLote = Duration.between(lote.getInicioEm(), fimLote).toMillis();

                    String mensagemResumo = "Consultados=" + vendas.size()
                            + " | Enviados=" + resultado.enviados()
                            + " | Falhas=" + resultado.falhas();

                    StatusExecucaoEnum statusLote;
                    if (vendas.isEmpty()) {
                        statusLote = StatusExecucaoEnum.SUCESSO;
                    } else if (resultado.falhas() == 0) {
                        statusLote = StatusExecucaoEnum.SUCESSO;
                    } else if (resultado.enviados() > 0) {
                        statusLote = StatusExecucaoEnum.FALHA_PARCIAL;
                    } else {
                        statusLote = StatusExecucaoEnum.FALHA;
                    }

                    lotePersistenceService.finalizarLote(
                            lote.getLoteId(),
                            vendas.size(),
                            resultado.enviados(),
                            resultado.falhas(),
                            resultado.pendentes(),
                            statusLote,
                            mensagemResumo,
                            fimLote,
                            tempoTotalMsLote
                    );

                    LOG.info("LOYALTY lote finalizado | execucaoId={} loteId={} lojaId={} nomeLoja={} codLojaRms={} codLojaEconect={} dataMovimento={} consultados={} enviados={} falhas={} pendentes={} status={}",
                            execucao.getExecucaoLoyaltyId(),
                            lote.getLoteId(),
                            loja.getLojaId(),
                            loja.getNome(),
                            loja.getCodLojaRms(),
                            loja.getCodLojaEconect(),
                            data,
                            vendas.size(),
                            resultado.enviados(),
                            resultado.falhas(),
                            resultado.pendentes(),
                            statusLote);

                    totalConsultados += vendas.size();
                    totalEnviados += resultado.enviados();
                    totalFalhas += resultado.falhas();
                }
            }

            LocalDateTime fimExecucao = LocalDateTime.now();
            long tempoTotalMsExecucao = Duration.between(inicio, fimExecucao).toMillis();

            String mensagemResumo = "Modo=" + modo
                    + " | Lojas=" + lojas.size()
                    + " | Lotes=" + totalLotes
                    + " | Consultados=" + totalConsultados
                    + " | Enviados=" + totalEnviados
                    + " | Falhas=" + totalFalhas;

            StatusExecucaoEnum statusExecucao;
            if (totalFalhas == 0) {
                statusExecucao = StatusExecucaoEnum.SUCESSO;
            } else if (totalEnviados > 0) {
                statusExecucao = StatusExecucaoEnum.FALHA_PARCIAL;
            } else {
                statusExecucao = StatusExecucaoEnum.FALHA;
            }

            LOG.info("LOYALTY execução finalizada | execucaoId={} modo={} status={} lojas={} lotes={} consultados={} enviados={} falhas={} tempoMs={}",
                    execucao.getExecucaoLoyaltyId(),
                    modo,
                    statusExecucao,
                    lojas.size(),
                    totalLotes,
                    totalConsultados,
                    totalEnviados,
                    totalFalhas,
                    tempoTotalMsExecucao);

            RotinaExecucaoLoyalty saved = execucaoPersistenceService.finalizarExecucao(
                    execucao.getExecucaoLoyaltyId(),
                    lojas.size(),
                    totalLotes,
                    totalConsultados,
                    totalEnviados,
                    totalFalhas,
                    statusExecucao,
                    mensagemResumo,
                    null,
                    fimExecucao,
                    tempoTotalMsExecucao
            );

            notificarSeAplicavel(saved, modoLote, totalConsultados, totalLotes);

            return saved;

        } catch (Exception e) {
            LocalDateTime fimExecucao = LocalDateTime.now();
            long tempoTotalMsExecucao = Duration.between(inicio, fimExecucao).toMillis();

            LOG.error("LOYALTY execução com erro | execucaoId={} modo={} msg={}",
                    execucao.getExecucaoLoyaltyId(), modo, e.getMessage(), e);

            RotinaExecucaoLoyalty saved = execucaoPersistenceService.finalizarExecucao(
                    execucao.getExecucaoLoyaltyId(),
                    null,
                    totalLotes,
                    totalConsultados,
                    totalEnviados,
                    totalFalhas,
                    StatusExecucaoEnum.FALHA,
                    null,
                    e.getMessage(),
                    fimExecucao,
                    tempoTotalMsExecucao
            );

            notificarSeAplicavel(saved, modoLote, totalConsultados, totalLotes);

            return saved;
        }
    }

    private void notificarSeAplicavel(RotinaExecucaoLoyalty saved,
                                      boolean modoLote,
                                      int totalConsultados,
                                      int totalLotes) {
        if (saved == null || saved.getExecucaoLoyaltyId() == null) {
            return;
        }

        boolean execucaoGrande = modoLote
                && (totalConsultados > MAX_CUPONS_NOTIFICACAO_DETALHADA
                || totalLotes > MAX_LOTES_NOTIFICACAO_DETALHADA);

        try {
            if (execucaoGrande) {
                LOG.warn("LOYALTY notificação detalhada substituída por resumida | execucaoId={} consultados={} lotes={} limiteCupons={} limiteLotes={}",
                        saved.getExecucaoLoyaltyId(),
                        totalConsultados,
                        totalLotes,
                        MAX_CUPONS_NOTIFICACAO_DETALHADA,
                        MAX_LOTES_NOTIFICACAO_DETALHADA);

                notificacaoLoyaltyService.notificarFinalizacaoLoyaltyResumida(saved.getExecucaoLoyaltyId());
            } else {
                notificacaoLoyaltyService.notificarFinalizacaoLoyalty(saved.getExecucaoLoyaltyId());
            }
        } catch (Exception e) {
            LOG.error("LOYALTY falha ao enviar e-mail de notificação | execucaoId={} msg={}",
                    saved.getExecucaoLoyaltyId(), e.getMessage(), e);
        }
    }

    private ProcessamentoLoteResult processarCupomACupom(RotinaExecucaoLoyalty execucao,
                                                         RotinaExecucaoLoyaltyLote lote,
                                                         Loja loja,
                                                         LocalDate data,
                                                         List<LoyaltyCupomOrigemDTO> vendas) {

        int enviadosLote = 0;
        int falhasLote = 0;
        int pendentesLote = 0;

        final int totalCuponsLote = vendas.size();
        int processadosLote = 0;
        final long t0Nano = System.nanoTime();
        long lastLogNano = t0Nano;

        for (LoyaltyCupomOrigemDTO venda : vendas) {
            LoyaltyCupomPayloadDTO payload = montarPayload(venda);

            LOG.debug("LOYALTY enviando cupom | execucaoId={} loteId={} lojaId={} nomeLoja={} codLojaRms={} codLojaEconect={} dataMovimento={} pdv={} cupom={} categoria={} idCliente={}",
                    execucao.getExecucaoLoyaltyId(),
                    lote.getLoteId(),
                    loja.getLojaId(),
                    loja.getNome(),
                    loja.getCodLojaRms(),
                    loja.getCodLojaEconect(),
                    venda.dtMovimento(),
                    venda.idPdv(),
                    venda.numCupom(),
                    payload.categoria(),
                    payload.idCliente());

            LoyaltyApiResponseDTO response = loyaltyApiClient.enviarCupom(payload);
            processadosLote++;

            boolean sucessoEnvio = response.sucesso() && response.httpStatus() == 201;

            if (sucessoEnvio) {
                enviadosLote++;
            } else {
                falhasLote++;
                pendentesLote++;

                LOG.warn("LOYALTY falha no envio do cupom | execucaoId={} loteId={} lojaId={} nomeLoja={} codLojaRms={} codLojaEconect={} dataMovimento={} pdv={} cupom={} httpStatus={} erro={}",
                        execucao.getExecucaoLoyaltyId(),
                        lote.getLoteId(),
                        loja.getLojaId(),
                        loja.getNome(),
                        loja.getCodLojaRms(),
                        loja.getCodLojaEconect(),
                        venda.dtMovimento(),
                        venda.idPdv(),
                        venda.numCupom(),
                        response.httpStatus(),
                        response.erro());
            }

            cupomPersistenceService.salvarResultadoCupom(
                    execucao.getExecucaoLoyaltyId(),
                    lote.getLoteId(),
                    loja.getLojaId(),
                    venda,
                    payload,
                    response,
                    sucessoEnvio
            );

            long nowNano = System.nanoTime();
            boolean passoCupom = (processadosLote % PROGRESS_STEP_CUPONS_UNITARIO) == 0;
            boolean passoTempo = (nowNano - lastLogNano) >= PROGRESS_STEP_SECONDS_UNITARIO * 1_000_000_000L;
            boolean terminou = processadosLote == totalCuponsLote;

            if (passoCupom || passoTempo || terminou) {
                int percent = totalCuponsLote > 0 ? (processadosLote * 100 / totalCuponsLote) : 100;

                LOG.info("LOYALTY progresso lote | execucaoId={} loteId={} lojaId={} nomeLoja={} codLojaRms={} codLojaEconect={} dataMovimento={} processados={}/{} ({}%) enviados={} falhas={} pendentes={}",
                        execucao.getExecucaoLoyaltyId(),
                        lote.getLoteId(),
                        loja.getLojaId(),
                        loja.getNome(),
                        loja.getCodLojaRms(),
                        loja.getCodLojaEconect(),
                        data,
                        processadosLote,
                        totalCuponsLote,
                        percent,
                        enviadosLote,
                        falhasLote,
                        pendentesLote);

                lastLogNano = nowNano;
            }
        }

        return new ProcessamentoLoteResult(enviadosLote, falhasLote, pendentesLote);
    }

    private ProcessamentoLoteResult processarEmLotes(RotinaExecucaoLoyalty execucao,
                                                     RotinaExecucaoLoyaltyLote lote,
                                                     Loja loja,
                                                     LocalDate data,
                                                     List<LoyaltyCupomOrigemDTO> vendas) {

        int enviadosLote = 0;
        int falhasLote = 0;
        int pendentesLote = 0;

        int tamanhoLote = loyaltyBatchProperties.getMaxCupons();

        List<LoyaltyCupomEnvioItem> itens = vendas.stream()
                .map(v -> new LoyaltyCupomEnvioItem(v, montarPayload(v)))
                .collect(Collectors.toList());

        List<List<LoyaltyCupomEnvioItem>> batches = particionar(itens, tamanhoLote);

        int totalCuponsLote = itens.size();
        int processadosLote = 0;
        long t0Nano = System.nanoTime();
        long lastLogNano = t0Nano;

        for (int i = 0; i < batches.size(); i++) {
            List<LoyaltyCupomEnvioItem> batch = batches.get(i);

            List<LoyaltyCupomOrigemDTO> vendasBatch = batch.stream()
                    .map(LoyaltyCupomEnvioItem::venda)
                    .collect(Collectors.toList());

            List<LoyaltyCupomPayloadDTO> payloadsBatch = batch.stream()
                    .map(LoyaltyCupomEnvioItem::payload)
                    .collect(Collectors.toList());

            LoyaltyApiResponseDTO response = loyaltyApiClient.enviarCuponsEmLote(payloadsBatch);

            boolean sucessoBatch = response.sucesso() && response.httpStatus() == 201;

            if (sucessoBatch) {
                enviadosLote += payloadsBatch.size();
            } else {
                falhasLote += payloadsBatch.size();
                pendentesLote += payloadsBatch.size();

                LOG.warn("LOYALTY falha no envio do batch | execucaoId={} loteId={} lojaId={} nomeLoja={} codLojaRms={} codLojaEconect={} dataMovimento={} batch={}/{} qtdCupons={} httpStatus={} erro={}",
                        execucao.getExecucaoLoyaltyId(),
                        lote.getLoteId(),
                        loja.getLojaId(),
                        loja.getNome(),
                        loja.getCodLojaRms(),
                        loja.getCodLojaEconect(),
                        data,
                        i + 1,
                        batches.size(),
                        payloadsBatch.size(),
                        response.httpStatus(),
                        response.erro());
            }

            cupomPersistenceService.salvarResultadosCuponsEmLote(
                    execucao.getExecucaoLoyaltyId(),
                    lote.getLoteId(),
                    loja.getLojaId(),
                    vendasBatch,
                    payloadsBatch,
                    response,
                    sucessoBatch
            );

            processadosLote += payloadsBatch.size();

            long nowNano = System.nanoTime();
            boolean passoCupom = (processadosLote % PROGRESS_STEP_CUPONS_LOTE) == 0;
            boolean passoTempo = (nowNano - lastLogNano) >= PROGRESS_STEP_SECONDS_LOTE * 1_000_000_000L;
            boolean terminou = processadosLote == totalCuponsLote;

            if (passoCupom || passoTempo || terminou) {
                int percent = totalCuponsLote > 0 ? (processadosLote * 100 / totalCuponsLote) : 100;

                LOG.info("LOYALTY progresso lote | execucaoId={} loteId={} lojaId={} nomeLoja={} codLojaRms={} codLojaEconect={} dataMovimento={} processados={}/{} ({}%) enviados={} falhas={} pendentes={} modo=LOTE",
                        execucao.getExecucaoLoyaltyId(),
                        lote.getLoteId(),
                        loja.getLojaId(),
                        loja.getNome(),
                        loja.getCodLojaRms(),
                        loja.getCodLojaEconect(),
                        data,
                        processadosLote,
                        totalCuponsLote,
                        percent,
                        enviadosLote,
                        falhasLote,
                        pendentesLote);

                lastLogNano = nowNano;
            }
        }

        return new ProcessamentoLoteResult(enviadosLote, falhasLote, pendentesLote);
    }

    @Override
    public int reprocessarPendencias(List<Long> lojaIds, LocalDate dataInicial, LocalDate dataFinal) {
        List<RotinaExecucaoLoyaltyCupom> pendencias;

        if (lojaIds == null || lojaIds.isEmpty() || dataInicial == null || dataFinal == null) {
            pendencias = cupomRepository.findByReprocessamentoPendenteTrueOrderByDataMovimentoAscExecucaoLoyaltyCupomIdAsc();
        } else {
            pendencias = cupomRepository
                    .findByReprocessamentoPendenteTrueAndLoja_LojaIdInAndDataMovimentoBetweenOrderByDataMovimentoAscExecucaoLoyaltyCupomIdAsc(
                            lojaIds, dataInicial, dataFinal);
        }

        LOG.info("LOYALTY reprocessamento iniciado | lojaIds={} dataInicial={} dataFinal={} qtdPendencias={}",
                lojaIds, dataInicial, dataFinal, pendencias.size());

        int sucesso = 0;

        for (RotinaExecucaoLoyaltyCupom pendencia : pendencias) {
            try {
                LOG.info("LOYALTY reprocessando pendência | pendenciaId={} execucaoId={} lojaId={} dataMovimento={} pdv={} cupom={} tentativaAtual={}",
                        pendencia.getExecucaoLoyaltyCupomId(),
                        pendencia.getExecucao() != null ? pendencia.getExecucao().getExecucaoLoyaltyId() : null,
                        pendencia.getLoja() != null ? pendencia.getLoja().getLojaId() : null,
                        pendencia.getDataMovimento(),
                        pendencia.getIdPdv(),
                        pendencia.getNumCupom(),
                        pendencia.getTentativasEnvio());

                LoyaltyCupomPayloadDTO payload =
                        objectMapper.readValue(pendencia.getPayloadJson(), LoyaltyCupomPayloadDTO.class);

                LoyaltyApiResponseDTO response = loyaltyApiClient.enviarCupom(payload);

                if (response.sucesso() && response.httpStatus() == 201) {
                    cupomPersistenceService.atualizarReprocessamento(
                            pendencia.getExecucaoLoyaltyCupomId(),
                            response,
                            StatusExecucaoEnum.SUCESSO,
                            false,
                            null,
                            toJson(payload)
                    );
                    sucesso++;

                    LOG.info("LOYALTY reprocessamento com sucesso | pendenciaId={} httpStatus={}",
                            pendencia.getExecucaoLoyaltyCupomId(),
                            response.httpStatus());
                } else {
                    cupomPersistenceService.atualizarReprocessamento(
                            pendencia.getExecucaoLoyaltyCupomId(),
                            response,
                            StatusExecucaoEnum.FALHA,
                            true,
                            response.erro(),
                            toJson(payload)
                    );

                    LOG.warn("LOYALTY reprocessamento falhou | pendenciaId={} httpStatus={} erro={}",
                            pendencia.getExecucaoLoyaltyCupomId(),
                            response.httpStatus(),
                            response.erro());
                }

            } catch (Exception e) {
                cupomPersistenceService.atualizarReprocessamentoComErro(
                        pendencia.getExecucaoLoyaltyCupomId(),
                        e.getMessage()
                );

                LOG.error("LOYALTY erro no reprocessamento | pendenciaId={} msg={}",
                        pendencia.getExecucaoLoyaltyCupomId(), e.getMessage(), e);
            }
        }

        LOG.info("LOYALTY reprocessamento finalizado | sucesso={} totalPendencias={}",
                sucesso, pendencias.size());

        return sucesso;
    }

    @Override
    public List<RotinaExecucaoLoyalty> listarHistorico() {
        return execucaoRepository.findTop50ByOrderByExecucaoLoyaltyIdDesc();
    }

    @Override
    public List<RotinaExecucaoLoyaltyCupom> listarPendencias() {
        return cupomRepository.findByReprocessamentoPendenteTrueOrderByDataMovimentoAscExecucaoLoyaltyCupomIdAsc();
    }

    @Override
    public RotinaExecucaoLoyalty executarDiaAnteriorAutomatico() {
        LocalDate data = LocalDate.now().minusDays(1);

        LOG.info("LOYALTY execução automática do dia anterior | data={}", data);

        return executarDataReferencia(data, null, true, OrigemExecucaoEnum.AUTOMATICA);
    }

    @Override
    public RotinaExecucaoLoyalty executarDataReferencia(LocalDate dataReferencia,
                                                        List<Long> lojaIds,
                                                        boolean selecionarTodas,
                                                        OrigemExecucaoEnum origemExecucao) {
        LocalDate data = (dataReferencia != null ? dataReferencia : LocalDate.now().minusDays(1));

        LOG.info("LOYALTY executarDataReferencia | dataReferencia={} lojaIds={} selecionarTodas={} origem={}",
                data, lojaIds, selecionarTodas, origemExecucao);

        return executarCargaNormal(lojaIds, data, data, selecionarTodas, origemExecucao);
    }

    private LoyaltyCupomPayloadDTO montarPayload(LoyaltyCupomOrigemDTO venda) {
        String cpfHash = loyaltyHashService.hashCpf(venda.idCliente());
        String idCliente = (cpfHash != null && !cpfHash.isBlank()) ? cpfHash : "0";
        String categoria = !"0".equals(idCliente) ? "Identificado" : "Nao Identificado";

        return new LoyaltyCupomPayloadDTO(
                venda.dtMovimento() != null ? venda.dtMovimento().format(FMT_DATA) : null,
                venda.idLoja(),
                venda.nomeLoja(),
                venda.idPdv(),
                idCliente,
                categoria,
                venda.numCupom(),
                venda.vlrVenda(),
                venda.qtdProduto(),
                venda.idOperador(),
                venda.canalVenda()
        );
    }

    private List<Loja> carregarLojasElegiveis(List<Long> lojaIds, boolean selecionarTodas) {
        List<Loja> lojas;

        if (selecionarTodas || lojaIds == null || lojaIds.isEmpty()) {
            lojas = lojaRepository.findByLoyaltyAtivoTrueAndCodLojaEconectIsNotNullOrderByNomeAsc();
        } else {
            lojas = lojaRepository.findByLojaIdInAndLoyaltyAtivoTrueAndCodLojaEconectIsNotNullOrderByNomeAsc(lojaIds);
        }

        lojas = lojas.stream()
                .sorted(
                        Comparator
                                .comparing(this::parseCodLojaRmsOrdenacao, Comparator.nullsLast(Integer::compareTo))
                                .thenComparing(l -> safe(l.getNome()))
                )
                .collect(Collectors.toList());

        LOG.debug("LOYALTY carregarLojasElegiveis | selecionarTodas={} lojaIds={} total={} ordemCodLojaRms={}",
                selecionarTodas,
                lojaIds,
                lojas.size(),
                lojas.stream()
                        .map(l -> safe(l.getCodLojaRms()) + "-" + safe(l.getNome()))
                        .collect(Collectors.joining(", ")));

        return lojas;
    }

    private Integer parseCodLojaRmsOrdenacao(Loja loja) {
        try {
            if (loja == null || loja.getCodLojaRms() == null) {
                return null;
            }

            String valor = loja.getCodLojaRms().trim();
            if (valor.isEmpty()) {
                return null;
            }

            return Integer.valueOf(valor);
        } catch (Exception e) {
            return null;
        }
    }

    private String safe(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private Integer parseLojaOrigem(String codLojaEconect) {
        try {
            return Integer.valueOf(codLojaEconect.trim());
        } catch (Exception e) {
            LOG.warn("LOYALTY parseLojaOrigem inválido | codLojaEconect={}", codLojaEconect);
            return null;
        }
    }

    private void validarPeriodo(LocalDate dataInicial, LocalDate dataFinal) {
        if (dataInicial == null || dataFinal == null) {
            throw new IllegalArgumentException("Informe data inicial e data final.");
        }
        if (dataFinal.isBefore(dataInicial)) {
            throw new IllegalArgumentException("Data final não pode ser menor que a data inicial.");
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            LOG.error("LOYALTY erro ao serializar payload para JSON | msg={}", e.getMessage(), e);
            return null;
        }
    }

    private <T> List<List<T>> particionar(List<T> origem, int tamanho) {
        List<List<T>> partes = new ArrayList<>();
        if (origem == null || origem.isEmpty()) {
            return partes;
        }

        int tamanhoReal = Math.max(1, tamanho);

        for (int i = 0; i < origem.size(); i += tamanhoReal) {
            partes.add(origem.subList(i, Math.min(i + tamanhoReal, origem.size())));
        }

        return partes;
    }

    private record LoyaltyCupomEnvioItem(LoyaltyCupomOrigemDTO venda, LoyaltyCupomPayloadDTO payload) {
    }

    private record ProcessamentoLoteResult(int enviados, int falhas, int pendentes) {
    }
}