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
import jakarta.faces.model.SelectItem;
import jakarta.inject.Named;
import sistema.rotinas.primefaces.lazy.CarregamentoLazyListForObject;
import sistema.rotinas.primefaces.model.ConfiguracaoEmail;
import sistema.rotinas.primefaces.service.interfaces.IConfiguracaoEmailService;

@Component
@Named
@SessionScoped
public class ConfiguracaoEmailBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ConfiguracaoEmailBean.class);

    private ConfiguracaoEmail configuracaoEmail;
    private CarregamentoLazyListForObject<ConfiguracaoEmail> configuracoesLazy;

    private String campoSelecionado;
    private String condicaoSelecionada;
    private String valorPesquisa;
    private List<SelectItem> camposPesquisa;
    private boolean mostrarFormulario;

    @Autowired
    private IConfiguracaoEmailService configuracaoEmailService;

    @PostConstruct
    public void init() {
        log.info("🚀 Inicializando ConfiguracaoEmailBean");
        mostrarFormulario = false;
        configuracaoEmail = new ConfiguracaoEmail();
        carregarSobDemanda();

        camposPesquisa = new ArrayList<>();
        camposPesquisa.add(new SelectItem("usuarioEmail", "Usuário"));
        camposPesquisa.add(new SelectItem("servidorSmtp", "Servidor SMTP"));
        camposPesquisa.add(new SelectItem("servidorLeitura", "Servidor IMAP/POP"));
    }

    public void carregarSobDemanda() {
        log.debug("🔄 Carregando configurações de email com Lazy");
        configuracoesLazy = new CarregamentoLazyListForObject<>(
                (first, pageSize) -> configuracaoEmailService.getAll(),
                () -> configuracaoEmailService.getAll().size()
        );
    }

    public void prepararNovoCadastro() {
        log.info("🆕 Preparando novo cadastro");
        this.configuracaoEmail = new ConfiguracaoEmail();
        this.mostrarFormulario = true;
    }
    
    public void testarEnvioEmail() {
        try {
            boolean sucesso = configuracaoEmailService.testarEnvioEmail(configuracaoEmail);
            if (sucesso) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", "E-mail de teste enviado com sucesso."));
            } else {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Falha ao enviar e-mail de teste."));
            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao enviar e-mail: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    public void testarLeitura() {
        try {
            boolean sucesso = configuracaoEmailService.testarConexaoLeitura(configuracaoEmail);
            if (sucesso) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", "Conexão com servidor de leitura bem-sucedida."));
            } else {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Falha na conexão de leitura. Verifique os dados."));
            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao testar leitura: " + e.getMessage()));
            e.printStackTrace();
        }
    }


    public void salvar() {
        try {
            configuracaoEmailService.save(configuracaoEmail);
            carregarSobDemanda();
            configuracaoEmail = new ConfiguracaoEmail();
            mostrarFormulario = false;

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", "Configuração salva com sucesso!"));

        } catch (IllegalArgumentException ex) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", ex.getMessage()));
            mostrarFormulario = true;
        } catch (Exception ex) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro",
                            "Ocorreu um erro ao salvar. Verifique os dados e tente novamente."));
            ex.printStackTrace();
            mostrarFormulario = true;
        }
    }

    public void prepararEditar(ConfiguracaoEmail config) {
        log.info("✍️ Editando configuração: {}", config);
        this.configuracaoEmail = config;
        this.mostrarFormulario = true;
    }

    public void excluir(Long id) {
        try {
            log.warn("❌ Excluindo configuração ID: {}", id);
            configuracaoEmailService.deleteById(id);
            carregarSobDemanda();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", "Configuração excluída com sucesso!"));
        } catch (Exception ex) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro ao excluir",
                            "Erro ao excluir. Verifique dependências."));
            ex.printStackTrace();
        }
    }

    public void pesquisar() {
        carregarSobDemanda();
        mostrarFormulario = false;
    }

    public void limparFiltros() {
        log.info("🧹 Limpando filtros de pesquisa");
        campoSelecionado = null;
        condicaoSelecionada = null;
        valorPesquisa = null;
        carregarSobDemanda();
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Filtros limpos", "Todos os filtros foram removidos."));
    }

    // 🔧 Getters e Setters

    public ConfiguracaoEmail getConfiguracaoEmail() {
        return configuracaoEmail;
    }

    public void setConfiguracaoEmail(ConfiguracaoEmail configuracaoEmail) {
        this.configuracaoEmail = configuracaoEmail;
    }

    public CarregamentoLazyListForObject<ConfiguracaoEmail> getConfiguracoesLazy() {
        return configuracoesLazy;
    }

    public void setConfiguracoesLazy(CarregamentoLazyListForObject<ConfiguracaoEmail> configuracoesLazy) {
        this.configuracoesLazy = configuracoesLazy;
    }

    public String getCampoSelecionado() {
        return campoSelecionado;
    }

    public void setCampoSelecionado(String campoSelecionado) {
        this.campoSelecionado = campoSelecionado;
    }

    public String getCondicaoSelecionada() {
        return condicaoSelecionada;
    }

    public void setCondicaoSelecionada(String condicaoSelecionada) {
        this.condicaoSelecionada = condicaoSelecionada;
    }

    public String getValorPesquisa() {
        return valorPesquisa;
    }

    public void setValorPesquisa(String valorPesquisa) {
        this.valorPesquisa = valorPesquisa;
    }

    public List<SelectItem> getCamposPesquisa() {
        return camposPesquisa;
    }

    public void setCamposPesquisa(List<SelectItem> camposPesquisa) {
        this.camposPesquisa = camposPesquisa;
    }

    public boolean isMostrarFormulario() {
        return mostrarFormulario;
    }

    public void setMostrarFormulario(boolean mostrarFormulario) {
        this.mostrarFormulario = mostrarFormulario;
    }
}
