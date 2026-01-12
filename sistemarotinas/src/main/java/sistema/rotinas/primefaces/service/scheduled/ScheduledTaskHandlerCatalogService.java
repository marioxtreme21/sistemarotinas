package sistema.rotinas.primefaces.service.scheduled;

import java.util.*;
import org.springframework.stereotype.Service;
import sistema.rotinas.primefaces.scheduled.runtime.ScheduledTaskHandler;

@Service
public class ScheduledTaskHandlerCatalogService {

    private final Map<String, ScheduledTaskHandler> byKey;

    public ScheduledTaskHandlerCatalogService(List<ScheduledTaskHandler> handlers) {
        Map<String, ScheduledTaskHandler> map = new HashMap<>();

        if (handlers != null) {
            for (ScheduledTaskHandler h : handlers) {
                if (h == null) continue;

                String raw = h.taskKey();
                if (raw == null || raw.trim().isEmpty()) continue;

                String key = normalize(raw);

                // protege contra duplicidade (isso é MUITO importante)
                if (map.containsKey(key)) {
                    throw new IllegalStateException(
                        "Handler duplicado para taskKey=" + key +
                        " (" + map.get(key).getClass().getName() + " e " + h.getClass().getName() + ")"
                    );
                }

                map.put(key, h);
            }
        }

        this.byKey = Collections.unmodifiableMap(map);
    }

    public List<String> listHandlerKeys() {
        List<String> keys = new ArrayList<>(byKey.keySet());
        keys.sort(String::compareToIgnoreCase);
        return keys;
    }

    public boolean exists(String taskKey) {
        if (taskKey == null) return false;
        return byKey.containsKey(normalize(taskKey));
    }

    public Optional<ScheduledTaskHandler> get(String taskKey) {
        if (taskKey == null) return Optional.empty();
        return Optional.ofNullable(byKey.get(normalize(taskKey)));
    }

    private static String normalize(String s) {
        return s.trim().toUpperCase(Locale.ROOT);
    }
}