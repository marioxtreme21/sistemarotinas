package sistema.rotinas.primefaces.bean;

import java.io.Serializable;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

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
import sistema.rotinas.primefaces.model.Loja;
import sistema.rotinas.primefaces.model.LojaRemoteConfig;
import sistema.rotinas.primefaces.model.LojaRemoteConfig.Protocolo;
import sistema.rotinas.primefaces.service.interfaces.ILojaRemoteConfigService;
import sistema.rotinas.primefaces.service.interfaces.ILojaService;

@Component
@Named
@SessionScoped
public class LojaRemoteConfigBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(LojaRemoteConfigBean.class);

    @Autowired
    private ILojaRemoteConfigService lojaRemoteConfigService;
    @Autowired
    private ILojaService lojaService;

    private LojaRemoteConfig cfg;
    private List<LojaRemoteConfig> lista;
    private List<Loja> lojas;
    private Long selectedLojaId;
    private List<SelectItem> protocolos;
    private String senhaInput;
    private boolean globalFlag;
    private boolean mostrarFormulario = false; // ✅ default fechado

    private String campoPesquisa;
    private String condicaoSelecionada;
    private String valorPesquisa;

    @PostConstruct
    public void init() {
        log.info("Inicializando LojaRemoteConfigBean");
        this.cfg = new LojaRemoteConfig();
        this.globalFlag = false;
        this.cfg.setGlobal(false);
        this.senhaInput = "";
        carregarLojas();
        carregarProtocolos();
        atualizarLista();
        limparFiltros();

        // ✅ ao iniciar a sessão, já deixa fechado
        this.mostrarFormulario = false;
    }

    /**
     * ✅ Chamado no preRenderView da página.
     * Fecha o formulário ao ENTRAR na tela (GET). Em postback não mexe.
     */
    public void onPageOpen() {
        try {
            FacesContext fc = FacesContext.getCurrentInstance();
            if (fc != null && fc.isPostback()) {
                return; // não interfere em Novo/Editar/Salvar/Ajax
            }
            this.mostrarFormulario = false;
        } catch (Exception e) {
            // não quebra a renderização
            log.debug("onPageOpen ignorado: {}", e.getMessage());
        }
    }

    private void carregarLojas() {
        try {
            this.lojas = lojaService.getAllLojas();
            if (this.lojas != null) {
                this.lojas = this.lojas.stream()
                        .sorted((a, b) -> a.getNome().compareToIgnoreCase(b.getNome()))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Erro ao carregar lojas", e);
            addMsg(FacesMessage.SEVERITY_ERROR, "Erro", "Falha ao carregar lojas.");
        }
    }

    private void carregarProtocolos() {
        this.protocolos = Arrays.stream(Protocolo.values())
                .map(p -> new SelectItem(p, p.name()))
                .collect(Collectors.toList());
    }

    private void atualizarLista() {
        try {
            this.lista = lojaRemoteConfigService.findAll();
        } catch (Exception e) {
            log.error("Erro ao listar configurações remotas", e);
            addMsg(FacesMessage.SEVERITY_ERROR, "Erro", "Falha ao listar registros.");
        }
    }

    public void prepararNovo() {
        this.cfg = new LojaRemoteConfig();
        this.globalFlag = false;
        this.cfg.setGlobal(false);
        this.selectedLojaId = null;
        this.cfg.setLoja(null);
        this.senhaInput = "";
        this.mostrarFormulario = true; // ✅ abre
    }

    public void prepararNovoGlobal() {
        this.cfg = new LojaRemoteConfig();
        this.globalFlag = true;
        this.cfg.setGlobal(true);
        this.selectedLojaId = null;
        this.cfg.setLoja(null);
        this.senhaInput = "";
        this.mostrarFormulario = true; // ✅ abre
    }

    public void onSelecionarLoja() {
        try {
            if (selectedLojaId == null) {
                this.senhaInput = "";
                return;
            }
            LojaRemoteConfig existente = lojaRemoteConfigService.findByLojaId(selectedLojaId);
            if (existente != null) {
                this.cfg = existente;
                this.globalFlag = Boolean.TRUE.equals(existente.getGlobal());
                if (!this.globalFlag && existente.getLoja() != null) {
                    this.selectedLojaId = existente.getLoja().getLojaId();
                }
            } else {
                this.cfg = new LojaRemoteConfig();
                Loja loja = lojas.stream()
                        .filter(l -> l.getLojaId().equals(selectedLojaId))
                        .findFirst()
                        .orElse(null);
                this.cfg.setLoja(loja);
                this.globalFlag = false;
                this.cfg.setGlobal(false);
            }
            this.senhaInput = "";
            this.mostrarFormulario = true; // ✅ mantém aberto
        } catch (Exception e) {
            log.error("Erro ao selecionar loja", e);
            addMsg(FacesMessage.SEVERITY_ERROR, "Erro", "Falha ao carregar a configuração da loja.");
        }
    }

    public void editar(LojaRemoteConfig c) {
        this.cfg = c;
        this.globalFlag = Boolean.TRUE.equals(c.getGlobal());
        this.selectedLojaId = (c.getLoja() == null ? null : c.getLoja().getLojaId());
        this.senhaInput = "";
        this.mostrarFormulario = true; // ✅ abre
    }

    public void prepararEditar(LojaRemoteConfig c) {
        editar(c);
    }

    public void excluir(Long id) {
        try {
            lojaRemoteConfigService.deleteById(id);
            atualizarLista();
            addMsg(FacesMessage.SEVERITY_INFO, "Sucesso", "Excluído com sucesso.");
        } catch (Exception e) {
            log.error("Erro ao excluir configuração remota", e);
            addMsg(FacesMessage.SEVERITY_ERROR, "Erro", "Não foi possível excluir.");
        }
    }

    public void salvar() {
        try {
            cfg.setGlobal(globalFlag);

            if (Boolean.TRUE.equals(cfg.getGlobal())) {
                cfg.setLoja(null);
                this.selectedLojaId = null;
            } else {
                if (selectedLojaId == null) {
                    addMsg(FacesMessage.SEVERITY_WARN, "Validação",
                            "Loja é obrigatória quando a configuração não for global.");
                    return;
                }
                Loja loja = lojas.stream()
                        .filter(l -> l.getLojaId().equals(selectedLojaId))
                        .findFirst()
                        .orElse(null);
                if (loja == null) {
                    addMsg(FacesMessage.SEVERITY_WARN, "Validação", "Loja inválida.");
                    return;
                }
                cfg.setLoja(loja);
            }

            if (senhaInput != null && !senhaInput.isBlank()) {
                cfg.setSenhaRemota(senhaInput);
            }

            boolean novo = (cfg.getRemoteConfigId() == null);
            LojaRemoteConfig salvo = (novo ? lojaRemoteConfigService.save(cfg) : lojaRemoteConfigService.update(cfg));

            this.cfg = salvo;
            this.globalFlag = Boolean.TRUE.equals(salvo.getGlobal());
            this.selectedLojaId = (salvo.getLoja() != null ? salvo.getLoja().getLojaId() : null);

            this.senhaInput = "";
            atualizarLista();

            addMsg(FacesMessage.SEVERITY_INFO, "Sucesso", "Configuração salva.");

            // aqui você pode escolher:
            // - fechar após salvar:
            this.mostrarFormulario = false;

            // - ou manter o seu padrão de prepararNovo() (abre). Se preferir, comente a linha acima e use:
            // prepararNovo();

        } catch (Exception e) {
            log.error("Erro ao salvar configuração remota", e);
            addMsg(FacesMessage.SEVERITY_ERROR, "Erro", "Falha ao salvar: " + e.getMessage());
        }
    }

    public void testarConexao() {
        try {
            LojaRemoteConfig paraTeste = cloneParaTeste(cfg, senhaInput);
            int timeoutMs = (paraTeste.getConnectTimeoutMs() != null && paraTeste.getConnectTimeoutMs() > 0)
                    ? paraTeste.getConnectTimeoutMs()
                    : 20000;

            String resultado = lojaRemoteConfigService.testConnection(paraTeste, Duration.ofMillis(timeoutMs));
            showResultadoTeste(resultado);
        } catch (Exception e) {
            log.error("Erro ao testar conexão (toolbar)", e);
            addMsg(FacesMessage.SEVERITY_ERROR, "Falha de conexão", e.getMessage());
        }
    }

    public void testarConexao(LojaRemoteConfig salvo) {
        try {
            int timeoutMs = (salvo.getConnectTimeoutMs() != null && salvo.getConnectTimeoutMs() > 0)
                    ? salvo.getConnectTimeoutMs()
                    : 20000;
            String resultado = lojaRemoteConfigService.testConnection(salvo, Duration.ofMillis(timeoutMs));
            showResultadoTeste(resultado);
        } catch (Exception e) {
            log.error("Erro ao testar conexão (linha)", e);
            addMsg(FacesMessage.SEVERITY_ERROR, "Falha de conexão", e.getMessage());
        }
    }

    private LojaRemoteConfig cloneParaTeste(LojaRemoteConfig base, String senhaDigitada) {
        LojaRemoteConfig x = new LojaRemoteConfig();
        x.setRemoteConfigId(base.getRemoteConfigId());
        x.setLoja(base.getLoja());
        x.setGlobal(base.getGlobal());

        x.setProtocolo(base.getProtocolo());
        x.setHostRemoto(base.getHostRemoto());
        x.setPortaRemota(base.getPortaRemota());
        x.setUsuarioRemoto(base.getUsuarioRemoto());

        String efetiva = (senhaDigitada != null && !senhaDigitada.isBlank()) ? senhaDigitada : base.getSenhaRemota();
        if (efetiva != null && !efetiva.isBlank()) {
            x.setSenhaRemota(efetiva);
        }

        x.setCaminhoChavePrivada(base.getCaminhoChavePrivada());
        x.setBaseDirRemoto(base.getBaseDirRemoto());
        x.setFtpPassiveMode(Boolean.TRUE.equals(base.getFtpPassiveMode()));
        x.setFtpsTlsExplicit(Boolean.TRUE.equals(base.getFtpsTlsExplicit()));
        x.setValidarCertificado(Boolean.TRUE.equals(base.getValidarCertificado()));
        x.setConnectTimeoutMs(base.getConnectTimeoutMs());
        x.setReadTimeoutMs(base.getReadTimeoutMs());
        x.setRetries(base.getRetries());
        return x;
    }

    private void showResultadoTeste(String resultado) {
        if (resultado == null) {
            addMsg(FacesMessage.SEVERITY_WARN, "Aviso", "Sem retorno do teste.");
            return;
        }
        String r = resultado.trim().replace("\n", " | ");
        if (r.toUpperCase(Locale.ROOT).startsWith("OK")) {
            addMsg(FacesMessage.SEVERITY_INFO, "Conexão OK", r);
        } else {
            addMsg(FacesMessage.SEVERITY_WARN, "Falha de conexão", r);
        }
    }

    private void addMsg(FacesMessage.Severity sev, String sum, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(sev, sum, detail));
    }

    public void pesquisar() {
        try {
            atualizarLista();
            if (isBlank(campoPesquisa) || isBlank(condicaoSelecionada) || isBlank(valorPesquisa)) {
                return;
            }
            final String valor = valorPesquisa.trim();
            final boolean contains = "contains".equalsIgnoreCase(condicaoSelecionada);
            final boolean equal = "equal".equalsIgnoreCase(condicaoSelecionada);

            this.lista = this.lista.stream().filter(x -> {
                String alvo;
                switch (String.valueOf(campoPesquisa)) {
                case "loja.cod_loja_rms":
                    alvo = (x.getLoja() != null ? String.valueOf(x.getLoja().getCodLojaRms()) : "");
                    break;
                case "host_remoto":
                    alvo = nullSafe(x.getHostRemoto());
                    break;
                case "protocolo":
                    alvo = (x.getProtocolo() != null ? x.getProtocolo().name() : "");
                    break;
                default:
                    String a = (x.getLoja() != null ? String.valueOf(x.getLoja().getCodLojaRms()) : "");
                    String b = nullSafe(x.getHostRemoto());
                    String c = (x.getProtocolo() != null ? x.getProtocolo().name() : "");
                    String blob = (a + " " + b + " " + c).toLowerCase(Locale.ROOT);
                    return blob.contains(valor.toLowerCase(Locale.ROOT));
                }

                if (contains) {
                    return alvo.toLowerCase(Locale.ROOT).contains(valor.toLowerCase(Locale.ROOT));
                } else if (equal) {
                    return Objects.equals(alvo, valor);
                }
                return true;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Erro ao pesquisar", e);
            addMsg(FacesMessage.SEVERITY_ERROR, "Erro", "Falha na pesquisa: " + e.getMessage());
        }
    }

    public void limparFiltros() {
        this.campoPesquisa = null;
        this.condicaoSelecionada = null;
        this.valorPesquisa = null;
        atualizarLista();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    public LojaRemoteConfig getCfg() { return cfg; }
    public void setCfg(LojaRemoteConfig cfg) { this.cfg = cfg; }

    public List<LojaRemoteConfig> getLista() { return lista; }
    public void setLista(List<LojaRemoteConfig> lista) { this.lista = lista; }

    public List<Loja> getLojas() { return lojas; }
    public void setLojas(List<Loja> lojas) { this.lojas = lojas; }

    public Long getSelectedLojaId() { return selectedLojaId; }
    public void setSelectedLojaId(Long selectedLojaId) { this.selectedLojaId = selectedLojaId; }

    public List<SelectItem> getProtocolos() { return protocolos; }
    public void setProtocolos(List<SelectItem> protocolos) { this.protocolos = protocolos; }

    public String getSenhaInput() { return senhaInput; }
    public void setSenhaInput(String senhaInput) { this.senhaInput = senhaInput; }

    public boolean isGlobalFlag() { return globalFlag; }
    public void setGlobalFlag(boolean globalFlag) {
        this.globalFlag = globalFlag;
        if (this.cfg == null) this.cfg = new LojaRemoteConfig();
        this.cfg.setGlobal(globalFlag);
        if (globalFlag) {
            this.selectedLojaId = null;
            this.cfg.setLoja(null);
        }
    }

    public boolean isMostrarFormulario() { return mostrarFormulario; }
    public void setMostrarFormulario(boolean mostrarFormulario) { this.mostrarFormulario = mostrarFormulario; }

    public void exibirFormulario() { this.mostrarFormulario = true; }
    public void ocultarFormulario() { this.mostrarFormulario = false; }

    public String getCampoPesquisa() { return campoPesquisa; }
    public void setCampoPesquisa(String campoPesquisa) { this.campoPesquisa = campoPesquisa; }

    public String getCondicaoSelecionada() { return condicaoSelecionada; }
    public void setCondicaoSelecionada(String condicaoSelecionada) { this.condicaoSelecionada = condicaoSelecionada; }

    public String getValorPesquisa() { return valorPesquisa; }
    public void setValorPesquisa(String valorPesquisa) { this.valorPesquisa = valorPesquisa; }

    @Deprecated
    public Long getCampoSelecionado() { return this.selectedLojaId; }

    @Deprecated
    public void setCampoSelecionado(Long id) { this.selectedLojaId = id; }
}
