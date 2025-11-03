package sistema.rotinas.primefaces.bean;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
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
import sistema.rotinas.primefaces.dto.ProdutoNivelPrecoDTO;
import sistema.rotinas.primefaces.service.interfaces.IComparativoProdutosService;

@Component
@Named
@SessionScoped
public class ProdutoComparativoBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Autowired
    private IComparativoProdutosService comparativoService;

    // Níveis padrão conforme solicitado
    private Integer nivel144 = 103; // host 10.1.1.144
    private Integer nivel50  = 13;  // host 10.1.1.50

    private List<ProdutoNivelPrecoDTO> resultado;
    private StreamedContent arquivoXlsx;

    @PostConstruct
    public void init() { }

    public void comparar() {
        try {
            this.resultado = comparativoService.eansPresentes144Ausentes50(nivel144, nivel50);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Comparativo executado",
                            "Registros encontrados: " + (resultado == null ? 0 : resultado.size())));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro ao comparar", e.getMessage()));
        }
    }

    public StreamedContent getArquivoXlsx() {
        try {
            if (resultado == null || resultado.isEmpty()) {
                // Gera mesmo assim, planilha vazia com cabeçalho
                this.resultado = comparativoService.eansPresentes144Ausentes50(nivel144, nivel50);
            }
            byte[] bytes = comparativoService.gerarPlanilhaExcel(resultado);
            InputStream is = new ByteArrayInputStream(bytes);

            String nome = String.format("Comparativo_144x50_%d_vs_%d.xlsx", nivel144, nivel50);
            this.arquivoXlsx = DefaultStreamedContent.builder()
                    .name(nome)
                    .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .stream(() -> is)
                    .build();
            return this.arquivoXlsx;
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro ao gerar XLSX", e.getMessage()));
            return null;
        }
    }

    // getters/setters
    public Integer getNivel144() { return nivel144; }
    public void setNivel144(Integer nivel144) { this.nivel144 = nivel144; }

    public Integer getNivel50() { return nivel50; }
    public void setNivel50(Integer nivel50) { this.nivel50 = nivel50; }

    public List<ProdutoNivelPrecoDTO> getResultado() { return resultado; }
}
