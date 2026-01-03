package sistema.rotinas.primefaces.bean.rotina;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import sistema.rotinas.primefaces.enums.OrigemExecucaoEnum;
import sistema.rotinas.primefaces.model.Loja;
import sistema.rotinas.primefaces.service.interfaces.ILojaService;
import sistema.rotinas.primefaces.service.interfaces.IRotinaMgvRunnerService;

@Component
@Named
@SessionScoped
public class RotinaMgvExecucaoBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Autowired
    private ILojaService lojaService;

    @Autowired
    private IRotinaMgvRunnerService runner;

    private List<Loja> lojas;
    private List<Long> lojaIdsSelecionadas;

    private boolean selecionarTodas;
    private Long ultimaExecucaoId;

    @PostConstruct
    public void init() {
        this.lojas = new ArrayList<>(lojaService.getAllLojas());
        this.lojaIdsSelecionadas = new ArrayList<>();
        this.selecionarTodas = true;
    }

    public void executar() {
        try {
            List<Long> ids = (selecionarTodas ? null : lojaIdsSelecionadas);
            this.ultimaExecucaoId = runner.executar(ids, OrigemExecucaoEnum.MANUAL, null);
            addMsg(FacesMessage.SEVERITY_INFO, "Rotina MGV", "Execução registrada. ID: " + ultimaExecucaoId);
        } catch (Exception e) {
            addMsg(FacesMessage.SEVERITY_ERROR, "Rotina MGV", "Falha ao executar: " + e.getMessage());
        }
    }

    private void addMsg(FacesMessage.Severity sev, String sum, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(sev, sum, detail));
    }

    // Getters/Setters

    public List<Loja> getLojas() {
        return lojas;
    }

    public List<Long> getLojaIdsSelecionadas() {
        return lojaIdsSelecionadas;
    }

    public void setLojaIdsSelecionadas(List<Long> lojaIdsSelecionadas) {
        this.lojaIdsSelecionadas = lojaIdsSelecionadas;
    }

    public boolean isSelecionarTodas() {
        return selecionarTodas;
    }

    public void setSelecionarTodas(boolean selecionarTodas) {
        this.selecionarTodas = selecionarTodas;
    }

    public Long getUltimaExecucaoId() {
        return ultimaExecucaoId;
    }
}
