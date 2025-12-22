package sistema.rotinas.primefaces.bean;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import sistema.rotinas.primefaces.model.Loja;
import sistema.rotinas.primefaces.service.DivergenciaPrecoCrmService;
import sistema.rotinas.primefaces.service.interfaces.ILojaService;

import java.io.Serializable;
import java.util.List;

@Component
@Named
@SessionScoped
public class DivergenciaPrecoCrmBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Autowired
    private ILojaService lojaService;

    @Autowired
    private DivergenciaPrecoCrmService divergenciaPrecoCrmService;

    private List<Loja> lojas;
    private Loja lojaSelecionada;
    private boolean todas;

    @PostConstruct
    public void init() {
        // ✅ Se quiser respeitar permissões do usuário, troque por:
        // lojas = lojaService.getLojasPermitidasDoUsuarioLogado();
        lojas = lojaService.getAllLojas();

        // Padrão: todas selecionado (combo desabilitado)
        todas = true;
    }

    /**
     * ✅ Chamado quando marca/desmarca "Todas as lojas"
     * Se marcar "todas", limpamos a seleção para evitar confusão.
     */
    public void onToggleTodas() {
        if (todas) {
            lojaSelecionada = null;
        }
    }

    public void executar() {
        try {
            Loja alvo = todas ? null : lojaSelecionada;

            if (!todas && alvo == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN,
                                "Atenção", "Selecione uma loja ou marque 'Todas as lojas'."));
                return;
            }

            divergenciaPrecoCrmService.executarManual(alvo);

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Concluído",
                            "Rotina executada. Se houve divergências, o e-mail foi enviado com anexos."));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Erro",
                            "Falha ao executar rotina: " + e.getMessage()));
        }
    }

    public List<Loja> getLojas() {
        return lojas;
    }

    public Loja getLojaSelecionada() {
        return lojaSelecionada;
    }

    public void setLojaSelecionada(Loja lojaSelecionada) {
        this.lojaSelecionada = lojaSelecionada;
    }

    public boolean isTodas() {
        return todas;
    }

    public void setTodas(boolean todas) {
        this.todas = todas;
    }
}
