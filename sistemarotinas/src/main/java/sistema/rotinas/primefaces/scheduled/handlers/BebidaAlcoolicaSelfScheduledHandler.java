package sistema.rotinas.primefaces.scheduled.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import sistema.rotinas.primefaces.scheduled.runtime.ScheduledTaskHandler;
import sistema.rotinas.primefaces.scheduled.runtime.TaskExecutionContext;
import sistema.rotinas.primefaces.scheduled.runtime.TaskRunResult;
import sistema.rotinas.primefaces.service.interfaces.IRotinaBebidaAlcoolicaSelfService;

@Component
public class BebidaAlcoolicaSelfScheduledHandler implements ScheduledTaskHandler {

    public static final String KEY = "ROTINA_BEBIDA_ALCOOLICA_SELF";

    private static final Logger LOG = LoggerFactory.getLogger("SCHEDULER_DB");

    private final IRotinaBebidaAlcoolicaSelfService rotinaService;

    public BebidaAlcoolicaSelfScheduledHandler(IRotinaBebidaAlcoolicaSelfService rotinaService) {
        this.rotinaService = rotinaService;
    }

    @Override
    public String taskKey() {
        return KEY;
    }

    @Override
    public TaskRunResult execute(TaskExecutionContext ctx) {
        String tag = safeTrim(ctx != null ? ctx.paramString("tag", "AUTO") : "AUTO");
        if (tag == null) tag = "AUTO";

        boolean retry = (ctx != null && ctx.paramBool("retry", false));

        try {
            LOG.info("[HANDLER][BEBIDA_SELF] Início. tag={} retry={}", tag, retry);

            // rotina original do @Scheduled
            rotinaService.executarRotinaSemSelect();

            LOG.info("[HANDLER][BEBIDA_SELF] OK. tag={} retry={}", tag, retry);

            return TaskRunResult.ok("Rotina Bebida Alcoólica (Self) executada com sucesso. tag=" + tag + " retry=" + retry);

        } catch (Exception e) {
            LOG.error("[HANDLER][BEBIDA_SELF] FAIL. tag={} retry={} msg={}", tag, retry, e.getMessage(), e);
            return TaskRunResult.fail("Falha ao executar ROTINA_BEBIDA_ALCOOLICA_SELF: " + e.getMessage());
        }
    }

    private static String safeTrim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}