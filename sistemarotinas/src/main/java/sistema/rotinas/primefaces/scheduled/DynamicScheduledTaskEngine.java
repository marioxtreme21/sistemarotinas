package sistema.rotinas.primefaces.scheduled;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import sistema.rotinas.primefaces.model.scheduled.ScheduledTaskConfig;
import sistema.rotinas.primefaces.repository.scheduled.ScheduledTaskConfigRepository;
import sistema.rotinas.primefaces.scheduled.runtime.ScheduledTaskHandler;
import sistema.rotinas.primefaces.scheduled.runtime.TaskExecutionContext;
import sistema.rotinas.primefaces.scheduled.runtime.TaskRunResult;
import sistema.rotinas.primefaces.service.scheduled.ScheduledTaskRuntimeService;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DynamicScheduledTaskEngine {

    private static final Logger LOG = LoggerFactory.getLogger("SCHEDULER_DB");

    @Value("${app.instance:PROD}")
    private String instance;

    @Value("${app.allow.scheduler.execution:true}")
    private boolean allowExecution;

    private final ScheduledTaskConfigRepository repo;
    private final ScheduledTaskRuntimeService runtime;
    private final Map<String, ScheduledTaskHandler> handlersByKey = new HashMap<>();

    // ✅ throttle de log (não spamma a cada poll)
    private static LocalDateTime lastOwnerWarnAt = null;

    public DynamicScheduledTaskEngine(ScheduledTaskConfigRepository repo,
                                     ScheduledTaskRuntimeService runtime,
                                     List<ScheduledTaskHandler> handlers) {
        this.repo = repo;
        this.runtime = runtime;

        if (handlers != null) {
            for (ScheduledTaskHandler h : handlers) {
                if (h != null && h.taskKey() != null && !h.taskKey().isBlank()) {
                    handlersByKey.put(h.taskKey(), h);
                }
            }
        }

        LOG.info("[DBSCHED] Engine iniciado. handlersRegistrados={}", handlersByKey.size());
    }

    @Scheduled(fixedDelayString = "${scheduler.poll-ms:15000}")
    @Transactional
    public void pollAndRun() {

        if (!allowExecution) {
            LOG.debug("[DBSCHED] execução bloqueada (app.allow.scheduler.execution=false) instance={}", instance);
            return;
        }

        String inst = normalizeInstance(instance);
        LocalDateTime now = LocalDateTime.now();

        // ✅ loga inconsistências de owner (no máx 1x por hora)
        logOwnerNullOrBlankThrottled();

        // owners aceitos para esta instância (o ALL já é tratado no SQL)
        List<String> owners = List.of(inst);

        List<ScheduledTaskConfig> due = repo.findDue(now, owners, PageRequest.of(0, 50));
        if (due == null || due.isEmpty()) return;

        LOG.info("[DBSCHED] due={} instance={} now={}", due.size(), inst, now);

        for (ScheduledTaskConfig cfg : due) {
            runOne(cfg, now, inst);
        }
    }

    private void logOwnerNullOrBlankThrottled() {
        LocalDateTime now = LocalDateTime.now();

        // 1x por hora
        if (lastOwnerWarnAt != null && lastOwnerWarnAt.plusHours(1).isAfter(now)) {
            return;
        }

        long qtd = 0;
        try {
            qtd = repo.countOwnerNullOrBlank();
        } catch (Exception e) {
            // não derruba engine por causa disso
            LOG.debug("[DBSCHED] Falha ao contar owner null/blank: {}", e.getMessage());
            return;
        }

        if (qtd <= 0) {
            lastOwnerWarnAt = now;
            return;
        }

        // lista alguns exemplos
        List<ScheduledTaskConfig> exemplos;
        try {
            exemplos = repo.listOwnerNullOrBlank(PageRequest.of(0, 5));
        } catch (Exception e) {
            exemplos = List.of();
        }

        LOG.warn("[DBSCHED] Encontrado owner NULL/blank no banco. total={} (exibindo até 5)", qtd);
        for (ScheduledTaskConfig t : exemplos) {
            LOG.warn("[DBSCHED] owner inválido | id={} taskKey={} cronExpr={} enabled={} running={} nextRunAt={}",
                    t != null ? t.getId() : null,
                    t != null ? t.getTaskKey() : null,
                    t != null ? t.getCronExpr() : null,
                    t != null ? t.getEnabled() : null,
                    t != null ? t.getRunning() : null,
                    t != null ? t.getNextRunAt() : null);
        }

        lastOwnerWarnAt = now;
    }

    private void runOne(ScheduledTaskConfig cfg, LocalDateTime now, String inst) {
        if (cfg == null) return;

        Long id = cfg.getId();
        String key = cfg.getTaskKey();

        // claim (evita concorrência DEV/PROD no mesmo banco)
        int claimed = repo.claim(id, now);
        if (claimed != 1) {
            return;
        }

        long t0 = System.currentTimeMillis();

        ScheduledTaskHandler handler = handlersByKey.get(key);
        ZoneId zone = runtime.resolveZone(cfg.getZoneId());

        LocalDateTime nextRun = null;
        String status = TaskRunResult.FAIL;
        String message = null;
        String error = null;
        long okInc = 0, failInc = 0;

        try {

            if (cfg.getOwner() == null || cfg.getOwner().isBlank()) {
                LOG.warn("[DBSCHED][{}] owner NULL/blank na task em execução. id={} instance={}", key, id, inst);
            }

            if (handler == null) {
                status = TaskRunResult.FAIL;
                message = "Handler não encontrado para task_key=" + key;
                failInc = 1;
                LOG.warn("[DBSCHED][{}] {}", key, message);

            } else {
                Map<String, Object> params = runtime.parseParams(cfg.getParamsJson());
                TaskExecutionContext ctx = new TaskExecutionContext(cfg, params);

                TaskRunResult result = handler.execute(ctx);

                status = safeStatus(result != null ? result.getStatus() : null);
                message = safeMessage(result != null ? result.getMessage() : null);

                if (TaskRunResult.OK.equals(status) || TaskRunResult.SKIPPED.equals(status)) okInc = 1;
                else failInc = 1;

                if (result != null && result.getError() != null && !result.getError().isBlank()) {
                    error = runtime.truncErr(result.getError());
                }

                LOG.info("[DBSCHED][{}] status={} msg={}", key, status, message);
            }

        } catch (Exception e) {
            status = TaskRunResult.FAIL;
            failInc = 1;
            message = "Exception ao executar";
            error = runtime.truncErr(stackTrace(e));
            LOG.error("[DBSCHED][{}] FAIL msg={}", key, e.getMessage(), e);

        } finally {
            try {
                nextRun = runtime.nextRun(cfg.getCronExpr(), zone, LocalDateTime.now());
            } catch (Exception e) {
                String extra = "Falha ao calcular nextRun: " + e.getMessage();
                error = runtime.truncErr((error == null ? extra : (error + "\n" + extra)));
                nextRun = null;
                status = TaskRunResult.FAIL;
                failInc = Math.max(failInc, 1);
                LOG.warn("[DBSCHED][{}] cron inválido ou erro ao calcular nextRun: {}", key, e.getMessage());
            }

            long ms = System.currentTimeMillis() - t0;

            String msgFinal = (message != null && !message.isBlank())
                    ? (message + " | tempoMs=" + ms)
                    : ("tempoMs=" + ms);

            repo.finish(id, LocalDateTime.now(), status, msgFinal, error, nextRun, okInc, failInc);
        }
    }

    private static String normalizeInstance(String v) {
        if (v == null || v.isBlank()) {
            return "PROD";
        }
        return v.trim().toUpperCase();
    }

    /**
     * ✅ FIX do erro:
     * aceita Object (String/Enum/etc) e normaliza em OK/SKIPPED/FAIL
     */
    private static String safeStatus(Object raw) {
        if (raw == null) return TaskRunResult.FAIL;

        String s = String.valueOf(raw).trim().toUpperCase();
        if (s.isEmpty()) return TaskRunResult.FAIL;

        if (s.equals("OK") || s.equals("SUCCESS") || s.equals("SUCESSO")) return TaskRunResult.OK;
        if (s.equals("SKIPPED") || s.equals("PULADO")) return TaskRunResult.SKIPPED;
        if (s.equals("FAIL") || s.equals("FAILED") || s.equals("FALHA") || s.equals("ERROR")) return TaskRunResult.FAIL;

        return TaskRunResult.FAIL;
    }

    private static String safeMessage(String msg) {
        if (msg == null) return null;
        String s = msg.trim();
        return s.isBlank() ? null : s;
    }

    private static String stackTrace(Throwable t) {
        try {
            StringWriter sw = new StringWriter(4096);
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            pw.flush();
            return sw.toString();
        } catch (Exception e) {
            return String.valueOf(t.getMessage());
        }
    }
}