package sistema.rotinas.primefaces.dto.rotina.price;

import java.time.LocalDateTime;
import java.util.List;

import sistema.rotinas.primefaces.enums.EtapaArquivoEnum;
import sistema.rotinas.primefaces.enums.StatusExecucaoEnum;

public class ArquivoEmailDTO {

	private Long execucaoArquivoId;

	private String patternEsperado;
	private String nomeArquivo;
	private Boolean required;

	private StatusExecucaoEnum statusFinal;
	private EtapaArquivoEnum etapaFinal;

	private Long tempoTotalMs;

	private String origem;
	private String destino;

	private String mensagem;
	private String erro;

	// ✅ NOVO
	private LocalDateTime lastModifiedOrigem;
	private LocalDateTime lastModifiedDestino;

	private List<EtapaEmailDTO> etapas;

	public Long getExecucaoArquivoId() {
		return execucaoArquivoId;
	}

	public void setExecucaoArquivoId(Long execucaoArquivoId) {
		this.execucaoArquivoId = execucaoArquivoId;
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

	public StatusExecucaoEnum getStatusFinal() {
		return statusFinal;
	}

	public void setStatusFinal(StatusExecucaoEnum statusFinal) {
		this.statusFinal = statusFinal;
	}

	public EtapaArquivoEnum getEtapaFinal() {
		return etapaFinal;
	}

	public void setEtapaFinal(EtapaArquivoEnum etapaFinal) {
		this.etapaFinal = etapaFinal;
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

	public List<EtapaEmailDTO> getEtapas() {
		return etapas;
	}

	public void setEtapas(List<EtapaEmailDTO> etapas) {
		this.etapas = etapas;
	}
}