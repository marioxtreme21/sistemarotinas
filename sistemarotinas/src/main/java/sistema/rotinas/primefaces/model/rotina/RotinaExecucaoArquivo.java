package sistema.rotinas.primefaces.model.rotina;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;

import jakarta.persistence.*;
import sistema.rotinas.primefaces.enums.EtapaArquivoEnum;
import sistema.rotinas.primefaces.enums.StatusExecucaoEnum;
import sistema.rotinas.primefaces.model.Loja;

@Entity
@Table(name = "rotina_execucao_arquivo")
public class RotinaExecucaoArquivo implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "execucao_arquivo_id")
	private Long execucaoArquivoId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "execucao_id", nullable = false)
	private RotinaExecucao execucao;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "execucao_loja_id")
	private RotinaExecucaoLoja execucaoLoja;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "loja_id")
	private Loja loja;

	@Column(name = "cod_loja_rms", length = 60)
	private String codLojaRms;

	@Column(name = "pattern_esperado", length = 200)
	private String patternEsperado;

	@Column(name = "nome_arquivo", length = 255)
	private String nomeArquivo;

	@Column(name = "required", nullable = false)
	private Boolean required = true;

	@Enumerated(EnumType.STRING)
	@Column(name = "etapa", nullable = false, length = 30)
	private EtapaArquivoEnum etapa = EtapaArquivoEnum.VALIDACAO_ARQUIVOS;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private StatusExecucaoEnum status = StatusExecucaoEnum.EM_ANDAMENTO;

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

	/**
	 * true -> atualizado false -> desatualizado null -> não verificado
	 */
	@Column(name = "origem_atualizada")
	private Boolean origemAtualizada;

	@Column(name = "inicio_em", nullable = false)
	private LocalDateTime inicioEm;

	@Column(name = "fim_em")
	private LocalDateTime fimEm;

	@Column(name = "tempo_total_ms")
	private Long tempoTotalMs;

	@Column(name = "mensagem", columnDefinition = "TEXT")
	private String mensagem;

	@Column(name = "erro", columnDefinition = "TEXT")
	private String erro;

	// ==========================================================
	// ✅ Getters "View" (JSF) - DateTimeConverter precisa de Date
	// ==========================================================

	@Transient
	public Date getLastModifiedOrigemDate() {
		return toDate(lastModifiedOrigem);
	}

	@Transient
	public Date getLastModifiedDestinoDate() {
		return toDate(lastModifiedDestino);
	}

	@Transient
	public Date getInicioDate() {
		return toDate(inicioEm);
	}

	@Transient
	public Date getFimDate() {
		return toDate(fimEm);
	}

	private static Date toDate(LocalDateTime ldt) {
		if (ldt == null)
			return null;
		return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
	}

	// ========== Getters/Setters ==========

	public Long getExecucaoArquivoId() {
		return execucaoArquivoId;
	}

	public void setExecucaoArquivoId(Long execucaoArquivoId) {
		this.execucaoArquivoId = execucaoArquivoId;
	}

	public RotinaExecucao getExecucao() {
		return execucao;
	}

	public void setExecucao(RotinaExecucao execucao) {
		this.execucao = execucao;
	}

	public RotinaExecucaoLoja getExecucaoLoja() {
		return execucaoLoja;
	}

	public void setExecucaoLoja(RotinaExecucaoLoja execucaoLoja) {
		this.execucaoLoja = execucaoLoja;
	}

	public Loja getLoja() {
		return loja;
	}

	public void setLoja(Loja loja) {
		this.loja = loja;
	}

	public String getCodLojaRms() {
		return codLojaRms;
	}

	public void setCodLojaRms(String codLojaRms) {
		this.codLojaRms = codLojaRms;
	}

	public String getPatternEsperado() {
		return patternEsperado;
	}

	public void setPatternEsperado(String patternEsperado) {
		this.patternEsperado = patternEsperado;
	}

	public String getNomeArquivo() {
		return nomeArquivo;
	}

	public void setNomeArquivo(String nomeArquivo) {
		this.nomeArquivo = nomeArquivo;
	}

	public Boolean getRequired() {
		return required;
	}

	public void setRequired(Boolean required) {
		this.required = required;
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

	public Boolean getOrigemAtualizada() {
		return origemAtualizada;
	}

	public void setOrigemAtualizada(Boolean origemAtualizada) {
		this.origemAtualizada = origemAtualizada;
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

	@Override
	public int hashCode() {
		return Objects.hash(execucaoArquivoId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof RotinaExecucaoArquivo))
			return false;
		RotinaExecucaoArquivo other = (RotinaExecucaoArquivo) obj;
		return Objects.equals(execucaoArquivoId, other.execucaoArquivoId);
	}

	@Override
	public String toString() {
		return "RotinaExecucaoArquivo{id=" + execucaoArquivoId + ", etapa=" + etapa + ", status=" + status + "}";
	}
}
