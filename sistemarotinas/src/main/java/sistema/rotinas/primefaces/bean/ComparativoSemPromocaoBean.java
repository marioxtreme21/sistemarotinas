package sistema.rotinas.primefaces.bean;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.time.LocalDate;
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
import sistema.rotinas.primefaces.dto.ResultadoSemPromocaoDTO;
import sistema.rotinas.primefaces.service.interfaces.IComparativoSemPromocaoService;

@Component
@Named("comparativoSemPromocaoBean") // 🔧 nome explícito para EL
@SessionScoped
public class ComparativoSemPromocaoBean implements Serializable {
	private static final long serialVersionUID = 1L;

	@Autowired
	private IComparativoSemPromocaoService service;

	private Integer loja144 = 103;
	private Integer nivel50 = 13;
	private LocalDate dataReferencia = LocalDate.of(2025, 8, 29);
	private List<ResultadoSemPromocaoDTO> resultado;
	private StreamedContent arquivoXlsx;

	@PostConstruct
	public void init() {
	}

	public void comparar() {
		try {
			this.resultado = service.comparar(loja144, nivel50, dataReferencia);
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
					"Comparação concluída", "Registros gerados: " + (resultado == null ? 0 : resultado.size())));
		} catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro ao comparar", e.getMessage()));
		}
	}

	public StreamedContent getArquivoXlsx() {
		try {
			byte[] bytes = service.gerarPlanilhaExcel(resultado == null ? List.of() : resultado);
			InputStream is = new ByteArrayInputStream(bytes);
			String nome = String.format("SemPromo_144_%d_vs_50_%d_%s.xlsx", loja144, nivel50, dataReferencia);
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

	public LocalDate getDataReferencia() {
		return dataReferencia;
	}

	public void setDataReferencia(LocalDate dataReferencia) {
		this.dataReferencia = dataReferencia;
	}

	public List<ResultadoSemPromocaoDTO> getResultado() {
		return resultado;
	}
}
