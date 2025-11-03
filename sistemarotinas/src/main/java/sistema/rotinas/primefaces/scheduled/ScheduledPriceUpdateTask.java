package sistema.rotinas.primefaces.scheduled;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import sistema.rotinas.primefaces.dto.PriceUpdateRunResult;
import sistema.rotinas.primefaces.service.NotificacaoService;
import sistema.rotinas.primefaces.service.ParallelPriceUpdateOrchestrator;

@Component
public class ScheduledPriceUpdateTask {

    private static final Logger log = LoggerFactory.getLogger(ScheduledPriceUpdateTask.class);

    private final ParallelPriceUpdateOrchestrator orchestrator;
    private final NotificacaoService notificacaoService;

    public ScheduledPriceUpdateTask(ParallelPriceUpdateOrchestrator orchestrator,
                                    NotificacaoService notificacaoService) {
        this.orchestrator = orchestrator;
        this.notificacaoService = notificacaoService;
    }

    /**
     * Executa todos os envios de preço diariamente às 00:50.
     * Cron: segundo minuto hora dia mes diaDaSemana
     */
  //  @Scheduled(cron = "0 50 0 * * *"/*, zone = "America/Sao_Paulo"*/)
    public void executarMadrugada() {
        final Instant t0 = Instant.now();
        log.info("[Scheduler][PriceUpdate] Início da rotina diária (00:50).");

        // 1) Executa todas as lojas ATIVAS respeitando a prioridade e coleta o relatório
        List<PriceUpdateRunResult> resultados =
                orchestrator.executarTodasAsLojasEmParaleloComRelatorio();

        // 2) Envia e-mail de resumo (apenas aqui, para não duplicar)
        try {
            notificacaoService.notificarResumoPriceUpdate(resultados, true);
            log.info("[Scheduler][PriceUpdate] E-mail de resumo enviado com {} linha(s).", resultados.size());
        } catch (Exception ex) {
            log.error("[Scheduler][PriceUpdate] Falha ao enviar e-mail de resumo: {}", ex.getMessage(), ex);
        }

        log.info("[Scheduler][PriceUpdate] Rotina concluída em {} ms.",
                Duration.between(t0, Instant.now()).toMillis());
    }
}
