package sistema.rotinas.primefaces.model.rotina;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.*;
import sistema.rotinas.primefaces.enums.EtapaArquivoEnum;
import sistema.rotinas.primefaces.enums.StatusExecucaoEnum;

@Entity
@Table(name = "rotina_execucao_arquivo_etapa")
public class RotinaExecucaoArquivoEtapa implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "etapa_id")
	private Long etapaId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "execucao_arquivo_id", nullable = false)
	private RotinaExecucaoArquivo execucaoArquivo;

	@Enumerated(EnumType.STRING)
	@Column(name = "etapa", nullable = false, length = 60)
	private EtapaArquivoEnum etapa;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private StatusExecucaoEnum status = StatusExecucaoEnum.EM_ANDAMENTO;

	@Column(name = "inicio_em", nullable = false)
	private LocalDateTime inicioEm;

	@Column(name = "fim_em")
	private LocalDateTime fimEm;

	@Column(name = "tempo_total_ms")
	private Long tempoTotalMs;

	@Column(name = "origem", length = 700)
	private String origem;

	@Column(name = "destino", length = 700)
	private String destino;

	@Column(name = "tamanho_origem_bytes")
	private Long tamanhoOrigemBytes;

	@Column(name = "tamanho_destino_bytes")
	private Long tamanhoDestinoBytes;

	@Column(name = "last_modified_origem")
	private LocalDateTime lastModifiedOrigem;

	@Column(name = "last_modified_destino")
	private LocalDateTime lastModifiedDestino;

	@Column(name = "mensagem", columnDefinition = "TEXT")
	private String mensagem;

	@Column(name = "erro", columnDefinition = "TEXT")
	private String erro;

	// ===== getters/setters =====

	public Long getEtapaId() {
		return etapaId;
	}

	public void setEtapaId(Long etapaId) {
		this.etapaId = etapaId;
	}

	public RotinaExecucaoArquivo getExecucaoArquivo() {
		return execucaoArquivo;
	}

	public void setExecucaoArquivo(RotinaExecucaoArquivo execucaoArquivo) {
		this.execucaoArquivo = execucaoArquivo;
	}

	public EtapaArquivoEnum getEtapa() {
		return etapa;
	}

	public void setEtapa(EtapaArquivoEnum etapa) {
		this.etapa = etapa;
	}

	public StatusExecucaoEnum getStatus() {
		return status;
	}

	public void setStatus(StatusExecucaoEnum status) {
		this.status = status;
	}

	public LocalDateTime getInicioEm() {
		return inicioEm;
	}

	public void setInicioEm(LocalDateTime inicioEm) {
		this.inicioEm = inicioEm;
	}

	public LocalDateTime getFimEm() {
		return fimEm;
	}

	public void setFimEm(LocalDateTime fimEm) {
		this.fimEm = fimEm;
	}

	public Long getTempoTotalMs() {
		return tempoTotalMs;
	}

	public void setTempoTotalMs(Long tempoTotalMs) {
		this.tempoTotalMs = tempoTotalMs;
	}

	public String getOrigem() {
		return origem;
	}

	public void setOrigem(String origem) {
		this.origem = origem;
	}

	public String getDestino() {
		return destino;
	}

	public void setDestino(String destino) {
		this.destino = destino;
	}

	public Long getTamanhoOrigemBytes() {
		return tamanhoOrigemBytes;
	}

	public void setTamanhoOrigemBytes(Long tamanhoOrigemBytes) {
		this.tamanhoOrigemBytes = tamanhoOrigemBytes;
	}

	public Long getTamanhoDestinoBytes() {
		return tamanhoDestinoBytes;
	}

	public void setTamanhoDestinoBytes(Long tamanhoDestinoBytes) {
		this.tamanhoDestinoBytes = tamanhoDestinoBytes;
	}

	public LocalDateTime getLastModifiedOrigem() {
		return lastModifiedOrigem;
	}

	public void setLastModifiedOrigem(LocalDateTime lastModifiedOrigem) {
		this.lastModifiedOrigem = lastModifiedOrigem;
	}

	public LocalDateTime getLastModifiedDestino() {
		return lastModifiedDestino;
	}

	public void setLastModifiedDestino(LocalDateTime lastModifiedDestino) {
		this.lastModifiedDestino = lastModifiedDestino;
	}

	public String getMensagem() {
		return mensagem;
	}

	public void setMensagem(String mensagem) {
		this.mensagem = mensagem;
	}

	public String getErro() {
		return erro;
	}

	public void setErro(String erro) {
		this.erro = erro;
	}

	// ============================================================
	// ✅ COMPATIBILIDADE (evita "PropertyNotFound" em xhtml antigos)
	// ============================================================

	/**
	 * Alias para telas antigas que esperavam "execucaoArquivoEtapaId".
	 * Não altera mapeamento (a entity usa field access).
	 */
	@Transient
	public Long getExecucaoArquivoEtapaId() {
		return this.etapaId;
	}

	/**
	 * "ordem" pode ser útil para ordenação/visual no xhtml (se algum lugar usar).
	 * Não persiste; apenas derivado.
	 */
	@Transient
	public Integer getOrdem() {
		return (this.etapa != null ? (this.etapa.ordinal() + 1) : null);
	}

	@Override
	public int hashCode() {
		return Objects.hash(etapaId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof RotinaExecucaoArquivoEtapa))
			return false;
		RotinaExecucaoArquivoEtapa other = (RotinaExecucaoArquivoEtapa) obj;
		return Objects.equals(etapaId, other.etapaId);
	}
}
