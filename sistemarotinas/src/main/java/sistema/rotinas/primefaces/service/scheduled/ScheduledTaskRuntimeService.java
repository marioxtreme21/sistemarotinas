package sistema.rotinas.primefaces.service.scheduled;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.Collections;
import java.util.Map;

@Service
public class ScheduledTaskRuntimeService {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private static final int ERR_MAX = 4000;

	public Map<String, Object> parseParams(String json) {
		if (json == null || json.isBlank())
			return Collections.emptyMap();
		try {
			return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
			});
		} catch (Exception e) {
			// params inválido não deve derrubar task; tratamos como vazio
			return Collections.emptyMap();
		}
	}

	public ZoneId resolveZone(String zoneId) {
		try {
			if (zoneId != null && !zoneId.isBlank())
				return ZoneId.of(zoneId.trim());
		} catch (Exception ignore) {
		}
		return ZoneId.systemDefault();
	}

	public LocalDateTime nextRun(String cronExpr, ZoneId zone, LocalDateTime from) {
		CronExpression cron = CronExpression.parse(cronExpr);
		ZonedDateTime zdt = from.atZone(zone);
		ZonedDateTime next = cron.next(zdt);
		return next != null ? next.toLocalDateTime() : null;
	}

	public String truncErr(String s) {
		if (s == null)
			return null;
		if (s.length() <= ERR_MAX)
			return s;
		return s.substring(0, Math.max(0, ERR_MAX - 20)) + "\n... (truncado)";
	}
}