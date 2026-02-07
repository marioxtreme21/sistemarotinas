// FILE: src/main/java/sistema/rotinas/primefaces/bean/porteira/RotinaPorteiraBean.java
package sistema.rotinas.primefaces.bean.porteira;

import java.io.Serializable;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import sistema.rotinas.primefaces.model.porteira.PorteiraEletronica;
import sistema.rotinas.primefaces.service.interfaces.porteira.IPorteiraEletronicaService;
import sistema.rotinas.primefaces.service.porteira.PorteiraEletronicaRuntimeClient;
import sistema.rotinas.primefaces.service.porteira.NotificacaoPorteiraService;

@Component
@Named
@ViewScoped
public class RotinaPorteiraBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger("ROTINA_PORTEIRA");

    private List<PorteiraEletronica> porteiras;
    private PorteiraEletronica porteiraSelecionada;

    private int progress = 0;

    @Autowired
    private IPorteiraEletronicaService porteiraService;

    @Autowired
    private PorteiraEletronicaRuntimeClient runtimeClient;

    @Autowired
    private NotificacaoPorteiraService notificacaoPorteiraService;

    @PostConstruct
    public void init() {
        carregarPorteiras();
    }

    public void carregarPorteiras() {
        porteiras = porteiraService.getAllPorteiras();
    }

    public void ativarAgora() {
        executar("ATIVAR", true);
    }

    public void desativarAgora() {
        executar("DESATIVAR", false);
    }

    private void executar(String acao, boolean ativar) {
        if (porteiraSelecionada == null) {
            msgWarn("Selecione uma porteira.");
            return;
        }

        LOG.info("Solicitação {} - porteiraId={}, descricao={}, ip={}",
                acao, porteiraSelecionada.getId(), porteiraSelecionada.getDescricao(), porteiraSelecionada.getIp());

        PorteiraEletronicaRuntimeClient.RuntimeExecResult r =
                ativar ? runtimeClient.ativar(porteiraSelecionada) : runtimeClient.desativar(porteiraSelecionada);

        boolean ok = (r != null && r.isOk());
        String logDetalhado = (r != null ? r.getLog() : "Retorno nulo do runtimeClient");

        if (ok) {
            LOG.info("{} OK - porteiraId={}, descricao={}", acao, porteiraSelecionada.getId(), porteiraSelecionada.getDescricao());
            msgInfo("Porteira " + (ativar ? "ativada" : "desativada") + ". Veja logs em ROTINA_PORTEIRA.");
        } else {
            LOG.error("{} FALHA - porteiraId={}, descricao={}", acao, porteiraSelecionada.getId(), porteiraSelecionada.getDescricao());
            msgErro("Falha ao " + (ativar ? "ativar" : "desativar") + ". Veja logs em ROTINA_PORTEIRA.");
        }

        // detalhamento vai no e-mail
        notificacaoPorteiraService.notificarAcao(
                porteiraSelecionada,
                acao,
                ok,
                logDetalhado
        );
    }

    private void msgInfo(String s) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "OK", s));
    }

    private void msgWarn(String s) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Atenção", s));
    }

    private void msgErro(String s) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", s));
    }

    public List<PorteiraEletronica> getPorteiras() { return porteiras; }
    public void setPorteiras(List<PorteiraEletronica> porteiras) { this.porteiras = porteiras; }

    public PorteiraEletronica getPorteiraSelecionada() { return porteiraSelecionada; }
    public void setPorteiraSelecionada(PorteiraEletronica porteiraSelecionada) { this.porteiraSelecionada = porteiraSelecionada; }

    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }
}