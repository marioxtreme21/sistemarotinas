package sistema.rotinas.primefaces.scheduled;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import sistema.rotinas.primefaces.enums.OrigemExecucaoEnum;
import sistema.rotinas.primefaces.service.interfaces.IRotinaMgvRunnerService;

@Component
public class ScheduledRotinaMgv {

    private static final Logger log = LoggerFactory.getLogger(ScheduledRotinaMgv.class);

    @Autowired
    private IRotinaMgvRunnerService runner;

    @Value("${sistemarotinas.rotina.mgv.ativo:false}")
    private boolean ativo;

    //@Scheduled(cron = "${sistemarotinas.rotina.mgv.cron:0 30 7 * * *}")
    public void executar() {
        if (!ativo) {
            log.debug("[SCHED-MGV] Desativado (ativo=false).");
            return;
        }

        try {
            log.info("[SCHED-MGV] Disparando rotina MGV (todas as lojas).");
            Long execucaoId = runner.executar(null, OrigemExecucaoEnum.SCHEDULER, "scheduler");
            log.info("[SCHED-MGV] Execução registrada. execucaoId={}", execucaoId);
        } catch (Exception e) {
            log.error("[SCHED-MGV] Falha ao executar rotina MGV: {}", e.getMessage(), e);
        }
    }
}
