package sistema.rotinas.primefaces.scheduled;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import sistema.rotinas.primefaces.service.NotificacaoService;
import sistema.rotinas.primefaces.service.RelatorioPrecosAlteradosService;

@Component
public class ScheduledRelatorioPrecosAlteradosTask {

    private static final Logger log = LoggerFactory.getLogger(ScheduledRelatorioPrecosAlteradosTask.class);

    private final RelatorioPrecosAlteradosService relatorioService;
    private final NotificacaoService notificacaoService;

    // formato igual ao usado na tela/bean
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final ZoneId ZONE_SP = ZoneId.of("America/Sao_Paulo");

    public ScheduledRelatorioPrecosAlteradosTask(RelatorioPrecosAlteradosService relatorioService,
                                                 NotificacaoService notificacaoService) {
        this.relatorioService = relatorioService;
        this.notificacaoService = notificacaoService;
    }

    /**
     * Executa diariamente às 01:00 (America/Sao_Paulo).
     * Período considerado: HOJE 00:00:00 → HOJE 07:00:00.
     *
     * cron = "segundo minuto hora diaDoMês mês diaDaSemana"
     */
    @Scheduled(cron = "0 00 01 * * *", zone = "America/Sao_Paulo")
    public void gerarEEnviarRelatorioDiario() {
        ZonedDateTime agora = ZonedDateTime.now(ZONE_SP);
        LocalDate hoje = agora.toLocalDate();

        String dtIni = FMT.format(hoje.atTime(0, 0, 0).atZone(ZONE_SP));
        String dtFim = FMT.format(hoje.atTime(7, 0, 0).atZone(ZONE_SP));

        log.info("[Scheduler][RelatorioPrecosAlterados] Início: período {} -> {}", dtIni, dtFim);

        try {
            // null = todas as lojas (o service já trata isso internamente)
            List<String> paths = relatorioService.gerarPdfParaLojas(null, dtIni, dtFim);

            if (paths == null || paths.isEmpty()) {
                log.warn("[Scheduler][RelatorioPrecosAlterados] Nenhum PDF gerado para o período {} -> {}.", dtIni, dtFim);
                return;
            }

            // Envia 1 único e-mail com todos os anexos
            notificacaoService.notificarRelatorioPrecosAlteradosComAnexos(paths, dtIni, dtFim);
            log.info("[Scheduler][RelatorioPrecosAlterados] E-mail enviado com {} anexo(s).", paths.size());

        } catch (Exception e) {
            log.error("[Scheduler][RelatorioPrecosAlterados] Falha na rotina: {}", e.getMessage(), e);
        } finally {
            log.info("[Scheduler][RelatorioPrecosAlterados] Fim da execução diária.");
        }
    }
}
