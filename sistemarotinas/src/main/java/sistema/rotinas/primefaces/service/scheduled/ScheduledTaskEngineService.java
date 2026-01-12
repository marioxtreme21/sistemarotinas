package sistema.rotinas.primefaces.service.scheduled;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sistema.rotinas.primefaces.model.scheduled.ScheduledTaskConfig;
import sistema.rotinas.primefaces.repository.scheduled.ScheduledTaskConfigRepository;
import sistema.rotinas.primefaces.scheduled.runtime.ScheduledTaskHandler;
import sistema.rotinas.primefaces.scheduled.runtime.TaskExecutionContext;
import sistema.rotinas.primefaces.scheduled.runtime.TaskRunResult;
import sistema.rotinas.primefaces.service.interfaces.scheduled.IScheduledTaskEngineService;

@Service
public class ScheduledTaskEngineService implements IScheduledTaskEngineService {

    private static final Logger LOG = LoggerFactory.getLogger("SCHEDULER_DB");

    @Value("${app.instance:PROD}")
    private String instance;

    @Value("${app.allow.scheduler.execution:true}")
    private boolean allowExecution;

    /**
     * Se true: SKIPPED conta como OK (incrementa okCount).
     * Se false: SKIPPED não incrementa okCount nem failCount.
     */
    @Value("${app.scheduler.countSkippedAsOk:true}")
    private boolean countSkippedAsOk;

    private final ScheduledTaskConfigRepository repo;
    private final ScheduledTaskRuntimeService runtime;

    /**
     * Catálogo simples por key normalizada (UPPER/TRIM).
     */
    private final Map<String, ScheduledTaskHandler> handlersByKey;

    public ScheduledTaskEngineService(ScheduledTaskConfigRepository repo,
                                     ScheduledTaskRuntimeService runtime,
                                     List<ScheduledTaskHandler> handlers) {
        this.repo = repo;
        this.runtime = runtime;

        Map<String, ScheduledTaskHandler> map = new HashMap<>();
        if (handlers != null) {
            for (ScheduledTaskHandler h : handlers) {
                if (h == null) continue;

                String raw = h.taskKey();
                if (raw == null || raw.trim().isEmpty()) continue;

                String key = normalizeKey(raw);

                // Protege duplicidade
                if (map.containsKey(key)) {
                    throw new IllegalStateException(
                        "Handler duplicado para taskKey=" + key +
                        " (" + map.get(key).getClass().getName() + " e " + h.getClass().getName() + ")"
                    );
                }

                map.put(key, h);
            }
        }
        this.handlersByKey = Map.copyOf(map);

        LOG.info("[DBSCHED][ENGINE] handlers carregados={}", handlersByKey.size());
    }

