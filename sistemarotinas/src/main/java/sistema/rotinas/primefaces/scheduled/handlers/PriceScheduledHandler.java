package sistema.rotinas.primefaces.scheduled.handlers;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import sistema.rotinas.primefaces.enums.OrigemExecucaoEnum;
import sistema.rotinas.primefaces.scheduled.runtime.ScheduledTaskHandler;
import sistema.rotinas.primefaces.scheduled.runtime.TaskExecutionContext;
import sistema.rotinas.primefaces.scheduled.runtime.TaskRunResult;
import sistema.rotinas.primefaces.service.RotinaPriceAutoSelectorService;
import sistema.rotinas.primefaces.service.interfaces.IRotinaPriceRunnerService;

@Component
public class PriceScheduledHandler implements ScheduledTaskHandler {

    public static final String KEY = "ROTINA_PRICE_AUTOMATICA";

    private static final Logger LOG = LoggerFactory.getLogger("SCHEDULER_DB");

    private final RotinaPriceAutoSelectorService autoSelectorService;
    private final IRotinaPriceRunnerService runner;

    public PriceScheduledHandler(RotinaPriceAutoSelectorService autoSelectorService,
                                 IRotinaPriceRunnerService runner) {
        this.autoSelectorService = autoSelectorService;
        this.runner = runner;
    }

    @Override
    public String taskKey() {
        return KEY;
    }

    @Override
    public TaskRunResult execute(TaskExecutionContext ctx) {
        // defaults seguros (ctx nunca deveria ser null, mas deixa robusto)
        String tag = safeTrim(ctx != null ? ctx.paramString("tag", "AUTO") : "AUTO");
        if (tag == null) tag = "AUTO";

        boolean retry = (ctx != null && ctx.paramBool("retry", false));

        // (Opcional) caso você queira permitir no JSON: {"origem":"AUTOMATICA"} / {"origem":"MANUAL"}
        // Mantém default AUTOMATICA para não quebrar o fluxo atual.
        OrigemExecucaoEnum origem = OrigemExecucaoEnum.AUTOMATICA;
        try {
            String origemStr = safeTrim(ctx != null ? ctx.paramString("origem", null) : null);
            if (origemStr != null) {
                origem = OrigemExecucaoEnum.valueOf(origemStr.toUpperCase());
            }
        } catch (Exception ignored) {
            // se vier lixo, ignora e segue AUTOMATICA
        }

        try {
            LOG.info("[HANDLER][PRICE] Início. tag={} retry={} origem={}", tag, retry, origem);

            List<Long> lojaIds = autoSelectorService.selecionarLojasElegiveisHoje(retry, tag);

            if (lojaIds == null || lojaIds.isEmpty()) {
                LOG.info("[HANDLER][PRICE] SKIPPED. Nenhuma loja elegível. tag={} retry={}", tag, retry);
                return TaskRunResult.skipped("Nenhuma loja elegível. retry=" + retry + " tag=" + tag);
            }

            Long execucaoId = runner.executar(lojaIds, origem, tag);

            LOG.info("[HANDLER][PRICE] OK. execucaoId={} lojas={} tag={} retry={}", execucaoId, lojaIds.size(), tag, retry);

            return TaskRunResult.ok(
                "Runner PRICE executado. execucaoId=" + execucaoId +
                " lojas=" + lojaIds.size() +
                " retry=" + retry +
                " tag=" + tag
            );

        } catch (Exception e) {
            LOG.error("[HANDLER][PRICE] FAIL. tag={} retry={} msg={}", tag, retry, e.getMessage(), e);

            // Se TaskRunResult tiver fail(String, Throwable) use ele.
            // Como não vi sua assinatura, vou assumir que existe fail(String).
            return TaskRunResult.fail("Falha ao executar PRICE_AUTO: " + e.getMessage());
        }
    }

    private static String safeTrim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}