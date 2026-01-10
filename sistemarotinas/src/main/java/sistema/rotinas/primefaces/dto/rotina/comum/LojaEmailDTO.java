package sistema.rotinas.primefaces.dto.rotina.comum;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import sistema.rotinas.primefaces.enums.StatusExecucaoEnum;

public class LojaEmailDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	private Long execucaoLojaId;
	private String codLojaRms;
	private String nomeLoja;

	private StatusExecucaoEnum status;

	private LocalDateTime inicioEm;
	private LocalDateTime fimEm;

	private String mensagem;
	private String erro;

	private List<ArquivoEmailDTO> arquivos = new ArrayList<>();

	public Long getExecucaoLojaId() {
		return execucaoLojaId;
	}

	public void setExecucaoLojaId(Long execucaoLojaId) {
		this.execucaoLojaId = execucaoLojaId;
	}

	public String getCodLojaRms() {
		return codLojaRms;
	}

	public void setCodLojaRms(String codLojaRms) {
		this.codLojaRms = codLojaRms;
	}

	public String getNomeLoja() {
		return nomeLoja;
	}

	public void setNomeLoja(String nomeLoja) {
		this.nomeLoja = nomeLoja;
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

	public List<ArquivoEmailDTO> getArquivos() {
		return arquivos;
	}

	public void setArquivos(List<ArquivoEmailDTO> arquivos) {
		this.arquivos = arquivos;
	}
}
