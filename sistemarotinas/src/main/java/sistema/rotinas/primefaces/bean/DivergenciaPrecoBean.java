package sistema.rotinas.primefaces.bean;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import sistema.rotinas.primefaces.dto.DivergenciaPrecoDTO;
import sistema.rotinas.primefaces.service.interfaces.IDivergenciaPrecoService;

@Component
@Named
@SessionScoped
public class DivergenciaPrecoBean implements Serializable {
	private static final long serialVersionUID = 1L;

	@Autowired
	private IDivergenciaPrecoService service;

	private Integer loja144 = 103; // conforme prints
	private Integer nivel50 = 13; // conforme prints
	private List<DivergenciaPrecoDTO> resultado;
	private StreamedContent arquivoXlsx;

	@PostConstruct
	public void init() {
	}

	public void comparar() {
		try {
			this.resultado = service.comparar(loja144, nivel50, LocalDateTime.now());
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
					"Comparação concluída", "Itens divergentes: " + (resultado == null ? 0 : resultado.size())));
		} catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro ao comparar", e.getMessage()));
		}
	}

	public StreamedContent getArquivoXlsx() {
		try {
			byte[] bytes = service.gerarPlanilhaExcel(resultado == null ? List.of() : resultado);
			InputStream is = new ByteArrayInputStream(bytes);
			String nome = String.format("DivergenciaPreco_144_%d_vs_50_%d.xlsx", loja144, nivel50);
			this.arquivoXlsx = DefaultStreamedContent.builder().name(nome)
					.contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet").stream(() -> is)
					.build();
			return arquivoXlsx;
		} catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro ao gerar XLSX", e.getMessage()));
			return null;
		}
	}

	// getters/setters
	public Integer getLoja144() {
		return loja144;
	}

	public void setLoja144(Integer loja144) {
		this.loja144 = loja144;
	}

	public Integer getNivel50() {
		return nivel50;
	}

	public void setNivel50(Integer nivel50) {
		this.nivel50 = nivel50;
	}

	public List<DivergenciaPrecoDTO> getResultado() {
		return resultado;
	}
}
