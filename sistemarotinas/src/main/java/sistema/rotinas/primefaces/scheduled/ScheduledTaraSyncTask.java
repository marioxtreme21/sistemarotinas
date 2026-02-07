package sistema.rotinas.primefaces.scheduled;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import sistema.rotinas.primefaces.service.interfaces.ITaraService;

@Component
public class ScheduledTaraSyncTask {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTaraSyncTask.class);

    private final ITaraService taraService;

    public ScheduledTaraSyncTask(ITaraService taraService) {
        this.taraService = taraService;
    }

    /**
     * Executa a cada 30 minutos, horário de São Paulo.
     * Cron: segundo, minuto, hora, diaMes, mes, diaSemana
     * 0 0/30 * * * * => no minuto 0 e 30 de toda hora.
     */
    // @Scheduled(cron = "0 0/30 * * * *", zone = "America/Sao_Paulo")
    public void sincronizarCadPsoEmb() {
        log.info("[Scheduler][Tara] Início da sincronização automática cad_pso_emb -> servidor 144.");
        try {
            taraService.sincronizarComServidor144();
            log.info("[Scheduler][Tara] Sincronização automática concluída com sucesso.");
        } catch (Exception e) {
            log.error("[Scheduler][Tara] Erro ao sincronizar cad_pso_emb com servidor 144: {}", e.getMessage(), e);
        }
    }
}

