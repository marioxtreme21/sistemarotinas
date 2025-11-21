package sistema.rotinas.primefaces.scheduled;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import sistema.rotinas.primefaces.service.RelatorioVendasLojasService;

@Component
public class ScheduledRelatorioVendasLojasTask {

    private static final Logger log = LoggerFactory.getLogger(ScheduledRelatorioVendasLojasTask.class);

    private static final ZoneId ZONE_SP = ZoneId.of("America/Sao_Paulo");

    private final RelatorioVendasLojasService relatorioVendasLojasService;

    public ScheduledRelatorioVendasLojasTask(RelatorioVendasLojasService relatorioVendasLojasService) {
        this.relatorioVendasLojasService = relatorioVendasLojasService;
    }

    /**
     * Executa diariamente às 23:30 (America/Sao_Paulo).
     *
     * - Data de referência: dia ANTERIOR (ontem).
     * - Usa o MESMO método da tela: enviarRelatorioPorEmail(...)
     *
     * cron = "segundo minuto hora diaDoMês mês diaDaSemana"
     */
    @Scheduled(cron = "0 30 23 * * *", zone = "America/Sao_Paulo")
    public void gerarEEnviarRelatorioDiarioVendasLojas() {
        ZonedDateTime agora = ZonedDateTime.now(ZONE_SP);
        LocalDate dataReferencia = agora.toLocalDate().minusDays(0); // D-1

        log.info("[Scheduler][RelatorioVendasLojas] Início: gerando/enviando relatório de vendas para a data {}.", dataReferencia);

        try {
            // null => todas as lojas (mesmo comportamento da tela quando não filtra)
            String codLojaEconect = null;

            // ✅ Reuso TOTAL da lógica da tela
            relatorioVendasLojasService.enviarRelatorioPorEmail(
                    dataReferencia,
                    dataReferencia,
                    codLojaEconect
            );

            log.info("[Scheduler][RelatorioVendasLojas] Concluído: relatório enviado para a data {}.", dataReferencia);

        } catch (Exception e) {
            log.error("[Scheduler][RelatorioVendasLojas] Erro ao gerar/enviar relatório para a data {}: {}",
                    dataReferencia, e.getMessage(), e);
        }
    }
}
