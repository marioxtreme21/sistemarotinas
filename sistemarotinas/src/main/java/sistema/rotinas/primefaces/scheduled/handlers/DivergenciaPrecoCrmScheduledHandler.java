package sistema.rotinas.primefaces.scheduled.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import sistema.rotinas.primefaces.scheduled.runtime.ScheduledTaskHandler;
import sistema.rotinas.primefaces.scheduled.runtime.TaskExecutionContext;
import sistema.rotinas.primefaces.scheduled.runtime.TaskRunResult;
import sistema.rotinas.primefaces.service.DivergenciaPrecoCrmService;

/**
 * Handler DB-Scheduler equivalente ao antigo @ScheduledDivergenciaPrecoCrm.
 *
 * JSON opcional (paramsJson):
 * {
 *   "tag": "AUTO-06:00",
 *   "todasLojas": true
 * }
 *
 * Obs: o service já contém a regra de "se não houver divergências, não envia e-mail".
 */
@Component
public class DivergenciaPrecoCrmScheduledHandler implements ScheduledTaskHandler {

    public static final String KEY = "ROTINA_CRM_DIVERGENCIA_PRECO";

    private static final Logger LOG = LoggerFactory.getLogger("SCHEDULER_DB");

    private final DivergenciaPrecoCrmService service;

    public DivergenciaPrecoCrmScheduledHandler(DivergenciaPrecoCrmService service) {
        this.service = service;
    }

    @Override
    public String taskKey() {
        return KEY;
    }

    @Override
    public TaskRunResult execute(TaskExecutionContext ctx) {

        String tag = safeTrim(ctx != null ? ctx.paramString("tag", "AUTO") : "AUTO");
        if (tag == null) tag = "AUTO";

        // mantém compatível com o padrão atual: sempre todas as lojas
        boolean todasLojas = (ctx == null) ? true : ctx.paramBool("todasLojas", true);

        try {
            LOG.info("[HANDLER][DIVERGENCIA_CRM] Início. tag={} todasLojas={}", tag, todasLojas);

            // null = todas as lojas (mesmo comportamento do scheduled antigo)
            service.executarManual(todasLojas ? null : null);

            LOG.info("[HANDLER][DIVERGENCIA_CRM] OK. tag={}", tag);
            return TaskRunResult.ok("Divergência Preço CRM executada. tag=" + tag + " todasLojas=" + todasLojas);

        } catch (Exception e) {
            LOG.error("[HANDLER][DIVERGENCIA_CRM] FAIL. tag={} msg={}", tag, e.getMessage(), e);
            return TaskRunResult.fail("Falha Divergência Preço CRM: " + e.getMessage());
        }
    }

    private static String safeTrim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}