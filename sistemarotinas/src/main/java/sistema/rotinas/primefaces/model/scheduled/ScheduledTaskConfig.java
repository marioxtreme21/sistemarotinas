package sistema.rotinas.primefaces.model.scheduled;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "scheduled_task_config", indexes = { @Index(name = "idx_sched_task_taskkey", columnList = "task_key"),
		@Index(name = "idx_sched_task_owner_next", columnList = "owner,next_run_at"),
		@Index(name = "idx_sched_task_enabled_running_next", columnList = "enabled,running,next_run_at") })
public class ScheduledTaskConfig {

	public static final String OWNER_PROD = "PROD";
	public static final String OWNER_DEV = "DEV";
	public static final String OWNER_ALL = "ALL";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "task_key", nullable = false, length = 120)
	private String taskKey;

	@Column(name = "descricao", length = 255)
	private String descricao;

	@Column(name = "enabled", nullable = false)
	private Boolean enabled = true;

	@Column(name = "owner", nullable = false, length = 32)
	private String owner = OWNER_PROD;

	@Column(name = "cron_expr", nullable = false, length = 120)
	private String cronExpr;

	@Column(name = "zone_id", nullable = false, length = 64)
	private String zoneId = "America/Sao_Paulo";

	@Lob
	@Column(name = "params_json")
	private String paramsJson;

	@Column(name = "next_run_at")
	private LocalDateTime nextRunAt;

	@Column(name = "running", nullable = false)
	private Boolean running = false;

	@Column(name = "last_started_at")
	private LocalDateTime lastStartedAt;

	@Column(name = "last_finished_at")
	private LocalDateTime lastFinishedAt;

	@Column(name = "last_status", length = 16)
	private String lastStatus; // OK | SKIPPED | FAIL

	@Column(name = "last_message", length = 500)
	private String lastMessage;

	@Lob
	@Column(name = "last_error")
	private String lastError;

	@Column(name = "ok_count", nullable = false)
	private Long okCount = 0L;

	@Column(name = "fail_count", nullable = false)
	private Long failCount = 0L;

	// ✅ Agora Hibernate/JPA preenche (não depende de default no banco)
	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	// =========================================================
	// Lifecycle
	// =========================================================

	@PrePersist
	public void prePersist() {
		normalize();
		LocalDateTime now = LocalDateTime.now();
		if (createdAt == null)
			createdAt = now;
		updatedAt = now;
		// nextRunAt normalmente é calculado no service/engine ao salvar
	}

	@PreUpdate
	public void preUpdate() {
		normalize();
		updatedAt = LocalDateTime.now();
	}

	private void normalize() {
		taskKey = trimToNull(taskKey);
		cronExpr = trimToNull(cronExpr);
		descricao = trimToNull(descricao);
		zoneId = trimToNull(zoneId);
		owner = trimToNull(owner);
		paramsJson = trimToNull(paramsJson);

		if (enabled == null)
			enabled = true;
		if (running == null)
			running = false;

		if (zoneId == null)
			zoneId = "America/Sao_Paulo";
		if (owner == null)
			owner = OWNER_PROD;

		owner = owner.trim().toUpperCase();
		if (!OWNER_PROD.equals(owner) && !OWNER_DEV.equals(owner) && !OWNER_ALL.equals(owner)) {
			throw new IllegalArgumentException("owner inválido: " + owner + " (use PROD | DEV | ALL)");
		}

		if (taskKey == null)
			throw new IllegalArgumentException("taskKey é obrigatório.");
		if (cronExpr == null)
			throw new IllegalArgumentException("cronExpr é obrigatório.");
	}

	private static String trimToNull(String s) {
		if (s == null)
			return null;
		String t = s.trim();
		return t.isEmpty() ? null : t;
	}

	// =========================================================
	// Getters/Setters
	// =========================================================

	public Long getId() {
		return id;
	}

	public String getTaskKey() {
		return taskKey;
	}

	public void setTaskKey(String taskKey) {
		this.taskKey = taskKey;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getCronExpr() {
		return cronExpr;
	}

	public void setCronExpr(String cronExpr) {
		this.cronExpr = cronExpr;
	}

	public String getZoneId() {
		return zoneId;
	}

	public void setZoneId(String zoneId) {
		this.zoneId = zoneId;
	}

	public String getParamsJson() {
		return paramsJson;
	}

	public void setParamsJson(String paramsJson) {
		this.paramsJson = paramsJson;
	}

	public LocalDateTime getNextRunAt() {
		return nextRunAt;
	}

	public void setNextRunAt(LocalDateTime nextRunAt) {
		this.nextRunAt = nextRunAt;
	}

	public Boolean getRunning() {
		return running;
	}

	public void setRunning(Boolean running) {
		this.running = running;
	}

	public LocalDateTime getLastStartedAt() {
		return lastStartedAt;
	}

	public void setLastStartedAt(LocalDateTime lastStartedAt) {
		this.lastStartedAt = lastStartedAt;
	}

	public LocalDateTime getLastFinishedAt() {
		return lastFinishedAt;
	}

	public void setLastFinishedAt(LocalDateTime lastFinishedAt) {
		this.lastFinishedAt = lastFinishedAt;
	}

	public String getLastStatus() {
		return lastStatus;
	}

	public void setLastStatus(String lastStatus) {
		this.lastStatus = lastStatus;
	}

	public String getLastMessage() {
		return lastMessage;
	}

	public void setLastMessage(String lastMessage) {
		this.lastMessage = lastMessage;
	}

	public String getLastError() {
		return lastError;
	}

	public void setLastError(String lastError) {
		this.lastError = lastError;
	}

	public Long getOkCount() {
		return okCount;
	}

	public void setOkCount(Long okCount) {
		this.okCount = okCount;
	}

	public Long getFailCount() {
		return failCount;
	}

	public void setFailCount(Long failCount) {
		this.failCount = failCount;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	// =========================================================
	// equals/hashCode/toString
	// =========================================================

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof ScheduledTaskConfig other))
			return false;
		if (this.id != null && other.id != null) {
			return Objects.equals(this.id, other.id);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return (id != null ? id.hashCode() : System.identityHashCode(this));
	}

	@Override
	public String toString() {
		return "ScheduledTaskConfig{" + "id=" + id + ", taskKey='" + taskKey + '\'' + ", descricao='" + descricao + '\''
				+ ", enabled=" + enabled + ", owner='" + owner + '\'' + ", cronExpr='" + cronExpr + '\'' + ", zoneId='"
				+ zoneId + '\'' + ", nextRunAt=" + nextRunAt + ", running=" + running + ", lastStatus='" + lastStatus
				+ '\'' + '}';
	}
}