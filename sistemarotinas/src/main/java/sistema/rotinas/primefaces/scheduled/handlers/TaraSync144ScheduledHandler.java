package sistema.rotinas.primefaces.scheduled.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import sistema.rotinas.primefaces.scheduled.runtime.ScheduledTaskHandler;
import sistema.rotinas.primefaces.scheduled.runtime.TaskExecutionContext;
import sistema.rotinas.primefaces.scheduled.runtime.TaskRunResult;
import sistema.rotinas.primefaces.service.interfaces.ITaraService;

@Component
public class TaraSync144ScheduledHandler implements ScheduledTaskHandler {

    public static final String KEY = "ROTINA_TARA_SYNC_144"; // ou "ROTINA_TARA_SYNC_144"

    private static final Logger LOG = LoggerFactory.getLogger("SCHEDULER_DB");

    private final ITaraService taraService;

    public TaraSync144ScheduledHandler(ITaraService taraService) {
        this.taraService = taraService;
    }

    @Override
    public String taskKey() {
        return KEY;
    }

    @Override
    public TaskRunResult execute(TaskExecutionContext ctx) {
        // Params opcionais (se não vier nada no JSON, segue o padrão do @Scheduled antigo)
        String tag = safeTrim(ctx != null ? ctx.paramString("tag", "AUTO") : "AUTO");
        if (tag == null) tag = "AUTO";

        boolean dryRun = (ctx != null && ctx.paramBool("dryRun", false));

        LOG.info("[HANDLER][TARA] Início. tag={} dryRun={}", tag, dryRun);

        try {
            if (dryRun) {
                LOG.info("[HANDLER][TARA] SKIPPED (dryRun). Nenhuma ação executada. tag={}", tag);
                return TaskRunResult.skipped("dryRun=true. Nenhuma ação executada. tag=" + tag);
            }

            // Equivalente ao taraService.sincronizarComServidor144()
            taraService.sincronizarComServidor144();

            LOG.info("[HANDLER][TARA] OK. Sincronização concluída. tag={}", tag);
            return TaskRunResult.ok("Sincronização TARA (cad_pso_emb -> servidor 144) concluída. tag=" + tag);

        } catch (Exception e) {
            LOG.error("[HANDLER][TARA] FAIL. tag={} msg={}", tag, e.getMessage(), e);
            return TaskRunResult.fail("Erro ao sincronizar TARA com servidor 144. tag=" + tag + " msg=" + e.getMessage());
        }
    }

    private static String safeTrim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}