    @Override
    @Transactional
    public void executarUmaVez(Long scheduledTaskId) {

        if (scheduledTaskId == null) {
            throw new IllegalArgumentException("Informe o ID da task.");
        }

        if (!allowExecution) {
            throw new IllegalStateException(
                "Execução bloqueada (app.allow.scheduler.execution=false). instance=" + normalizeOwner(instance)
            );
        }

        ScheduledTaskConfig cfg = repo.findById(scheduledTaskId)
            .orElseThrow(() -> new IllegalArgumentException("Task não encontrada. id=" + scheduledTaskId));

        String inst = normalizeOwner(instance);
        String owner = normalizeOwner(cfg.getOwner());

        // Regra de owner: só executa se owner=ALL ou owner=instance
        if (!"ALL".equals(owner) && !owner.equals(inst)) {
            throw new IllegalStateException(
                "Task bloqueada por OWNER. taskOwner=" + owner + " instance=" + inst + " (permite ALL ou igual)"
            );
        }

        LOG.info("[DBSCHED][ENGINE] executarUmaVez | id={} key={} enabled={} running={} owner={} instance={}",
            cfg.getId(), cfg.getTaskKey(), cfg.getEnabled(), cfg.getRunning(), cfg.getOwner(), inst);

        // claim (evita concorrência)
        LocalDateTime now = LocalDateTime.now();
        int claimed = repo.claim(cfg.getId(), now);

        if (claimed != 1) {
            ScheduledTaskConfig atual = repo.findById(cfg.getId()).orElse(cfg);
            throw new IllegalStateException(
                "Não foi possível iniciar execução (claim=0). " +
                "enabled=" + atual.getEnabled() + " running=" + atual.getRunning() +
                " owner=" + atual.getOwner() + " instance=" + inst
            );
        }

        long t0 = System.currentTimeMillis();

        String rawKey = cfg.getTaskKey();
        String key = normalizeKey(rawKey);

        ScheduledTaskHandler handler = handlersByKey.get(key);

        ZoneId zone = safeZone(cfg.getZoneId());
        LocalDateTime nextRun = null;

        String status = TaskRunResult.FAIL;
        String message = null;
        String error = null;

        long okInc = 0;
        long failInc = 0;

        try {
            if (handler == null) {
                status = TaskRunResult.FAIL;
                message = "Handler não encontrado para task_key=" + rawKey;
                failInc = 1;
                LOG.warn("[DBSCHED][ENGINE][{}] {}", key, message);
            } else {

                Map<String, Object> params = runtime.parseParams(cfg.getParamsJson());
                TaskExecutionContext ctx = new TaskExecutionContext(cfg, params);

                TaskRunResult result = handler.execute(ctx);

                status = safeStatus(result != null ? result.getStatus() : null);
                message = (result != null ? result.getMessage() : null);
                error = (result != null ? result.getError() : null);

                if (TaskRunResult.OK.equals(status)) {
                    okInc = 1;
                } else if (TaskRunResult.SKIPPED.equals(status)) {
                    if (countSkippedAsOk) okInc = 1; // configurável
                } else {
                    failInc = 1;
                }

                LOG.info("[DBSCHED][ENGINE][{}] status={} msg={}", key, status, message);
            }

        } catch (Exception e) {
            status = TaskRunResult.FAIL;
            failInc = 1;
            message = "Exception ao executar";
            error = runtime.truncErr(stackTrace(e));
            LOG.error("[DBSCHED][ENGINE][{}] FAIL msg={}", key, e.getMessage(), e);

        } finally {

            try {
                // recalcula próximo agendamento com base no cron
                nextRun = runtime.nextRun(cfg.getCronExpr(), zone, LocalDateTime.now());
            } catch (Exception e) {
                String extra = "Falha ao calcular nextRun: " + e.getMessage();
                error = runtime.truncErr((error == null ? extra : (error + "\n" + extra)));
                nextRun = null;
                status = TaskRunResult.FAIL;
                failInc = Math.max(failInc, 1);
                okInc = 0; // se falhou no nextRun, marca fail
                LOG.warn("[DBSCHED][ENGINE][{}] cron inválido ou erro ao calcular nextRun: {}", key, e.getMessage());
            }

            long ms = System.currentTimeMillis() - t0;
            String msgFinal = (message != null && !message.isBlank())
                ? (message + " | tempoMs=" + ms)
                : ("tempoMs=" + ms);

            repo.finish(cfg.getId(), LocalDateTime.now(), status, msgFinal, runtime.truncErr(error), nextRun, okInc, failInc);

            LOG.info("[DBSCHED][ENGINE][{}] fim | id={} status={} nextRunAt={} tempoMs={}",
                key, cfg.getId(), status, nextRun, ms);
        }
    }

    private ZoneId safeZone(String zoneId) {
        try {
            return runtime.resolveZone(zoneId);
        } catch (Exception e) {
            LOG.warn("[DBSCHED][ENGINE] zone inválida='{}' => usando ZoneId.systemDefault(). msg={}", zoneId, e.getMessage());
            return ZoneId.systemDefault();
        }
    }

    private static String safeStatus(String s) {
        if (s == null) return TaskRunResult.FAIL;
        String x = s.trim().toUpperCase(Locale.ROOT);
        if (x.isBlank()) return TaskRunResult.FAIL;

        if (TaskRunResult.OK.equals(x) || TaskRunResult.SKIPPED.equals(x) || TaskRunResult.FAIL.equals(x)) {
            return x;
        }
        return TaskRunResult.FAIL;
    }

    private static String normalizeKey(String key) {
        return key == null ? "" : key.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeOwner(String owner) {
        String o = (owner == null ? "" : owner.trim().toUpperCase(Locale.ROOT));
        if (o.isBlank()) return "PROD";
        return o;
    }

    private static String stackTrace(Throwable t) {
        try {
            StringWriter sw = new StringWriter(4096);
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            pw.flush();
            return sw.toString();
        } catch (Exception e) {
            return String.valueOf(t != null ? t.getMessage() : "null");
        }
    }
}