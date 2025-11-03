package sistema.rotinas.primefaces.bean;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import sistema.rotinas.primefaces.service.FreteNfceService;

@Component
@Named
@SessionScoped
public class FreteNfceBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(FreteNfceBean.class);

    private LocalDate dataInicial;

    @Autowired
    private FreteNfceService freteNfceService;

    @PostConstruct
    public void init() {
        this.dataInicial = LocalDate.now().withDayOfMonth(1); // padrão: 1º dia do mês
        log.info("FreteNfceBean inicializado. dataInicial={}", dataInicial);
    }

    public void executar() {
    	System.out.println("Acessou o metodo Ajustar Frete");
        try {
            if (dataInicial == null) {
                addMsg(FacesMessage.SEVERITY_WARN, "Atenção", "Informe a data inicial.");
                return;
            }

            log.info("Iniciando processamento de fretes: dataInicial={}", dataInicial);
            freteNfceService.processar(dataInicial, BigDecimal.ZERO); // processa qualquer frete > 0
            addMsg(FacesMessage.SEVERITY_INFO, "Sucesso", "Processamento concluído com sucesso.");
        } catch (Exception e) {
            log.error("Erro ao executar processamento de fretes", e);
            addMsg(FacesMessage.SEVERITY_ERROR, "Erro",
                    "Falha ao processar fretes. Verifique os logs e tente novamente.");
        }
    }

    private void addMsg(FacesMessage.Severity severity, String resumo, String detalhe) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, resumo, detalhe));
    }

    // Getter & Setter
    public LocalDate getDataInicial() {
        return dataInicial;
    }

    public void setDataInicial(LocalDate dataInicial) {
        this.dataInicial = dataInicial;
    }
}
