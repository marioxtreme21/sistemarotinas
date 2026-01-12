package sistema.rotinas.primefaces.service.scheduled;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sistema.rotinas.primefaces.model.scheduled.ScheduledTaskConfig;
import sistema.rotinas.primefaces.repository.scheduled.ScheduledTaskConfigRepository;
import sistema.rotinas.primefaces.service.interfaces.scheduled.IScheduledTaskService;

@Service
public class ScheduledTaskService implements IScheduledTaskService {

    private static final Logger LOG = LoggerFactory.getLogger("SCHEDULER_DB");

    private final ScheduledTaskConfigRepository repo;
    private final ScheduledTaskRuntimeService runtime;
    private final ObjectMapper objectMapper;

    public ScheduledTaskService(ScheduledTaskConfigRepository repo,
                                ScheduledTaskRuntimeService runtime,
                                ObjectMapper objectMapper) {
        this.repo = repo;
        this.runtime = runtime;
        this.objectMapper = objectMapper;
    }

    // =========================
    // Listagem
    // =========================

    @Override
    @Transactional(readOnly = true)
    public List<ScheduledTaskConfig> listarTodas() {
        return repo.findAll().stream()
                .sorted(defaultSort())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduledTaskConfig> listarPorRotina(String rotina) {
        String r = normUpper(rotina);
        if (r.isBlank() || "ALL".equals(r)) {
            return listarTodas();
        }

        // ✅ aceita dois padrões:
        // PRICE_*  e  ROTINA_PRICE_*
        return repo.findAll().stream()
                .filter(t -> matchesRotina(nz(t.getTaskKey()), r))
                .sorted(defaultSort())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduledTaskConfig> listarPorTaskKey(String taskKey) {
        String tk = normUpper(taskKey);
        if (tk.isBlank()) return new ArrayList<>();

        return repo.findAll().stream()
                .filter(t -> tk.equalsIgnoreCase(nz(t.getTaskKey())))
                .sorted(Comparator
                        .comparing((ScheduledTaskConfig t) -> nz(t.getCronExpr()))
                        .thenComparing(t -> nz(t.getOwner())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduledTaskConfig findById(Long id) {
        if (id == null) return null;
        return repo.findById(id).orElse(null);
    }

    // =========================
    // Pesquisa (tela)
    // =========================

    @Override
    @Transactional(readOnly = true)
    public List<ScheduledTaskConfig> pesquisar(String rotina,
                                              String owner,
                                              boolean somenteAtivas,
                                              String texto) {

        String r = normUpper(rotina);
        String o = normUpper(owner);
        String q = normUpper(texto);

        return repo.findAll().stream()
                // ✅ filtro rotina (agora aceita ROTINA_PRICE_* também)
                .filter(t -> {
                    if (r.isBlank() || "ALL".equals(r)) return true;
                    return matchesRotina(nz(t.getTaskKey()), r);
                })
                // filtro owner
                .filter(t -> {
                    if (o.isBlank()) return true;
                    return o.equalsIgnoreCase(nz(t.getOwner()));
                })
                // somente ativas
                .filter(t -> !somenteAtivas || Boolean.TRUE.equals(t.getEnabled()))
                // texto em taskKey/descricao
                .filter(t -> {
                    if (q.isBlank()) return true;
                    String tk = nz(t.getTaskKey()).toUpperCase(Locale.ROOT);
                    String ds = nz(t.getDescricao()).toUpperCase(Locale.ROOT);
                    return tk.contains(q) || ds.contains(q);
                })
                .sorted(defaultSort())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> listarTaskKeysCadastradas() {
        return repo.findAll().stream()
                .map(ScheduledTaskConfig::getTaskKey)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> s.toUpperCase(Locale.ROOT))
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    // =========================
    // Persistência / validação
    // =========================

    @Override
    public void validar(ScheduledTaskConfig t) {
        if (t == null) throw new IllegalArgumentException("Task não informada.");

        if (isBlank(t.getTaskKey())) {
            throw new IllegalArgumentException("taskKey é obrigatório.");
        }
        if (isBlank(t.getCronExpr())) {
            throw new IllegalArgumentException("cronExpr é obrigatório.");
        }

        if (isBlank(t.getOwner())) {
            t.setOwner(ScheduledTaskConfig.OWNER_PROD);
            LOG.warn("[SCHED][VALIDACAO] owner estava nulo/vazio. Aplicado default owner=PROD para taskKey={}", t.getTaskKey());
        }

        String ownerNorm = normUpper(t.getOwner());
        if (!ScheduledTaskConfig.OWNER_PROD.equals(ownerNorm)
                && !ScheduledTaskConfig.OWNER_DEV.equals(ownerNorm)
                && !ScheduledTaskConfig.OWNER_ALL.equals(ownerNorm)) {
            throw new IllegalArgumentException("owner inválido: " + ownerNorm + " (use PROD | DEV | ALL)");
        }
        t.setOwner(ownerNorm);

        if (isBlank(t.getZoneId())) {
            t.setZoneId("America/Sao_Paulo");
        }

        if (t.getEnabled() == null) t.setEnabled(Boolean.TRUE);
        if (t.getRunning() == null) t.setRunning(Boolean.FALSE);

        validarCronExpr(t.getCronExpr());
        runtime.resolveZone(t.getZoneId());
        validarJson(t.getParamsJson());
    }

    @Override
    @Transactional
    public ScheduledTaskConfig salvar(ScheduledTaskConfig t) {
        validar(t);

        boolean enabled = Boolean.TRUE.equals(t.getEnabled());

        if (!enabled) {
            t.setNextRunAt(null);
        } else {
            try {
                ZoneId zone = runtime.resolveZone(t.getZoneId());
                LocalDateTime next = runtime.nextRun(t.getCronExpr(), zone, LocalDateTime.now());
                t.setNextRunAt(next);
            } catch (Exception e) {
                LOG.warn("[SCHED][SAVE] Falha ao calcular nextRunAt. taskKey={} cron={} zone={} msg={}",
                        t.getTaskKey(), t.getCronExpr(), t.getZoneId(), e.getMessage());
                t.setNextRunAt(null);
            }
        }

        return repo.save(t);
    }

    @Override
    @Transactional
    public void atualizarEnabled(Long id, boolean enabled) {
        ScheduledTaskConfig t = findById(id);
        if (t == null) throw new IllegalArgumentException("Task não encontrada. id=" + id);

        t.setEnabled(enabled);

        if (!enabled) {
            t.setNextRunAt(null);
        } else {
            if (t.getNextRunAt() == null) {
                try {
                    ZoneId zone = runtime.resolveZone(t.getZoneId());
                    t.setNextRunAt(runtime.nextRun(t.getCronExpr(), zone, LocalDateTime.now()));
                } catch (Exception e) {
                    LOG.warn("[SCHED][ENABLED] Não foi possível recalcular nextRunAt. id={} taskKey={} msg={}",
                            id, t.getTaskKey(), e.getMessage());
                }
            }
        }

        repo.save(t);
    }

    @Override
    @Transactional
    public void atualizarEnabledEmLote(List<Long> ids, boolean enabled) {
        if (ids == null || ids.isEmpty()) return;

        for (Long id : ids) {
            if (id == null) continue;
            try {
                atualizarEnabled(id, enabled);
            } catch (Exception e) {
                LOG.warn("[SCHED][BULK] Falha ao atualizar enabled. id={} enabled={} msg={}", id, enabled, e.getMessage());
            }
        }
    }

    // =========================
    // JSON / Cron
    // =========================

    @Override
    public void validarJson(String paramsJson) {
        String s = (paramsJson == null ? "" : paramsJson.trim());
        if (s.isBlank()) return;

        try {
            JsonNode node = objectMapper.readTree(s);
            if (node == null) {
                throw new IllegalArgumentException("paramsJson inválido (nulo).");
            }
            if (!node.isObject()) {
                throw new IllegalArgumentException("paramsJson deve ser um OBJETO JSON. Ex: {\"tag\":\"AUTO\",\"retry\":false}");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("paramsJson inválido: " + e.getMessage(), e);
        }
    }

    @Override
    public List<LocalDateTime> previewNextRuns(String cronExpr, String zoneId, int total) {
        if (total <= 0) total = 1;

        String cron = (cronExpr == null ? "" : cronExpr.trim());
        if (cron.isBlank()) throw new IllegalArgumentException("cronExpr vazio.");

        ZoneId zone = runtime.resolveZone(zoneId);
        CronExpression exp = CronExpression.parse(cron);

        List<LocalDateTime> out = new ArrayList<>();
        ZonedDateTime base = ZonedDateTime.now(zone);

        for (int i = 0; i < total; i++) {
            base = exp.next(base);
            if (base == null) break;
            out.add(base.toLocalDateTime());
        }
        return out;
    }

    // =========================
    // Helpers internos
    // =========================

    /**
     * ✅ casa com:
     * - PRICE_...
     * - ROTINA_PRICE_...
     */
    private static boolean matchesRotina(String taskKey, String rotinaUpper) {
        String tk = (taskKey == null ? "" : taskKey.trim()).toUpperCase(Locale.ROOT);
        String r = (rotinaUpper == null ? "" : rotinaUpper.trim()).toUpperCase(Locale.ROOT);

        if (r.isBlank() || "ALL".equals(r)) return true;

        return tk.startsWith(r + "_") || tk.startsWith("ROTINA_" + r + "_");
    }

    private static Comparator<ScheduledTaskConfig> defaultSort() {
        return Comparator
                .comparing((ScheduledTaskConfig t) -> nz(t.getTaskKey()).toUpperCase(Locale.ROOT))
                .thenComparing(t -> nz(t.getCronExpr()))
                .thenComparing(t -> nz(t.getOwner()))
                .thenComparing(t -> (t.getId() == null ? Long.MAX_VALUE : t.getId()));
    }

    private static void validarCronExpr(String cronExpr) {
        try {
            CronExpression.parse(cronExpr);
        } catch (Exception e) {
            throw new IllegalArgumentException("cronExpr inválida: " + e.getMessage(), e);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String normUpper(String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }
}