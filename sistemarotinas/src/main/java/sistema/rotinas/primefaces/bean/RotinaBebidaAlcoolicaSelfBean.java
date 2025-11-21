package sistema.rotinas.primefaces.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import sistema.rotinas.primefaces.dto.ProdutoBebidaAlcoolicaDTO;
import sistema.rotinas.primefaces.service.interfaces.IRotinaBebidaAlcoolicaSelfService;

@Component
@Named("rotinaBebidaAlcoolicaSelfBean")
@SessionScoped
public class RotinaBebidaAlcoolicaSelfBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(RotinaBebidaAlcoolicaSelfBean.class);

    @Autowired
    private IRotinaBebidaAlcoolicaSelfService rotinaBebidaAlcoolicaSelfService;

    private List<ProdutoBebidaAlcoolicaDTO> produtos;
    private boolean executando;

    @PostConstruct
    public void init() {
        log.info("[RotinaBebidaAlcoolicaSelfBean] Inicializando bean.");
        produtos = new ArrayList<>();
        executando = false;
    }

    public void executarRotina() {
        log.info("[RotinaBebidaAlcoolicaSelfBean] Execução manual da rotina solicitada.");
        FacesContext context = FacesContext.getCurrentInstance();

        try {
            executando = true;
            produtos = rotinaBebidaAlcoolicaSelfService.executarRotina();

            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Rotina executada com sucesso",
                    "Total de produtos marcados como bebida alcoólica: " + produtos.size()));

        } catch (Exception e) {
            log.error("[RotinaBebidaAlcoolicaSelfBean] Erro ao executar rotina de bebidas alcoólicas.", e);
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erro ao executar rotina",
                    "Ocorreu um erro ao executar a rotina. Verifique os logs."));
        } finally {
            executando = false;
        }
    }

    // Getters e Setters

    public List<ProdutoBebidaAlcoolicaDTO> getProdutos() {
        return produtos;
    }

    public void setProdutos(List<ProdutoBebidaAlcoolicaDTO> produtos) {
        this.produtos = produtos;
    }

    public boolean isExecutando() {
        return executando;
    }

    public void setExecutando(boolean executando) {
        this.executando = executando;
    }
}
