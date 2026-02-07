package sistema.rotinas.primefaces.scheduled.handlers;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import sistema.rotinas.primefaces.scheduled.runtime.ScheduledTaskHandler;
import sistema.rotinas.primefaces.scheduled.runtime.TaskExecutionContext;
import sistema.rotinas.primefaces.scheduled.runtime.TaskRunResult;
import sistema.rotinas.primefaces.service.RelatorioVendasLojasService;

@Component
public class RelatorioVendasLojasScheduledHandler implements ScheduledTaskHandler {

    public static final String KEY = "ROTINA_RELATORIO_VENDAS_LOJAS";

    private static final Logger LOG = LoggerFactory.getLogger("SCHEDULER_DB");
    private static final ZoneId ZONE_SP = ZoneId.of("America/Sao_Paulo");

    private final RelatorioVendasLojasService relatorioVendasLojasService;

    public RelatorioVendasLojasScheduledHandler(RelatorioVendasLojasService relatorioVendasLojasService) {
        this.relatorioVendasLojasService = relatorioVendasLojasService;
    }

    @Override
    public String taskKey() {
        return KEY;
    }

    @Override
    public TaskRunResult execute(TaskExecutionContext ctx) {
        // defaults seguros
        String tag = safeTrim(ctx != null ? ctx.paramString("tag", "AUTO") : "AUTO");
        if (tag == null) tag = "AUTO";

        // ✅ por padrão mantém igual ao seu @Scheduled atual (D0).
        // Se quiser "ontem", configure no JSON: {"daysOffset":1}
        int daysOffset = 0;
        try {
            if (ctx != null) {
                Integer v = ctx.paramInt("daysOffset", 0);
                if (v != null) daysOffset = v;
            }
        } catch (Exception ignored) { }

        // null => todas as lojas (igual tela quando não filtra)
        String codLojaEconect = null;
        try {
            codLojaEconect = safeTrim(ctx != null ? ctx.paramString("codLojaEconect", null) : null);
        } catch (Exception ignored) { }

        ZonedDateTime agora = ZonedDateTime.now(ZONE_SP);
        LocalDate dataReferencia = agora.toLocalDate().minusDays(daysOffset);

        try {
            LOG.info("[HANDLER][REL_VENDAS_LOJAS] Início. tag={} dataReferencia={} daysOffset={} codLojaEconect={}",
                    tag, dataReferencia, daysOffset, codLojaEconect);

            relatorioVendasLojasService.enviarRelatorioPorEmail(
                    dataReferencia,
                    dataReferencia,
                    codLojaEconect
            );

            LOG.info("[HANDLER][REL_VENDAS_LOJAS] OK. tag={} dataReferencia={}", tag, dataReferencia);

            return TaskRunResult.ok(
                    "Relatório vendas lojas enviado. dataReferencia=" + dataReferencia +
                    " codLojaEconect=" + (codLojaEconect == null ? "ALL" : codLojaEconect) +
                    " tag=" + tag
            );

        } catch (Exception e) {
            LOG.error("[HANDLER][REL_VENDAS_LOJAS] FAIL. tag={} dataReferencia={} msg={}",
                    tag, dataReferencia, e.getMessage(), e);

            return TaskRunResult.fail("Falha ao enviar relatório vendas lojas (" + dataReferencia + "): " + e.getMessage());
        }
    }

    private static String safeTrim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}