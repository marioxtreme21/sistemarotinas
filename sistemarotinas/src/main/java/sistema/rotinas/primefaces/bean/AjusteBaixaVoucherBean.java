package sistema.rotinas.primefaces.bean;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import sistema.rotinas.primefaces.dto.ResultadoRotinaVoucherDTO;
import sistema.rotinas.primefaces.service.interfaces.IAjusteBaixaVoucherService;

@Component
@Named
@SessionScoped
public class AjusteBaixaVoucherBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(AjusteBaixaVoucherBean.class);

    @Autowired
    private IAjusteBaixaVoucherService ajusteBaixaVoucherService;

    private Date dataInicial;
    private Date dataFinal;

    private ResultadoRotinaVoucherDTO resultado;

    @PostConstruct
    public void init() {
        LocalDate hoje = LocalDate.now();
        this.dataInicial = Date.from(hoje.minusDays(2).atStartOfDay(ZoneId.systemDefault()).toInstant());
        this.dataFinal = Date.from(hoje.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public void executar() {
        try {
            LocalDate ini = toLocalDate(dataInicial);
            LocalDate fim = toLocalDate(dataFinal);

            log.info("Executando rotina de baixa voucher (ini={}, fim={})", ini, fim);

            this.resultado = ajusteBaixaVoucherService.executar(ini, fim);

            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso",
                    "Rotina executada. Lidos: " + resultado.getTotalLidosEconect()
                    + " | Inseridos: " + resultado.getTotalMovInseridos()
                    + " | Atualizados: " + resultado.getTotalMovAtualizados()
                    + " | Já existiam: " + resultado.getTotalMovJaExistentes()
                    + " | CPF não encontrado: " + resultado.getTotalClientesNaoEncontrados()
                )
            );

        } catch (Exception ex) {
            log.error("Erro ao executar rotina de baixa voucher", ex);
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro",
                    "Falha ao executar rotina. Verifique logs e tente novamente."));
        }
    }

    private LocalDate toLocalDate(Date d) {
        if (d == null) return null;
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public Date getDataInicial() { return dataInicial; }
    public void setDataInicial(Date dataInicial) { this.dataInicial = dataInicial; }

    public Date getDataFinal() { return dataFinal; }
    public void setDataFinal(Date dataFinal) { this.dataFinal = dataFinal; }

    public ResultadoRotinaVoucherDTO getResultado() { return resultado; }
    public void setResultado(ResultadoRotinaVoucherDTO resultado) { this.resultado = resultado; }
}
