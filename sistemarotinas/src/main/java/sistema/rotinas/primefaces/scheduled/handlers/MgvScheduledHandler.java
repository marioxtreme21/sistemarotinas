package sistema.rotinas.primefaces.scheduled.handlers;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import sistema.rotinas.primefaces.enums.OrigemExecucaoEnum;
import sistema.rotinas.primefaces.scheduled.runtime.ScheduledTaskHandler;
import sistema.rotinas.primefaces.scheduled.runtime.TaskExecutionContext;
import sistema.rotinas.primefaces.scheduled.runtime.TaskRunResult;
import sistema.rotinas.primefaces.service.RotinaMgvAutoSelectorService;
import sistema.rotinas.primefaces.service.interfaces.IRotinaMgvRunnerService;

@Component
public class MgvScheduledHandler implements ScheduledTaskHandler {

    // ✅ defina uma key clara e estável (padrão igual do PRICE)
    public static final String KEY = "ROTINA_MGV_AUTOMATICA";

    private static final Logger LOG = LoggerFactory.getLogger("SCHEDULER_DB");

    private final RotinaMgvAutoSelectorService autoSelectorService;
    private final IRotinaMgvRunnerService runner;

    public MgvScheduledHandler(RotinaMgvAutoSelectorService autoSelectorService,
                               IRotinaMgvRunnerService runner) {
        this.autoSelectorService = autoSelectorService;
        this.runner = runner;
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

        boolean retry = (ctx != null && ctx.paramBool("retry", false));

        // default AUTOMATICA (mantém compatibilidade com seus fluxos)
        OrigemExecucaoEnum origem = OrigemExecucaoEnum.AUTOMATICA;
        try {
            String origemStr = safeTrim(ctx != null ? ctx.paramString("origem", null) : null);
            if (origemStr != null) {
                origem = OrigemExecucaoEnum.valueOf(origemStr.toUpperCase());
            }
        } catch (Exception ignored) {
            // se vier lixo no JSON, ignora e segue AUTOMATICA
        }

        try {
            LOG.info("[HANDLER][MGV] Início. tag={} retry={} origem={}", tag, retry, origem);

            List<Long> lojaIds = autoSelectorService.selecionarLojasElegiveisHoje(retry, tag);

            if (lojaIds == null || lojaIds.isEmpty()) {
                LOG.info("[HANDLER][MGV] SKIPPED. Nenhuma loja elegível. tag={} retry={}", tag, retry);
                return TaskRunResult.skipped("Nenhuma loja elegível. retry=" + retry + " tag=" + tag);
            }

            Long execucaoId = runner.executar(lojaIds, origem, tag);

            LOG.info("[HANDLER][MGV] OK. execucaoId={} lojas={} tag={} retry={}",
                    execucaoId, lojaIds.size(), tag, retry);

            return TaskRunResult.ok(
                "Runner MGV executado. execucaoId=" + execucaoId +
                " lojas=" + lojaIds.size() +
                " retry=" + retry +
                " tag=" + tag
            );

        } catch (Exception e) {
            LOG.error("[HANDLER][MGV] FAIL. tag={} retry={} msg={}", tag, retry, e.getMessage(), e);
            return TaskRunResult.fail("Falha ao executar MGV_AUTO: " + e.getMessage());
        }
    }

    private static String safeTrim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}