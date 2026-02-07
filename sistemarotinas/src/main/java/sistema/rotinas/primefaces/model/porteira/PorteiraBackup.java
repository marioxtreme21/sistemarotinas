// FILE: src/main/java/sistema/rotinas/primefaces/model/porteira/PorteiraBackup.java
package sistema.rotinas.primefaces.model.porteira;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "porteira_backup", indexes = { @Index(name = "idx_pb_porteira", columnList = "porteira_id"),
		@Index(name = "idx_pb_criado_em", columnList = "criado_em") }, uniqueConstraints = {
				// ✅ garante "último backup" por porteira (1 linha por porteira)
				@UniqueConstraint(name = "uk_pb_porteira", columnNames = { "porteira_id" }) })
public class PorteiraBackup implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Porteira origem do backup
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "porteira_id", nullable = false)
	private PorteiraEletronica porteira;

	@Column(name = "criado_em", nullable = false)
	private LocalDateTime criadoEm;

	@Column(name = "total_usuarios", nullable = false)
	private Integer totalUsuarios = 0;

	// (Opcional) status simples
	@Column(name = "status", length = 30)
	private String status; // "OK" | "FALHA" | "PARCIAL"

	// Log da execução (resumo)
	@Lob
	@Column(name = "log_execucao", columnDefinition = "LONGTEXT")
	private String logExecucao;

	public PorteiraBackup() {
	}

	@PrePersist
	public void prePersist() {
		if (criadoEm == null)
			criadoEm = LocalDateTime.now();
		if (totalUsuarios == null)
			totalUsuarios = 0;
	}

	// =========================
	// Getters/Setters
	// =========================

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public PorteiraEletronica getPorteira() {
		return porteira;
	}

	public void setPorteira(PorteiraEletronica porteira) {
		this.porteira = porteira;
	}

	public LocalDateTime getCriadoEm() {
		return criadoEm;
	}

	public void setCriadoEm(LocalDateTime criadoEm) {
		this.criadoEm = criadoEm;
	}

	public Integer getTotalUsuarios() {
		return totalUsuarios;
	}

	public void setTotalUsuarios(Integer totalUsuarios) {
		this.totalUsuarios = totalUsuarios;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getLogExecucao() {
		return logExecucao;
	}

	public void setLogExecucao(String logExecucao) {
		this.logExecucao = logExecucao;
	}
}