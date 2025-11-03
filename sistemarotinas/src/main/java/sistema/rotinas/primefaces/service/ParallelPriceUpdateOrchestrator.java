package sistema.rotinas.primefaces.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import sistema.rotinas.primefaces.dto.PriceUpdateRunResult;
import sistema.rotinas.primefaces.model.Loja;
import sistema.rotinas.primefaces.repository.LojaRepository;
import sistema.rotinas.primefaces.service.interfaces.IPriceUpdateService;

@Service
public class ParallelPriceUpdateOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(ParallelPriceUpdateOrchestrator.class);

    private final LojaRepository lojaRepository;
    private final IPriceUpdateService priceUpdateService;
    private final ThreadPoolTaskExecutor lojasExecutor;
    private final NotificacaoService notificacaoService; // mantido p/ métodos que já enviam e-mail

    public ParallelPriceUpdateOrchestrator(LojaRepository lojaRepository,
                                           IPriceUpdateService priceUpdateService,
                                           ThreadPoolTaskExecutor lojasExecutor,
                                           NotificacaoService notificacaoService) {
        this.lojaRepository = lojaRepository;
        this.priceUpdateService = priceUpdateService;
        this.lojasExecutor = lojasExecutor;
        this.notificacaoService = notificacaoService;
    }

    /* =================================================================================
       1) NOVOS MÉTODOS: retornam o RELATÓRIO (NÃO enviam e-mail)
       ================================================================================= */

    /** Executa TODAS as lojas ativas, respeitando prioridade, e retorna o relatório. */
    public List<PriceUpdateRunResult> executarTodasAsLojasEmParaleloComRelatorio() {
        Instant t0 = Instant.now();

        List<Loja> lojasAtivas = lojaRepository.findAll().stream()
                .filter(l -> Boolean.TRUE.equals(l.getEcommerceAtivo()))
                .sorted(lojaPriorityComparator())
                .toList();

        long qtdPrioritarias = lojasAtivas.stream().filter(this::isPrioritaria).count();
        log.info("[Orchestrator] (RELATÓRIO) Lojas ativas: {} | Prioritárias: {} | Pool: core={}, max={}, queueCapacity={}",
                lojasAtivas.size(), qtdPrioritarias,
                lojasExecutor.getCorePoolSize(),
                lojasExecutor.getMaxPoolSize(),
                lojasExecutor.getThreadPoolExecutor() != null
                        ? lojasExecutor.getThreadPoolExecutor().getQueue().remainingCapacity()
                        : -1);

        if (lojasAtivas.isEmpty()) {
            log.warn("[Orchestrator] Nenhuma loja ativa.");
            return List.of();
        }

        List<CompletableFuture<Void>> futures = new ArrayList<>(lojasAtivas.size());
        List<PriceUpdateRunResult> resultados = java.util.Collections.synchronizedList(new ArrayList<>());

        for (Loja loja : lojasAtivas) {
            String label = fmtLoja(loja);
            futures.add(submitWithRetry(() -> executarUmaLojaColetando(loja, resultados), label));
        }

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

        log.info("[Orchestrator] (RELATÓRIO) Finalizado para {} loja(s) em {} ms.",
                lojasAtivas.size(), Duration.between(t0, Instant.now()).toMillis());

        return resultados;
    }

    /** Executa apenas as lojas informadas (ativas), com prioridade entre elas, e retorna o relatório. */
    public List<PriceUpdateRunResult> executarLojasEmParaleloComRelatorio(List<Long> lojaIds) {
        if (lojaIds == null || lojaIds.isEmpty()) {
            log.warn("[Orchestrator] Lista de lojas vazia.");
            return List.of();
        }

        Instant t0 = Instant.now();

        List<Loja> selecionadas = lojaIds.stream()
                .map(id -> lojaRepository.findById(id).orElse(null))
                .filter(l -> l != null && Boolean.TRUE.equals(l.getEcommerceAtivo()))
                .sorted(lojaPriorityComparator())
                .toList();

        long qtdPrioritarias = selecionadas.stream().filter(this::isPrioritaria).count();
        log.info("[Orchestrator] (RELATÓRIO) Lojas selecionadas ativas: {} | Prioritárias: {}", selecionadas.size(), qtdPrioritarias);

        if (selecionadas.isEmpty()) {
            log.warn("[Orchestrator] Nenhuma loja ativa entre as selecionadas.");
            return List.of();
        }

        List<CompletableFuture<Void>> futures = new ArrayList<>(selecionadas.size());
        List<PriceUpdateRunResult> resultados = java.util.Collections.synchronizedList(new ArrayList<>());

        for (Loja loja : selecionadas) {
            String label = fmtLoja(loja);
            futures.add(submitWithRetry(() -> executarUmaLojaColetando(loja, resultados), label));
        }

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

        log.info("[Orchestrator] (RELATÓRIO) Finalizado (lista específica) para {} loja(s) em {} ms.",
                selecionadas.size(), Duration.between(t0, Instant.now()).toMillis());

        return resultados;
    }

    /* =================================================================================
       2) MÉTODOS EXISTENTES (mantidos): já disparam e-mail ao final
          -> continuam disponíveis para uso manual pela UI, se você quiser.
       ================================================================================= */

    /** Dispara TODAS as lojas ativas (com prioridade) e envia e-mail ao final. */
    public void executarTodasAsLojasEmParalelo() {
        List<PriceUpdateRunResult> resultados = executarTodasAsLojasEmParaleloComRelatorio();
        if (!resultados.isEmpty()) {
            notificacaoService.notificarResumoPriceUpdate(resultados, true);
        }
    }

    /** Dispara uma lista específica (com prioridade) e envia e-mail ao final. */
    public void executarLojasEmParalelo(List<Long> lojaIds) {
        List<PriceUpdateRunResult> resultados = executarLojasEmParaleloComRelatorio(lojaIds);
        if (!resultados.isEmpty()) {
            notificacaoService.notificarResumoPriceUpdate(resultados, false);
        }
    }

    /* =================================================================================
       Núcleo de execução por loja (compartilhado pelos métodos acima)
       ================================================================================= */

    /** Executa uma loja, coleta os contadores da execução e registra logs. */
    private void executarUmaLojaColetando(Loja loja, List<PriceUpdateRunResult> sink) {
        Instant t0 = Instant.now();
        String label = fmtLoja(loja);

        MDC.put("lojaId", String.valueOf(loja.getLojaId()));
        MDC.put("lojaNome", safeStr(loja.getNome()));
        MDC.put("policy", safeStr(loja.getPoliticaComercial()));
        MDC.put("wh", safeStr(loja.getWarehouse()));
        MDC.put("prioAtivo", String.valueOf(Boolean.TRUE.equals(loja.getPrioridadeEnvioAtivo())));
        MDC.put("prioRank", loja.getPrioridadeEnvioRanking() != null ? String.valueOf(loja.getPrioridadeEnvioRanking()) : "-");

        log.info("[Orchestrator] Iniciando {}.", label);
        try {
            PriceUpdateRunResult res =
                    ((PriceUpdateService) priceUpdateService).atualizarPrecosPorLojaComRelatorio(loja.getLojaId());
            sink.add(res);

            long ms = Duration.between(t0, Instant.now()).toMillis();
            log.info("[Orchestrator] Concluída {} em {} ms.", label, ms);
        } catch (Exception e) {
            long ms = Duration.between(t0, Instant.now()).toMillis();
            log.error("[Orchestrator] Falha em {} após {} ms: {}", label, ms, e.getMessage(), e);

            PriceUpdateRunResult erro = new PriceUpdateRunResult();
            erro.setLojaId(loja.getLojaId());
            erro.setLojaNome(loja.getNome());
            erro.setPoliticaComercial(loja.getPoliticaComercial());
            erro.setWarehouse(loja.getWarehouse());
            erro.setInicio(java.time.LocalDateTime.now());
            erro.setFim(java.time.LocalDateTime.now());
            erro.setObservacoes("Falha geral: " + e.getMessage());
            sink.add(erro);
        } finally {
            MDC.clear();
        }
    }

    private CompletableFuture<Void> submitWithRetry(Runnable task, String label) {
        int tentativas = 0;
        long backoffMs = 50L;

        while (true) {
            try {
                return CompletableFuture.runAsync(task, lojasExecutor);
            } catch (org.springframework.core.task.TaskRejectedException tre) {
                tentativas++;
                if (tentativas > 10) {
                    log.error("[Orchestrator] Rejeitado após {} tentativas ({}) — erro: {}",
                            tentativas, label, tre.getMessage());
                    throw tre;
                }
                log.warn("[Orchestrator] Fila/Executor cheio para {} — retry #{}, aguardando {} ms...",
                        label, tentativas, backoffMs);
                try { Thread.sleep(backoffMs); } catch (InterruptedException ignored) {}
                backoffMs = Math.min(backoffMs * 2, 1_000L);
            }
        }
    }

    private Comparator<Loja> lojaPriorityComparator() {
        return Comparator
                .comparing((Loja l) -> !Boolean.TRUE.equals(l.getPrioridadeEnvioAtivo()))
                .thenComparing(l -> l.getPrioridadeEnvioRanking(), Comparator.nullsLast(Integer::compareTo))
                .thenComparing(l -> safeStr(l.getNome()))
                .thenComparing(Loja::getLojaId);
    }

    private boolean isPrioritaria(Loja l) {
        return Boolean.TRUE.equals(l.getPrioridadeEnvioAtivo());
    }

    private String fmtLoja(Loja l) {
        String nome = safeStr(l.getNome());
        String policy = safeStr(l.getPoliticaComercial());
        String wh = safeStr(l.getWarehouse());
        String prioAtivo = String.valueOf(Boolean.TRUE.equals(l.getPrioridadeEnvioAtivo()));
        String prioRank = l.getPrioridadeEnvioRanking() != null ? String.valueOf(l.getPrioridadeEnvioRanking()) : "-";
        return String.format(
                "loja id=%s, nome='%s', policy=%s, warehouse='%s', prioridadeAtiva=%s, prioridadeRanking=%s",
                l.getLojaId(), nome, policy, wh, prioAtivo, prioRank);
    }

    private String safeStr(String v) {
        return v == null ? "-" : v;
    }
}
