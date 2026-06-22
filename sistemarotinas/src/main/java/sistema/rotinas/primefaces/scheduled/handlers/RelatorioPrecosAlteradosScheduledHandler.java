package sistema.rotinas.primefaces.scheduled.handlers;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import sistema.rotinas.primefaces.scheduled.runtime.ScheduledTaskHandler;
import sistema.rotinas.primefaces.scheduled.runtime.TaskExecutionContext;
import sistema.rotinas.primefaces.scheduled.runtime.TaskRunResult;
import sistema.rotinas.primefaces.service.NotificacaoService;
import sistema.rotinas.primefaces.service.RelatorioPrecosAlteradosService;

/**
 * Handler DB-Scheduler para:
 * ScheduledRelatorioPrecosAlteradosTask
 *
 * Padrão esperado no banco:
 * - task_key: ROTINA_RELATORIO_PRECOS_ALTERADOS (ou o que você preferir, mas tem que bater com o cadastro)
 *
 * Params JSON opcionais (exemplos):
 * {
 *   "zoneId": "America/Sao_Paulo",
 *   "startHour": 20,
 *   "startMinute": 0,
 *   "endHour": 7,
 *   "endMinute": 0,
 *   "dtIni": "11/01/2026 20:00:00",
 *   "dtFim": "12/01/2026 07:00:00"
 * }
 *
 * Regras:
 * - Se dtIni/dtFim vierem no JSON, usa exatamente eles.
 * - Senão, calcula ONTEM 20:00:00 -> HOJE 07:00:00 no zoneId (default SP).
 */
@Component
public class RelatorioPrecosAlteradosScheduledHandler implements ScheduledTaskHandler {

    public static final String KEY = "ROTINA_RELATORIO_PRECOS_ALTERADOS";

    private static final Logger LOG = LoggerFactory.getLogger("SCHEDULER_DB");

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final String DEFAULT_ZONE = "America/Sao_Paulo";

    private final RelatorioPrecosAlteradosService relatorioService;
    private final NotificacaoService notificacaoService;

    public RelatorioPrecosAlteradosScheduledHandler(RelatorioPrecosAlteradosService relatorioService,
                                                    NotificacaoService notificacaoService) {
        this.relatorioService = relatorioService;
        this.notificacaoService = notificacaoService;
    }

    @Override
    public String taskKey() {
        return KEY;
    }

    @Override
    public TaskRunResult execute(TaskExecutionContext ctx) {
        long t0 = System.currentTimeMillis();

        String zoneIdStr = safeTrim(ctx != null ? ctx.paramString("zoneId", DEFAULT_ZONE) : DEFAULT_ZONE);
        ZoneId zone = resolveZone(zoneIdStr);

        // Se vier dtIni/dtFim no JSON, usa.
        String dtIni = safeTrim(ctx != null ? ctx.paramString("dtIni", null) : null);
        String dtFim = safeTrim(ctx != null ? ctx.paramString("dtFim", null) : null);

        // Senão, calcula janela padrão (ONTEM 20:00:00 -> HOJE 07:00:00) com horas configuráveis
        if (dtIni == null || dtFim == null) {
            int startHour = ctx != null ? ctx.paramInt("startHour", 20) : 20;
            int startMinute = ctx != null ? ctx.paramInt("startMinute", 0) : 0;

            int endHour = ctx != null ? ctx.paramInt("endHour", 7) : 7;
            int endMinute = ctx != null ? ctx.paramInt("endMinute", 0) : 0;

            ZonedDateTime agora = ZonedDateTime.now(zone);
            LocalDate hoje = agora.toLocalDate();
            LocalDate ontem = hoje.minusDays(1);

            dtIni = FMT.format(ontem.atTime(startHour, startMinute, 0).atZone(zone));
            dtFim = FMT.format(hoje.atTime(endHour, endMinute, 0).atZone(zone));
        }

        LOG.info("[HANDLER][REL_PRECOS_ALT] Início. zoneId={} dtIni={} dtFim={}", zone.getId(), dtIni, dtFim);

        try {
            // null => todas as lojas (mesmo comportamento do @Scheduled)
            List<String> paths = relatorioService.gerarPdfParaLojas(null, dtIni, dtFim);

            if (paths == null || paths.isEmpty()) {
                LOG.info("[HANDLER][REL_PRECOS_ALT] SKIPPED. Nenhum PDF gerado. dtIni={} dtFim={}", dtIni, dtFim);
                return TaskRunResult.skipped("Nenhum PDF gerado. dtIni=" + dtIni + " dtFim=" + dtFim);
            }

            notificacaoService.notificarRelatorioPrecosAlteradosComAnexos(paths, dtIni, dtFim);

            long ms = System.currentTimeMillis() - t0;
            LOG.info("[HANDLER][REL_PRECOS_ALT] OK. anexos={} dtIni={} dtFim={} tempoMs={}",
                    paths.size(), dtIni, dtFim, ms);

            return TaskRunResult.ok("Relatório enviado com " + paths.size() + " anexo(s). dtIni=" + dtIni + " dtFim=" + dtFim);

        } catch (Exception e) {
            long ms = System.currentTimeMillis() - t0;
            LOG.error("[HANDLER][REL_PRECOS_ALT] FAIL. dtIni={} dtFim={} tempoMs={} msg={}",
                    dtIni, dtFim, ms, e.getMessage(), e);
            return TaskRunResult.fail("Falha ao gerar/enviar relatório de preços alterados: " + e.getMessage());
        }
    }

    private static ZoneId resolveZone(String zoneIdStr) {
        String z = (zoneIdStr == null || zoneIdStr.isBlank()) ? DEFAULT_ZONE : zoneIdStr.trim();
        try {
            return ZoneId.of(z);
        } catch (Exception ignored) {
            return ZoneId.of(DEFAULT_ZONE);
        }
    }

    private static String safeTrim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}