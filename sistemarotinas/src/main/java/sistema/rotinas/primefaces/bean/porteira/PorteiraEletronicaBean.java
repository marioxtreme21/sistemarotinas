package sistema.rotinas.primefaces.bean.porteira;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import jakarta.inject.Named;

import sistema.rotinas.primefaces.lazy.CarregamentoLazyListForObject;
import sistema.rotinas.primefaces.model.Loja;
import sistema.rotinas.primefaces.model.porteira.PorteiraEletronica;
import sistema.rotinas.primefaces.service.interfaces.ILojaService;
import sistema.rotinas.primefaces.service.interfaces.porteira.IPorteiraEletronicaService;

@Component
@Named
@SessionScoped
public class PorteiraEletronicaBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private PorteiraEletronica porteira;
    private CarregamentoLazyListForObject<PorteiraEletronica> porteirasLazy;

    private List<Loja> lojas;

    // pesquisa
    private String campoSelecionado;
    private String condicaoSelecionada;
    private String valorPesquisa;
    private List<SelectItem> camposPesquisa;

    private boolean mostrarFormulario;

    // =======================
    // senha (mascarar/editar)
    // =======================
    private String senhaOculta;      // valor editável no input
    private boolean editandoSenha;   // controla se mostra input ou mask
    private String senhaOriginal;    // backup para restaurar ao cancelar
    private boolean porteiraPersistida; // usado no XHTML (rendered)

    // =======================
    // datas/horas (PrimeFaces usa Date)
    // =======================
    private Date dataInicio;
    private Date dataFim;
    private Date horaInicio;
    private Date horaFim;

    @Autowired
    private IPorteiraEletronicaService porteiraService;

    @Autowired
    private ILojaService lojaService;

    @PostConstruct
    public void init() {
        mostrarFormulario = false;
        porteira = new PorteiraEletronica();

        senhaOculta = "";
        senhaOriginal = null;
        editandoSenha = false;
        porteiraPersistida = false;

        lojas = lojaService.getAllLojas();

        camposPesquisa = new ArrayList<>();
        camposPesquisa.add(new SelectItem("descricao", "Descrição"));
        camposPesquisa.add(new SelectItem("ip", "IP"));
        camposPesquisa.add(new SelectItem("loja.nome", "Loja"));

        carregarPorteirasSobDemanda();
    }

    public void carregarPorteirasSobDemanda() {
        porteirasLazy = new CarregamentoLazyListForObject<>(
                (first, pageSize) -> porteiraService.findAllPorteiras(first, pageSize, null, true),
                () -> porteiraService.countPorteiras());
    }

    public void prepararNovoCadastro() {
        this.porteira = new PorteiraEletronica();

        // modo cadastro novo
        this.porteiraPersistida = false;

        // senha: em novo cadastro já deixa digitar
        this.editandoSenha = true;
        this.senhaOculta = "";
        this.senhaOriginal = null;

        this.dataInicio = null;
        this.dataFim = null;
        this.horaInicio = null;
        this.horaFim = null;

        this.mostrarFormulario = true;
    }

    public void prepararEditar(PorteiraEletronica p) {
        this.porteira = p;

        this.dataInicio = convertLocalDateToDate(p.getDataInicio());
        this.dataFim = convertLocalDateToDate(p.getDataFim());
        this.horaInicio = convertLocalTimeToDate(p.getHoraInicio());
        this.horaFim = convertLocalTimeToDate(p.getHoraFim());

        // marca como persistida (para rendered)
        this.porteiraPersistida = (p != null && p.getId() != null);

        // traz a senha do banco (não confia no objeto por segurança)
        String senhaReal = porteiraService.buscarSenhaPelaId(p.getId());
        this.senhaOriginal = (senhaReal != null) ? senhaReal : "";
        this.senhaOculta = this.senhaOriginal;

        // inicia mascarado (não editando) até clicar
        this.editandoSenha = false;

        this.mostrarFormulario = true;
    }

    public void iniciarEdicaoSenha() {
        this.editandoSenha = true;
        if (this.senhaOculta == null) {
            this.senhaOculta = "";
        }
        // garante backup caso entre por algum caminho não esperado
        if (this.senhaOriginal == null) {
            this.senhaOriginal = this.senhaOculta;
        }
    }

    public void cancelarEdicaoSenha() {
        this.editandoSenha = false;

        // volta o valor anterior
        if (this.senhaOriginal == null) {
            this.senhaOriginal = "";
        }
        this.senhaOculta = this.senhaOriginal;

        // também restaura na entidade em memória (para não correr risco ao salvar)
        if (this.porteira != null) {
            this.porteira.setSenhaIntegracao(this.senhaOriginal);
        }
    }

    public void salvar() {
        try {
            // joga Date -> LocalDate/LocalTime na entidade
            porteira.setDataInicio(convertDateToLocalDate(dataInicio));
            porteira.setDataFim(convertDateToLocalDate(dataFim));
            porteira.setHoraInicio(convertDateToLocalTime(horaInicio));
            porteira.setHoraFim(convertDateToLocalTime(horaFim));

            // ==========================
            // SENHA: regra correta
            // ==========================
            if (porteira.getId() != null) {
                // edição de registro existente
                if (editandoSenha) {
                    // usuário alterou (ou decidiu manter mas está em modo edição)
                    porteira.setSenhaIntegracao(senhaOculta);
                } else {
                    // não alterou -> mantém original do banco
                    String original = porteiraService.buscarSenhaPelaId(porteira.getId());
                    porteira.setSenhaIntegracao(original);
                }
            } else {
                // novo cadastro -> usa o que digitou
                porteira.setSenhaIntegracao(senhaOculta);
            }

            porteiraService.save(porteira);

            carregarPorteirasSobDemanda();
            lojas = lojaService.getAllLojas();

            // após salvar
            this.porteiraPersistida = false;
            this.editandoSenha = false;
            this.senhaOculta = "";
            this.senhaOriginal = null;

            this.porteira = new PorteiraEletronica();
            this.mostrarFormulario = false;

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", "Porteira salva com sucesso!"));

        } catch (IllegalArgumentException ex) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Validação", ex.getMessage()));
            mostrarFormulario = true;
        } catch (Exception ex) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao salvar a porteira."));
            mostrarFormulario = true;
        }
    }

    public void excluir(Long id) {
        try {
            porteiraService.deleteById(id);
            carregarPorteirasSobDemanda();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", "Porteira excluída com sucesso!"));
        } catch (Exception ex) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao excluir."));
        }
    }

    public void pesquisar() {
        boolean campoVazio = campoSelecionado == null || campoSelecionado.isEmpty();
        boolean condicaoVazia = condicaoSelecionada == null || condicaoSelecionada.isEmpty();
        boolean valorVazio = valorPesquisa == null || valorPesquisa.trim().isEmpty();

        if (campoVazio && condicaoVazia && valorVazio) {
            carregarPorteirasSobDemanda();
            mostrarFormulario = false;
            return;
        }

        if (campoVazio) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Atenção", "Selecione o campo."));
            return;
        }
        if (!valorVazio && condicaoVazia) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Atenção", "Selecione a condição."));
            return;
        }
        if (valorVazio && (!campoVazio && !condicaoVazia)) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Atenção", "Informe o valor."));
            return;
        }

        porteirasLazy = new CarregamentoLazyListForObject<>(
                (first, pageSize) -> porteiraService.findPorteirasByCriteria(
                        campoSelecionado, condicaoSelecionada, valorPesquisa, first, pageSize, null, true),
                () -> porteiraService.countPorteirasByCriteria(
                        campoSelecionado, condicaoSelecionada, valorPesquisa));

        mostrarFormulario = false;
    }

    public void limparFiltros() {
        campoSelecionado = null;
        condicaoSelecionada = null;
        valorPesquisa = null;
        carregarPorteirasSobDemanda();
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "OK", "Filtros limpos."));
    }

    // helpers para tabela
    public Date getDataInicio(PorteiraEletronica p) {
        return convertLocalDateToDate(p.getDataInicio());
    }

    public Date getDataFim(PorteiraEletronica p) {
        return convertLocalDateToDate(p.getDataFim());
    }

    public Date getHoraInicio(PorteiraEletronica p) {
        return convertLocalTimeToDate(p.getHoraInicio());
    }

    public Date getHoraFim(PorteiraEletronica p) {
        return convertLocalTimeToDate(p.getHoraFim());
    }

    // =======================
    // conversões
    // =======================
    private LocalDate convertDateToLocalDate(Date date) {
        return date == null
                ? null
                : Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * Ajuste: pega somente HH:mm:ss (ignora data) e "limpa" nanos.
     */
    private LocalTime convertDateToLocalTime(Date date) {
        if (date == null) return null;
        LocalTime t = Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalTime();
        return LocalTime.of(t.getHour(), t.getMinute(), t.getSecond());
    }

    private Date convertLocalDateToDate(LocalDate localDate) {
        return localDate == null
                ? null
                : Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private Date convertLocalTimeToDate(LocalTime localTime) {
        return localTime == null
                ? null
                : Date.from(localTime.atDate(LocalDate.of(1970, 1, 1))
                        .atZone(ZoneId.systemDefault()).toInstant());
    }

    // =======================
    // Getters/Setters
    // =======================
    public PorteiraEletronica getPorteira() {
        return porteira;
    }

    public void setPorteira(PorteiraEletronica porteira) {
        this.porteira = porteira;
    }

    public CarregamentoLazyListForObject<PorteiraEletronica> getPorteirasLazy() {
        return porteirasLazy;
    }

    public void setPorteirasLazy(CarregamentoLazyListForObject<PorteiraEletronica> porteirasLazy) {
        this.porteirasLazy = porteirasLazy;
    }

    public List<Loja> getLojas() {
        return lojas;
    }

    public void setLojas(List<Loja> lojas) {
        this.lojas = lojas;
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

    public String getSenhaOculta() {
        return senhaOculta;
    }

    public void setSenhaOculta(String senhaOculta) {
        this.senhaOculta = senhaOculta;
    }

    public boolean isEditandoSenha() {
        return editandoSenha;
    }

    public void setEditandoSenha(boolean editandoSenha) {
        this.editandoSenha = editandoSenha;
    }

    public boolean isPorteiraPersistida() {
        return porteiraPersistida;
    }

    public void setPorteiraPersistida(boolean porteiraPersistida) {
        this.porteiraPersistida = porteiraPersistida;
    }

    public Date getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(Date dataInicio) {
        this.dataInicio = dataInicio;
    }

    public Date getDataFim() {
        return dataFim;
    }

    public void setDataFim(Date dataFim) {
        this.dataFim = dataFim;
    }

    public Date getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(Date horaInicio) {
        this.horaInicio = horaInicio;
    }

    public Date getHoraFim() {
        return horaFim;
    }

    public void setHoraFim(Date horaFim) {
        this.horaFim = horaFim;
    }
}