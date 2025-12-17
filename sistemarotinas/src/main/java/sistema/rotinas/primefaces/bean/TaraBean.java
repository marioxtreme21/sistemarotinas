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
import sistema.rotinas.primefaces.model.Tara;
import sistema.rotinas.primefaces.service.interfaces.ITaraService;

@Component
@Named
@SessionScoped
public class TaraBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(TaraBean.class);

    private Tara tara;
    private CarregamentoLazyListForObject<Tara> tarasLazy;

    // Campos de pesquisa
    private String campoSelecionado;
    private String condicaoSelecionada; // "equal" | "contains"
    private String valorPesquisa;
    private List<SelectItem> camposPesquisa;
    private boolean mostrarFormulario;

    @Autowired
    private ITaraService taraService;

    @PostConstruct
    public void init() {
        log.info("Inicializando TaraBean");
        mostrarFormulario = false;
        tara = new Tara();
        carregarTarasSobDemanda();

        camposPesquisa = new ArrayList<>();
        // Campos conforme tabela cad_pso_emb
        camposPesquisa.add(new SelectItem("prd", "Código Produto (PRD)"));
        camposPesquisa.add(new SelectItem("pso_emb", "Peso Embalagem (pso_emb)"));
        camposPesquisa.add(new SelectItem("sec", "Seção (sec)"));
        camposPesquisa.add(new SelectItem("grp", "Grupo (grp)"));
        camposPesquisa.add(new SelectItem("sgr", "Subgrupo (sgr)"));
        camposPesquisa.add(new SelectItem("dat_atz", "Data Atualização (dat_atz)"));
    }

    public void carregarTarasSobDemanda() {
        log.debug("Iniciando carregamento sob demanda de taras...");
        tarasLazy = new CarregamentoLazyListForObject<>((first, pageSize) -> {
            List<Tara> taras = taraService.findAllTaras(first, pageSize, "prd", true);
            log.debug("Taras carregadas: {}", taras.size());
            return taras;
        }, () -> {
            int total = taraService.countTaras();
            log.debug("Total de taras: {}", total);
            return total;
        });
    }

    public void prepararNovoCadastro() {
        log.info("Preparando novo cadastro de tara");
        this.tara = new Tara();
        this.mostrarFormulario = true;
    }

    public void salvar() {
        try {
            log.info("Salvando tara: {}", tara);
            taraService.save(tara);
            carregarTarasSobDemanda();
            tara = new Tara();
            mostrarFormulario = false;

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Sucesso",
                            "Registro de tara salvo com sucesso!"));
        } catch (IllegalArgumentException ex) {
            log.error("Erro ao salvar tara: {}", ex.getMessage());
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Erro",
                            ex.getMessage()));
            mostrarFormulario = true;
        } catch (Exception ex) {
            log.error("Erro inesperado ao salvar tara.", ex);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Erro",
                            "Ocorreu um erro ao salvar. Verifique os dados e tente novamente."));
            mostrarFormulario = true;
        }
    }

    /**
     * Usado pelo botão "Editar" do dataTable (pode estar chamando prepararEditar).
     */
    public void prepararEditar(Tara tara) {
        if (tara == null) {
            log.warn("Tentativa de preparar edição com tara nula.");
            return;
        }

        log.info("Preparando edição da tara: {}", tara);
        // Reaproveita a lógica do método editar
        editar(tara);
        this.mostrarFormulario = true;
    }

    /**
     * Mantido para uso interno ou se a página chamar diretamente editar(tara).
     */
    public void editar(Tara tara) {
        if (tara == null) {
            log.warn("Tentativa de edição com tara nula.");
            return;
        }

        log.info("Editando tara: {}", tara);
        this.tara = tara;
        this.mostrarFormulario = true;
    }

    public void excluir(Tara taraSelecionada) {
        if (taraSelecionada == null) {
            log.warn("Tentativa de exclusão com tara nula.");
            return;
        }

        try {
            log.warn("Excluindo tara: {}", taraSelecionada);
            taraService.deleteById(taraSelecionada.getPrd());
            carregarTarasSobDemanda();

            // Limpa o formulário após exclusão
            this.tara = new Tara();
            this.mostrarFormulario = false;

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Sucesso",
                            "Registro de tara excluído com sucesso!"));
        } catch (Exception e) {
            log.error("Erro ao excluir tara.", e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Erro",
                            "Ocorreu um erro ao excluir a tara. Tente novamente."));
        }
    }

    public void pesquisar() {
        log.info("Iniciando pesquisa de Tara...");

        boolean campoVazio = campoSelecionado == null || campoSelecionado.isEmpty();
        boolean condicaoVazia = condicaoSelecionada == null || condicaoSelecionada.isEmpty();
        boolean valorVazio = valorPesquisa == null || valorPesquisa.trim().isEmpty();

        if (campoVazio && condicaoVazia && valorVazio) {
            log.info("Pesquisa vazia — carregando todos os registros.");
            carregarTarasSobDemanda();
            mostrarFormulario = false;
            return;
        }

        if (campoVazio) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN,
                            "Atenção",
                            "Selecione o campo para realizar a pesquisa."));
            return;
        }

        if (!valorVazio && condicaoVazia) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN,
                            "Atenção",
                            "Selecione a condição para realizar a pesquisa."));
            return;
        }

        if (valorVazio && (!campoVazio && !condicaoVazia)) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN,
                            "Atenção",
                            "Informe o valor para realizar a pesquisa."));
            return;
        }

        log.info(String.format("Executando pesquisa — Campo: %s | Condição: %s | Valor: %s",
                campoSelecionado, condicaoSelecionada, valorPesquisa));

        tarasLazy = new CarregamentoLazyListForObject<>(
                (first, pageSize) -> taraService.findTarasByCriteria(
                        campoSelecionado,
                        condicaoSelecionada,
                        valorPesquisa,
                        first,
                        pageSize,
                        "prd",
                        true),
                () -> taraService.countTarasByCriteria(
                        campoSelecionado,
                        condicaoSelecionada,
                        valorPesquisa));

        mostrarFormulario = false;
    }

    public void limparFiltros() {
        campoSelecionado = null;
        condicaoSelecionada = null;
        valorPesquisa = null;
        carregarTarasSobDemanda();
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                        "Filtros limpos",
                        "Todos os filtros foram removidos."));
    }

    public void sincronizarComServidor144() {
        log.info("Solicitada sincronização manual da cad_pso_emb com servidor 144.");
        FacesContext context = FacesContext.getCurrentInstance();
        try {
            taraService.sincronizarComServidor144();
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Sincronização concluída",
                    "Tabela cad_pso_emb sincronizada com o servidor 144."));
        } catch (Exception e) {
            log.error("Erro ao sincronizar com servidor 144.", e);
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erro na sincronização",
                    "Ocorreu um erro ao sincronizar com o servidor 144. Verifique os logs."));
        }
    }

    // Getters e Setters

    public Tara getTara() {
        return tara;
    }

    public void setTara(Tara tara) {
        this.tara = tara;
    }

    public CarregamentoLazyListForObject<Tara> getTarasLazy() {
        return tarasLazy;
    }

    public void setTarasLazy(CarregamentoLazyListForObject<Tara> tarasLazy) {
        this.tarasLazy = tarasLazy;
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
