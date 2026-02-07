package sistema.rotinas.primefaces.scheduled;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import sistema.rotinas.primefaces.enums.OrigemExecucaoEnum;
import sistema.rotinas.primefaces.service.RotinaPriceAutoSelectorService;
import sistema.rotinas.primefaces.service.interfaces.IRotinaPriceRunnerService;

@Component
public class ScheduledRotinaPrice {

    private static final Logger LOG = LoggerFactory.getLogger("ROTINA_PRICE");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired private RotinaPriceAutoSelectorService autoSelectorService;
    @Autowired private IRotinaPriceRunnerService runner;

    // ===== 1ª tentativa do dia =====
    //@Scheduled(cron = "0 45 16 * * *") // 04:00
    public void executar0400() {
        executarJanela("AUTO-04:00", false);
    }

    // ===== Retries =====
   // @Scheduled(cron = "0 10 11 * * *") // 11:10
    public void executar1110() {
        executarJanela("AUTO-11:10", true);
    }

   // @Scheduled(cron = "0 16 10 * * *") // 12:10
    public void executar1210() {
        executarJanela("AUTO-12:10", true);
    }

   // @Scheduled(cron = "0 10 13 * * *") // 13:10
    public void executar1310() {
        executarJanela("AUTO-13:10", true);
    }

    private void executarJanela(String tag, boolean retry) {
        Long execucaoId = null;
        long t0 = System.currentTimeMillis();

        LOG.info("[SCHED][PRICE][{}] Início do scheduler. retry={} agora={}", tag, retry, LocalDateTime.now().format(FMT));

        try {
            List<Long> lojaIds = autoSelectorService.selecionarLojasElegiveisHoje(retry, tag);

            if (lojaIds == null || lojaIds.isEmpty()) {
                LOG.info("[SCHED][PRICE][{}] Nenhuma loja elegível para executar nesta janela. retry={}", tag, retry);
                return;
            }

            LOG.info("[SCHED][PRICE][{}] Executando runner para lojas={} retry={}", tag, lojaIds.size(), retry);

            execucaoId = runner.executar(lojaIds, OrigemExecucaoEnum.AUTOMATICA, tag);

            LOG.info("[SCHED][PRICE][{}] Runner finalizado. execucaoId={}", tag, execucaoId);

        } catch (Exception e) {
            LOG.error("[SCHED][PRICE][{}] Falha inesperada no scheduler. execucaoId={} msg={}",
                    tag, execucaoId, e.getMessage(), e);
        } finally {
            long ms = System.currentTimeMillis() - t0;
            LOG.info("[SCHED][PRICE][{}] Fim do scheduler. execucaoId={} tempoMs={}", tag, execucaoId, ms);
        }
    }
}
