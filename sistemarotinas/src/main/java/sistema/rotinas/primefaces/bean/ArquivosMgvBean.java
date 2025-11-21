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
import sistema.rotinas.primefaces.model.ArquivosMgv;
import sistema.rotinas.primefaces.model.ArquivosMgvPattern;
import sistema.rotinas.primefaces.model.Loja;
import sistema.rotinas.primefaces.model.LojaRemoteConfig;
import sistema.rotinas.primefaces.service.interfaces.IArquivosMgvPatternService;
import sistema.rotinas.primefaces.service.interfaces.IArquivosMgvService;
import sistema.rotinas.primefaces.service.interfaces.ILojaRemoteConfigService;
import sistema.rotinas.primefaces.service.interfaces.ILojaService;

@Component
@Named
@SessionScoped
public class ArquivosMgvBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ArquivosMgvBean.class);

    private ArquivosMgv mgv;
    private List<ArquivosMgv> lista;
    private ArquivosMgvPattern novoPattern;
    private List<ArquivosMgvPattern> patternsDaLoja;

    // Controle de exibição do formulário (exigido pelo xhtml)
    private boolean mostrarFormulario;

    // Contrato do componente de pesquisa
    private String campoSelecionado;
    private String condicaoSelecionada; // "equal" | "contains"
    private String valorPesquisa;
    private List<SelectItem> camposPesquisa;

    @Autowired
    private IArquivosMgvService service;
    @Autowired
    private IArquivosMgvPatternService patternService;
    @Autowired
    private ILojaService lojaService;
    @Autowired
    private ILojaRemoteConfigService remoteCfgService;

    @PostConstruct
    public void init() {
        log.info("Inicializando ArquivosMgvBean");
        mgv = new ArquivosMgv();
        novoPattern = new ArquivosMgvPattern();
        mostrarFormulario = false;         // inicia oculto
        carregarCamposPesquisaPadrao();
        limparFiltros();                   // também carrega a lista
    }

    private void carregarCamposPesquisaPadrao() {
        camposPesquisa = new ArrayList<>();
        // Ajuste conforme os filtros que serão suportados no service
        camposPesquisa.add(new SelectItem("loja", "Loja"));
        camposPesquisa.add(new SelectItem("diretorio", "Diretório"));
        camposPesquisa.add(new SelectItem("nomeArquivo", "Nome do Arquivo"));
        camposPesquisa.add(new SelectItem("status", "Status"));
    }

    public void atualizarLista() {
        this.lista = service.findAll();
    }

    // Pesquisa (usada no fragmento de pesquisa)
    public void pesquisar() {
        log.info("Pesquisando MGV -> campo='{}', condicao='{}', valor='{}'",
                campoSelecionado, condicaoSelecionada, valorPesquisa);
        try {
            // TODO: implementar no service um pesquisar(campo, condicao, valor)
            // this.lista = service.pesquisar(campoSelecionado, condicaoSelecionada, valorPesquisa);
            this.lista = service.findAll(); // fallback para manter a tela operante
        } catch (Exception e) {
            log.error("Erro ao pesquisar MGV", e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro na pesquisa", e.getMessage()));
        }
    }

    public void limparFiltros() {
        this.campoSelecionado = null;
        this.condicaoSelecionada = null;
        this.valorPesquisa = null;
        atualizarLista();
        log.debug("Filtros de pesquisa limpos e lista recarregada.");
    }

    // Fluxo de cadastro
    public void prepararNovoCadastro() {
        this.mgv = new ArquivosMgv();
        this.novoPattern = new ArquivosMgvPattern();
        this.patternsDaLoja = null;
        this.mostrarFormulario = true; // abre o formulário
    }

    public void selecionarLoja(Loja loja) {
        this.mgv.setLoja(loja);
        LojaRemoteConfig cfg = remoteCfgService.findByLojaId(loja.getLojaId());
        if (cfg != null) {
            this.mgv.setRemoteConfig(cfg);
        }
        carregarPatterns();
    }

    public void carregarPatterns() {
        if (this.mgv != null && this.mgv.getMgvId() != null) {
            this.patternsDaLoja = patternService.listarPorMgv(this.mgv.getMgvId());
        }
    }

    public void salvar() {
        try {
            if (mgv.getMgvId() == null) {
                service.save(mgv);
            } else {
                service.update(mgv);
            }
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", "Configuração MGV salva."));
            atualizarLista();
            this.mostrarFormulario = false; // fecha após salvar
            // opcional: limpar o form para novo cadastro
            this.mgv = new ArquivosMgv();
            this.novoPattern = new ArquivosMgvPattern();
            this.patternsDaLoja = null;
        } catch (Exception e) {
            log.error("Erro ao salvar MGV", e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", e.getMessage()));
        }
    }

    public void editar(ArquivosMgv cfg) {
        this.mgv = cfg;
        carregarPatterns();
        this.mostrarFormulario = true; // abre para edição
    }

    public void excluir(Long id) {
        try {
            service.deleteById(id);
            atualizarLista();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", "Excluído com sucesso."));
        } catch (Exception e) {
            log.error("Erro ao excluir MGV", e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Não foi possível excluir."));
        }
    }

    // Patterns
    public void adicionarPattern() {
        try {
            if (this.mgv == null || this.mgv.getMgvId() == null) {
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_WARN, "Atenção", "Salve a configuração MGV antes de adicionar padrões."));
                return;
            }
            novoPattern.setMgv(this.mgv);
            patternService.save(novoPattern);
            this.novoPattern = new ArquivosMgvPattern();
            carregarPatterns();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", "Pattern adicionado."));
        } catch (Exception e) {
            log.error("Erro ao adicionar pattern", e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", e.getMessage()));
        }
    }

    public void removerPattern(Long patternId) {
        try {
            patternService.deleteById(patternId);
            carregarPatterns();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", "Pattern removido."));
        } catch (Exception e) {
            log.error("Erro ao remover pattern", e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Não foi possível remover o pattern."));
        }
    }

    // Getters/Setters
    public ArquivosMgv getMgv() {
        return mgv;
    }

    public void setMgv(ArquivosMgv mgv) {
        this.mgv = mgv;
    }

    public List<ArquivosMgv> getLista() {
        return lista;
    }

    public void setLista(List<ArquivosMgv> lista) {
        this.lista = lista;
    }

    public ArquivosMgvPattern getNovoPattern() {
        return novoPattern;
    }

    public void setNovoPattern(ArquivosMgvPattern novoPattern) {
        this.novoPattern = novoPattern;
    }

    public List<ArquivosMgvPattern> getPatternsDaLoja() {
        return patternsDaLoja;
    }

    public void setPatternsDaLoja(List<ArquivosMgvPattern> patternsDaLoja) {
        this.patternsDaLoja = patternsDaLoja;
    }

    public List<Loja> getLojas() {
        return lojaService.getAllLojas();
    }

    public boolean isMostrarFormulario() {
        return mostrarFormulario;
    }

    public void setMostrarFormulario(boolean mostrarFormulario) {
        this.mostrarFormulario = mostrarFormulario;
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
}
