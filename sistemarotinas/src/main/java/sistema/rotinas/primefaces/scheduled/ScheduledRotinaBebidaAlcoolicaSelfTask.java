package sistema.rotinas.primefaces.scheduled;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import sistema.rotinas.primefaces.service.interfaces.IRotinaBebidaAlcoolicaSelfService;

@Component
public class ScheduledRotinaBebidaAlcoolicaSelfTask {

    private static final Logger log = LoggerFactory.getLogger(ScheduledRotinaBebidaAlcoolicaSelfTask.class);

    private final IRotinaBebidaAlcoolicaSelfService rotinaBebidaAlcoolicaSelfService;

    public ScheduledRotinaBebidaAlcoolicaSelfTask(IRotinaBebidaAlcoolicaSelfService rotinaBebidaAlcoolicaSelfService) {
        this.rotinaBebidaAlcoolicaSelfService = rotinaBebidaAlcoolicaSelfService;
    }

    /**
     * Executa a cada 30 minutos, horário de São Paulo.
     * Cron: segundo, minuto, hora, diaMes, mes, diaSemana
     * 0 0/30 * * * *  => no minuto 0 e 30 de toda hora.
     */
    @Scheduled(cron = "0 0/15 * * * *", zone = "America/Sao_Paulo")
    public void executarRotinaAutomaticamente() {
        log.info("[Scheduler][BebidasAlcoolicasSelf] Início da execução automática (a cada 30 min).");
        try {
            rotinaBebidaAlcoolicaSelfService.executarRotinaSemSelect();
            log.info("[Scheduler][BebidasAlcoolicasSelf] Execução automática concluída com sucesso.");
        } catch (Exception e) {
            log.error("[Scheduler][BebidasAlcoolicasSelf] Erro ao executar rotina automática: {}", e.getMessage(), e);
        }
    }
}
