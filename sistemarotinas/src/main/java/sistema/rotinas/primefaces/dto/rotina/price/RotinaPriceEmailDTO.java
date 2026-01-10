package sistema.rotinas.primefaces.dto.rotina.price;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import sistema.rotinas.primefaces.dto.rotina.comum.LojaEmailDTO;
import sistema.rotinas.primefaces.enums.OrigemExecucaoEnum;
import sistema.rotinas.primefaces.enums.StatusExecucaoEnum;
import sistema.rotinas.primefaces.enums.TipoRotinaEnum;

public class RotinaPriceEmailDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	private Long execucaoId;
	private TipoRotinaEnum tipo;
	private OrigemExecucaoEnum origem;
	private StatusExecucaoEnum status;

	private String solicitante;
	private LocalDateTime inicioEm;
	private LocalDateTime fimEm;
	private Long tempoTotalMs;

	private String mensagemResumo;
	private String erroGeral;

	private List<LojaEmailDTO> lojas = new ArrayList<>();

	private Integer totalLojas;
	private Integer lojasSucesso;
	private Integer lojasParcial;
	private Integer lojasFalha;

	private Integer totalArquivos;
	private Integer arquivosSucesso;
	private Integer arquivosParcial;
	private Integer arquivosFalha;

	public Long getExecucaoId() {
		return execucaoId;
	}

	public void setExecucaoId(Long execucaoId) {
		this.execucaoId = execucaoId;
	}

	public TipoRotinaEnum getTipo() {
		return tipo;
	}

	public void setTipo(TipoRotinaEnum tipo) {
		this.tipo = tipo;
	}

	public OrigemExecucaoEnum getOrigem() {
		return origem;
	}

	public void setOrigem(OrigemExecucaoEnum origem) {
		this.origem = origem;
	}

	public StatusExecucaoEnum getStatus() {
		return status;
	}

	public void setStatus(StatusExecucaoEnum status) {
		this.status = status;
	}

	public String getSolicitante() {
		return solicitante;
	}

	public void setSolicitante(String solicitante) {
		this.solicitante = solicitante;
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

	public String getMensagemResumo() {
		return mensagemResumo;
	}

	public void setMensagemResumo(String mensagemResumo) {
		this.mensagemResumo = mensagemResumo;
	}

	public String getErroGeral() {
		return erroGeral;
	}

	public void setErroGeral(String erroGeral) {
		this.erroGeral = erroGeral;
	}

	public List<LojaEmailDTO> getLojas() {
		return lojas;
	}

	public void setLojas(List<LojaEmailDTO> lojas) {
		this.lojas = lojas;
	}

	public Integer getTotalLojas() {
		return totalLojas;
	}

	public void setTotalLojas(Integer totalLojas) {
		this.totalLojas = totalLojas;
	}

	public Integer getLojasSucesso() {
		return lojasSucesso;
	}

	public void setLojasSucesso(Integer lojasSucesso) {
		this.lojasSucesso = lojasSucesso;
	}

	public Integer getLojasParcial() {
		return lojasParcial;
	}

	public void setLojasParcial(Integer lojasParcial) {
		this.lojasParcial = lojasParcial;
	}

	public Integer getLojasFalha() {
		return lojasFalha;
	}

	public void setLojasFalha(Integer lojasFalha) {
		this.lojasFalha = lojasFalha;
	}

	public Integer getTotalArquivos() {
		return totalArquivos;
	}

	public void setTotalArquivos(Integer totalArquivos) {
		this.totalArquivos = totalArquivos;
	}

	public Integer getArquivosSucesso() {
		return arquivosSucesso;
	}

	public void setArquivosSucesso(Integer arquivosSucesso) {
		this.arquivosSucesso = arquivosSucesso;
	}

	public Integer getArquivosParcial() {
		return arquivosParcial;
	}

	public void setArquivosParcial(Integer arquivosParcial) {
		this.arquivosParcial = arquivosParcial;
	}

	public Integer getArquivosFalha() {
		return arquivosFalha;
	}

	public void setArquivosFalha(Integer arquivosFalha) {
		this.arquivosFalha = arquivosFalha;
	}
}
