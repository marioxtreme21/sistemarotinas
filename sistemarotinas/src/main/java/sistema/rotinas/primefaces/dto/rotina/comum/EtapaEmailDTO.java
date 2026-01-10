package sistema.rotinas.primefaces.dto.rotina.comum;

import java.time.LocalDateTime;

import sistema.rotinas.primefaces.enums.EtapaArquivoEnum;
import sistema.rotinas.primefaces.enums.StatusExecucaoEnum;

public class EtapaEmailDTO {

	private Long etapaId;
	private EtapaArquivoEnum etapa;
	private StatusExecucaoEnum status;

	private LocalDateTime inicioEm;
	private LocalDateTime fimEm;
	private Long tempoTotalMs;

	private String origem;
	private String destino;

	private String mensagem;
	private String erro;

	// ✅ NOVO (se quiser exibir por etapa depois)
	private LocalDateTime lastModifiedOrigem;
	private LocalDateTime lastModifiedDestino;

	public Long getEtapaId() {
		return etapaId;
	}

	public void setEtapaId(Long etapaId) {
		this.etapaId = etapaId;
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
}