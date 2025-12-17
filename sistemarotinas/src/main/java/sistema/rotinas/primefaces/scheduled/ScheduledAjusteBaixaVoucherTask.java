// src/main/java/sistema/rotinas/primefaces/scheduled/ScheduledAjusteBaixaVoucherTask.java
package sistema.rotinas.primefaces.scheduled;

import java.time.LocalDate;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import sistema.rotinas.primefaces.dto.ResultadoRotinaVoucherDTO;
import sistema.rotinas.primefaces.service.interfaces.IAjusteBaixaVoucherService;

@Component
public class ScheduledAjusteBaixaVoucherTask {

    private static final Logger log = LoggerFactory.getLogger(ScheduledAjusteBaixaVoucherTask.class);

    private final IAjusteBaixaVoucherService ajusteBaixaVoucherService;

    public ScheduledAjusteBaixaVoucherTask(IAjusteBaixaVoucherService ajusteBaixaVoucherService) {
        this.ajusteBaixaVoucherService = ajusteBaixaVoucherService;
    }

    /**
     * Executa a rotina de baixa de voucher automaticamente a cada 10 minutos,
     * sempre usando a data do dia (America/Sao_Paulo).
     *
     * Cron: segundo, minuto, hora, diaMes, mes, diaSemana
     * 0 0/10 * * * * => no minuto 0, 10, 20, 30, 40 e 50 de toda hora.
     */
    @Scheduled(cron = "0 0/10 * * * *", zone = "America/Sao_Paulo")
    public void executarBaixaVoucherAutomatica() {

        LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

        log.info("[Scheduler][Voucher] Iniciando rotina automática de baixa de voucher para a data {}.", hoje);

        try {
            // usamos hoje como dataInicial e dataFinal
            ResultadoRotinaVoucherDTO resultado = ajusteBaixaVoucherService.executar(hoje, hoje);

            log.info(
                "[Scheduler][Voucher] Rotina automática concluída. Data={} | Lidos={}, Inseridos={}, Atualizados={}, JáExistentes={}, JáExistentesSemAlt={}, CPF não encontrado={}, Clientes marcados 99={}",
                hoje,
                resultado.getTotalLidosEconect(),
                resultado.getTotalMovInseridos(),
                resultado.getTotalMovAtualizados(),
                resultado.getTotalMovJaExistentes(),
                resultado.getTotalMovJaExistentesSemAlteracao(),
                resultado.getTotalClientesNaoEncontrados(),
                resultado.getTotalClientesMarcadosSituacao99()
            );

        } catch (Exception e) {
            log.error("[Scheduler][Voucher] Erro ao executar rotina automática de baixa de voucher para a data {}: {}",
                    hoje, e.getMessage(), e);
        }
    }
}
