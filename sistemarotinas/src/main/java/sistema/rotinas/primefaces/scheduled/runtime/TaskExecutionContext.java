package sistema.rotinas.primefaces.scheduled.runtime;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import sistema.rotinas.primefaces.model.scheduled.ScheduledTaskConfig;

public class TaskExecutionContext {

    private final ScheduledTaskConfig cfg;
    private final Map<String, Object> params;

    public TaskExecutionContext(ScheduledTaskConfig cfg, Map<String, Object> params) {
        this.cfg = cfg;
        this.params = (params == null ? new HashMap<>() : new HashMap<>(params));
    }

    public ScheduledTaskConfig getCfg() {
        return cfg;
    }

    public String getTaskKey() {
        return (cfg != null ? cfg.getTaskKey() : null);
    }

    public Map<String, Object> getParams() {
        return Collections.unmodifiableMap(params);
    }

    // =========================
    // Helpers de leitura
    // =========================
    public String paramString(String key, String def) {
        Object v = params.get(key);
        if (v == null) return def;
        String s = String.valueOf(v);
        return (s.isBlank() ? def : s);
    }

    public boolean paramBool(String key, boolean def) {
        Object v = params.get(key);
        if (v == null) return def;

        if (v instanceof Boolean b) return b;

        String s = String.valueOf(v).trim().toLowerCase();
        if (s.isBlank()) return def;

        return ("true".equals(s) || "1".equals(s) || "yes".equals(s) || "y".equals(s) || "sim".equals(s));
    }

    public long paramLong(String key, long def) {
        Object v = params.get(key);
        if (v == null) return def;

        if (v instanceof Number n) return n.longValue();

        try {
            return Long.parseLong(String.valueOf(v).trim());
        } catch (Exception e) {
            return def;
        }
    }

    public int paramInt(String key, int def) {
        Object v = params.get(key);
        if (v == null) return def;

        if (v instanceof Number n) return n.intValue();

        try {
            return Integer.parseInt(String.valueOf(v).trim());
        } catch (Exception e) {
            return def;
        }
    }
}