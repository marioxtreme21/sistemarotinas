package sistema.rotinas.primefaces.bean;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import sistema.rotinas.primefaces.dto.VendaLojaResumo;
import sistema.rotinas.primefaces.service.interfaces.IRelatorioVendasLojasService;

@Component
@Named
@SessionScoped
public class RelatorioVendasLojasBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(RelatorioVendasLojasBean.class);

    private static final DateTimeFormatter BR_DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Autowired
    private IRelatorioVendasLojasService relatorioVendasLojasService;

    // Datas do filtro
    private LocalDate dataInicial;
    private LocalDate dataFinal;

    // Mantido por compatibilidade (service hoje ignora esse filtro)
    private boolean apenasLojasPermitidas = true;

    // Lista exibida na tela
    private List<VendaLojaResumo> listaResultado;

    @PostConstruct
    public void init() {
        LocalDate ontem = LocalDate.now().minusDays(1);
        this.dataInicial = ontem;
        this.dataFinal = ontem;
        this.listaResultado = new ArrayList<>();
    }

    // =========================================================
    // AÇÃO ÚNICA DA TELA: Gera relatório + envia e-mail
    // =========================================================
    public void gerarRelatorio() {
        try {
            log.info("Gerando relatório de vendas por loja. Período: {} a {}, apenasLojasPermitidas={}",
                    dataInicial, dataFinal, apenasLojasPermitidas);

            // 1) Consulta e popula a lista para a tela
            listaResultado = relatorioVendasLojasService
                    .buscarVendasPeriodo(dataInicial, dataFinal, apenasLojasPermitidas);

            int qtdLojas = (listaResultado != null ? listaResultado.size() : 0);

            // 2) Envia o relatório por e-mail (todas as lojas => codLojaEconect = null)
            try {
                relatorioVendasLojasService.enviarRelatorioPorEmail(dataInicial, dataFinal, null);

                addMessage(FacesMessage.SEVERITY_INFO,
                        "Relatório gerado",
                        "Foram encontradas " + qtdLojas
                                + " lojas no período e o relatório foi enviado por e-mail.");

            } catch (Exception eMail) {
                log.error("Relatório gerado, mas ocorreu erro ao enviar e-mail de vendas por loja.", eMail);

                addMessage(FacesMessage.SEVERITY_WARN,
                        "Relatório gerado",
                        "Relatório gerado com sucesso (" + qtdLojas
                                + " lojas), porém houve erro ao enviar o e-mail: "
                                + eMail.getMessage());
            }

        } catch (Exception e) {
            log.error("Erro ao gerar relatório de vendas por loja", e);
            addMessage(FacesMessage.SEVERITY_ERROR,
                    "Erro ao gerar relatório",
                    e.getMessage());
        }
    }

    // =========================================================
    // Util
    // =========================================================
    private void addMessage(FacesMessage.Severity severity, String resumo, String detalhe) {
        FacesContext.getCurrentInstance()
                .addMessage(null, new FacesMessage(severity, resumo, detalhe));
    }

    // =========================================================
    // Getters / Setters "NOVOS"
    // =========================================================
    public LocalDate getDataInicial() {
        return dataInicial;
    }

    public void setDataInicial(LocalDate dataInicial) {
        this.dataInicial = dataInicial;
    }

    public LocalDate getDataFinal() {
        return dataFinal;
    }

    public void setDataFinal(LocalDate dataFinal) {
        this.dataFinal = dataFinal;
    }

    public boolean isApenasLojasPermitidas() {
        return apenasLojasPermitidas;
    }

    public void setApenasLojasPermitidas(boolean apenasLojasPermitidas) {
        this.apenasLojasPermitidas = apenasLojasPermitidas;
    }

    public List<VendaLojaResumo> getListaResultado() {
        return listaResultado;
    }

    public void setListaResultado(List<VendaLojaResumo> listaResultado) {
        this.listaResultado = listaResultado;
    }

    // =========================================================
    // ALIASES para compatibilidade com o XHTML antigo
    // (a página usa dataInicio / dataFim / resultados / periodoFormatado / totalGeral)
    // =========================================================

    public LocalDate getDataInicio() {
        return dataInicial;
    }

    public void setDataInicio(LocalDate dataInicio) {
        this.dataInicial = dataInicio;
    }

    public LocalDate getDataFim() {
        return dataFinal;
    }

    public void setDataFim(LocalDate dataFim) {
        this.dataFinal = dataFim;
    }

    // alias para #{relatorioVendasLojasBean.resultados}
    public List<VendaLojaResumo> getResultados() {
        return listaResultado;
    }

    public void setResultados(List<VendaLojaResumo> resultados) {
        this.listaResultado = resultados;
    }

    // alias para #{relatorioVendasLojasBean.periodoFormatado}
    public String getPeriodoFormatado() {
        if (dataInicial == null || dataFinal == null) {
            return "";
        }
        if (dataInicial.equals(dataFinal)) {
            // Ex.: "17/11/2025"
            return dataInicial.format(BR_DATE);
        }
        // Ex.: "17/11/2025 a 18/11/2025"
        return dataInicial.format(BR_DATE) + " a " + dataFinal.format(BR_DATE);
    }

    // alias para #{relatorioVendasLojasBean.totalGeral}
    // Soma o totalVenda de todas as linhas do relatório
    public BigDecimal getTotalGeral() {
        if (listaResultado == null || listaResultado.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return listaResultado.stream()
                .map(VendaLojaResumo::getTotalVenda)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
