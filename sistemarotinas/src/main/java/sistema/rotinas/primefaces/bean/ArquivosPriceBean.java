package sistema.rotinas.primefaces.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import sistema.rotinas.primefaces.model.ArquivosPrice;
import sistema.rotinas.primefaces.model.ArquivosPricePattern;
import sistema.rotinas.primefaces.model.Loja;
import sistema.rotinas.primefaces.model.LojaRemoteConfig;
import sistema.rotinas.primefaces.service.interfaces.IArquivosPricePatternService;
import sistema.rotinas.primefaces.service.interfaces.IArquivosPriceService;
import sistema.rotinas.primefaces.service.interfaces.ILojaRemoteConfigService;
import sistema.rotinas.primefaces.service.interfaces.ILojaService;

@Component
@Named
@SessionScoped
public class ArquivosPriceBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ArquivosPriceBean.class);

    private ArquivosPrice price;
    private List<ArquivosPrice> lista;

    private ArquivosPricePattern novoPattern;
    private List<ArquivosPricePattern> patternsDaLoja;

    private boolean mostrarFormulario;

    private String campoSelecionado;
    private String condicaoSelecionada;
    private String valorPesquisa;

    private List<Loja> lojas;

    // Mantém as senhas originais ao editar
    private String smbSenhaOriginal;
    private String destSenhaOriginal;

    // Controle de “modo edição” de senha (para máscara ******** + botão Alterar/Cancelar)
    private boolean editarSmbSenha;
    private boolean editarDestSenha;

    @Autowired
    private IArquivosPriceService service;

    @Autowired
    private IArquivosPricePatternService patternService;

    @Autowired
    private ILojaService lojaService;

    @Autowired
    private ILojaRemoteConfigService remoteCfgService;

    @PostConstruct
    public void init() {
        log.info("Inicializando ArquivosPriceBean");
        this.price = new ArquivosPrice();
        this.novoPattern = new ArquivosPricePattern();
        this.patternsDaLoja = new ArrayList<>();
        this.mostrarFormulario = false;

        this.smbSenhaOriginal = null;
        this.destSenhaOriginal = null;

        this.editarSmbSenha = false;
        this.editarDestSenha = false;

        carregarLojas();
        atualizarLista();
    }

    private void carregarLojas() {
        try {
            List<Loja> permitidas = lojaService.getLojasPermitidasDoUsuarioLogado();
            if (permitidas != null && !permitidas.isEmpty()) {
                this.lojas = new ArrayList<>(permitidas);
            } else {
                this.lojas = new ArrayList<>(lojaService.getAllLojas());
            }

            this.lojas.sort(
                Comparator.comparing((Loja l) -> safe(l.getCodLojaRms()), String.CASE_INSENSITIVE_ORDER)
                          .thenComparing(l -> safe(l.getNome()), String.CASE_INSENSITIVE_ORDER)
            );

        } catch (Exception e) {
            log.error("Erro ao carregar lojas", e);
            addMsg(FacesMessage.SEVERITY_ERROR, "Erro", "Falha ao carregar lojas.");
            this.lojas = new ArrayList<>();
        }
    }

    public void atualizarLista() {
        try {
            List<ArquivosPrice> all = service.findAll();
            all.sort(Comparator.comparing((ArquivosPrice ap) -> {
                if (ap.getLoja() == null) return "";
                return safe(ap.getLoja().getCodLojaRms());
            }, String.CASE_INSENSITIVE_ORDER));
            this.lista = all;
        } catch (Exception e) {
            log.error("Erro ao listar ArquivosPrice", e);
            addMsg(FacesMessage.SEVERITY_ERROR, "Erro", "Falha ao listar registros.");
            this.lista = new ArrayList<>();
        }
    }

    public void prepararNovoCadastro() {
        this.price = new ArquivosPrice();
        this.novoPattern = new ArquivosPricePattern();
        this.patternsDaLoja = new ArrayList<>();
        this.mostrarFormulario = true;

        this.smbSenhaOriginal = null;
        this.destSenhaOriginal = null;

        this.editarSmbSenha = false;
        this.editarDestSenha = false;
    }

    public void prepararEditar(ArquivosPrice cfg) {
        editar(cfg);
        this.mostrarFormulario = true;
    }

    public void editar(ArquivosPrice cfg) {
        this.price = cfg;

        // guarda senhas atuais (já chegam descriptografadas pelo AttributeConverter)
        this.smbSenhaOriginal = (cfg != null ? cfg.getSmbSenha() : null);
        this.destSenhaOriginal = (cfg != null ? cfg.getDestSenha() : null);

        // ao entrar em edição, começa “travado” (mostrando ********)
        this.editarSmbSenha = false;
        this.editarDestSenha = false;

        // ✅ importante: não deixar a senha "viva" no model durante a edição
        // (a tela exibirá máscara ******** e só abrirá p:password ao clicar em Alterar)
        if (this.price != null) {
            this.price.setSmbSenha(null);
            this.price.setDestSenha(null);
        }

        // limpa input de novo pattern
        this.novoPattern = new ArquivosPricePattern();

        carregarPatterns();
    }

    // ========= Controle de edição das senhas (usado no XHTML) =========

    public void habilitarEdicaoSmbSenha() {
        this.editarSmbSenha = true;
        // deixa o campo vazio para digitação de nova senha
        if (this.price != null) {
            this.price.setSmbSenha(null);
        }
    }

    public void cancelarEdicaoSmbSenha() {
        this.editarSmbSenha = false;
        if (this.price != null) {
            this.price.setSmbSenha(this.smbSenhaOriginal);
        }
    }

    public void habilitarEdicaoDestSenha() {
        this.editarDestSenha = true;
        // deixa o campo vazio para digitação de nova senha
        if (this.price != null) {
            this.price.setDestSenha(null);
        }
    }

    public void cancelarEdicaoDestSenha() {
        this.editarDestSenha = false;
        if (this.price != null) {
            this.price.setDestSenha(this.destSenhaOriginal);
        }
    }

    public void salvar() {
        try {
            if (price == null) {
                addMsg(FacesMessage.SEVERITY_ERROR, "Erro", "Objeto PRICE inválido.");
                this.mostrarFormulario = true;
                return;
            }

            if (price.getLoja() == null || price.getLoja().getLojaId() == null) {
                addMsg(FacesMessage.SEVERITY_WARN, "Validação", "Loja é obrigatória.");
                this.mostrarFormulario = true;
                return;
            }

            if (price.getRemoteConfig() == null || price.getRemoteConfig().getRemoteConfigId() == null) {
                LojaRemoteConfig efetiva = remoteCfgService.resolveEffectiveForLoja(price.getLoja().getLojaId());
                if (efetiva == null) {
                    addMsg(
                        FacesMessage.SEVERITY_WARN,
                        "Validação",
                        "Não existe Configuração Remota para esta loja (nem GLOBAL). Cadastre em Configurações → Configuração Remota (Loja)."
                    );
                    this.mostrarFormulario = true;
                    return;
                }
                price.setRemoteConfig(efetiva);
            }

            boolean novo = (price.getPriceId() == null);

            // ✅ Preserva senha original quando:
            // - usuário NÃO clicou em “Alterar”
            // - ou clicou, mas deixou em branco
            if (!novo) {
                if (!this.editarSmbSenha) {
                    price.setSmbSenha(this.smbSenhaOriginal);
                } else if (isBlank(price.getSmbSenha()) && !isBlank(this.smbSenhaOriginal)) {
                    price.setSmbSenha(this.smbSenhaOriginal);
                }

                if (!this.editarDestSenha) {
                    price.setDestSenha(this.destSenhaOriginal);
                } else if (isBlank(price.getDestSenha()) && !isBlank(this.destSenhaOriginal)) {
                    price.setDestSenha(this.destSenhaOriginal);
                }
            }

            // ✅ NOVO: valida e normaliza campos conforme tipo selecionado (sem apagar SMB/FS do outro modo)
            validarENormalizarPorTipoDestino(price);

            if (novo) {
                service.save(price);
            } else {
                service.update(price);
            }

            atualizarLista();

            addMsg(
                FacesMessage.SEVERITY_INFO,
                "Sucesso",
                novo ? "Configuração PRICE salva." : "Configuração PRICE atualizada."
            );

            // fecha e limpa o form (mantendo o comportamento atual)
            this.mostrarFormulario = false;
            this.price = new ArquivosPrice();
            this.novoPattern = new ArquivosPricePattern();
            this.patternsDaLoja = new ArrayList<>();

            this.smbSenhaOriginal = null;
            this.destSenhaOriginal = null;

            this.editarSmbSenha = false;
            this.editarDestSenha = false;

        } catch (IllegalArgumentException ex) {
            // ✅ NOVO: mensagens de validação amigáveis (sem stacktrace)
            addMsg(FacesMessage.SEVERITY_WARN, "Validação", ex.getMessage());
            this.mostrarFormulario = true;
        } catch (Exception e) {
            log.error("Erro ao salvar PRICE", e);
            addMsg(FacesMessage.SEVERITY_ERROR, "Erro", e.getMessage());
            this.mostrarFormulario = true;
        }
    }

    public void excluir(Long id) {
        try {
            List<ArquivosPricePattern> pats = patternService.listarPorPrice(id);
            int total = (pats == null) ? 0 : pats.size();

            if (pats != null) {
                for (ArquivosPricePattern p : pats) {
                    patternService.deleteById(p.getPatternId());
                }
            }

            service.deleteById(id);

            atualizarLista();

            if (this.price != null && Objects.equals(this.price.getPriceId(), id)) {
                this.price = new ArquivosPrice();
                this.novoPattern = new ArquivosPricePattern();
                this.patternsDaLoja = new ArrayList<>();
                this.mostrarFormulario = false;

                this.smbSenhaOriginal = null;
                this.destSenhaOriginal = null;

                this.editarSmbSenha = false;
                this.editarDestSenha = false;
            }

            addMsg(
                FacesMessage.SEVERITY_INFO,
                "Sucesso",
                total > 0
                    ? "Configuração excluída e " + total + " pattern(s) removido(s)."
                    : "Configuração excluída com sucesso."
            );

        } catch (Exception e) {
            log.error("Erro ao excluir PRICE id={}", id, e);
            addMsg(FacesMessage.SEVERITY_ERROR, "Erro", "Não foi possível excluir. " + e.getMessage());
        }
    }

    public void carregarPatterns() {
        if (this.price != null && this.price.getPriceId() != null) {
            this.patternsDaLoja = patternService.listarPorPrice(this.price.getPriceId());
        } else {
            this.patternsDaLoja = new ArrayList<>();
        }
    }

    public void adicionarPattern() {
        try {
            if (this.price == null || this.price.getPriceId() == null) {
                addMsg(FacesMessage.SEVERITY_WARN, "Atenção", "Salve a configuração PRICE antes de adicionar padrões.");
                return;
            }

            if (novoPattern == null) {
                novoPattern = new ArquivosPricePattern();
            }

            if (novoPattern.getPattern() == null || novoPattern.getPattern().trim().isEmpty()) {
                addMsg(FacesMessage.SEVERITY_WARN, "Validação", "Informe o Pattern.");
                return;
            }

            novoPattern.setPrice(this.price);
            patternService.save(novoPattern);

            this.novoPattern = new ArquivosPricePattern();
            carregarPatterns();

            addMsg(FacesMessage.SEVERITY_INFO, "Sucesso", "Pattern adicionado.");
        } catch (Exception e) {
            log.error("Erro ao adicionar pattern", e);
            addMsg(FacesMessage.SEVERITY_ERROR, "Erro", e.getMessage());
        }
    }

    public void removerPattern(Long patternId) {
        try {
            patternService.deleteById(patternId);
            carregarPatterns();
            addMsg(FacesMessage.SEVERITY_INFO, "Sucesso", "Pattern removido.");
        } catch (Exception e) {
            log.error("Erro ao remover pattern", e);
            addMsg(FacesMessage.SEVERITY_ERROR, "Erro", "Não foi possível remover o pattern.");
        }
    }

    public boolean isPricePersistido() {
        return this.price != null && this.price.getPriceId() != null;
    }

    public void pesquisar() {
        log.info("Pesquisa PRICE — Campo: {} | Condição: {} | Valor: {}", campoSelecionado, condicaoSelecionada, valorPesquisa);

        boolean campoVazio = campoSelecionado == null || campoSelecionado.isBlank();
        boolean condicaoVazia = condicaoSelecionada == null || condicaoSelecionada.isBlank();
        boolean valorVazio = valorPesquisa == null || valorPesquisa.trim().isBlank();

        if (campoVazio && condicaoVazia && valorVazio) {
            atualizarLista();
            mostrarFormulario = false;
            return;
        }

        if (campoVazio) {
            addMsg(FacesMessage.SEVERITY_WARN, "Atenção", "Selecione o campo para realizar a pesquisa.");
            return;
        }

        if (!valorVazio && condicaoVazia) {
            addMsg(FacesMessage.SEVERITY_WARN, "Atenção", "Selecione a condição para realizar a pesquisa.");
            return;
        }

        if (valorVazio && (!campoVazio && !condicaoVazia)) {
            addMsg(FacesMessage.SEVERITY_WARN, "Atenção", "Informe o valor para realizar a pesquisa.");
            return;
        }

        try {
            List<ArquivosPrice> base = service.findAll();
            String needle = valorPesquisa.trim().toLowerCase(Locale.ROOT);
            boolean equal = "equal".equalsIgnoreCase(condicaoSelecionada);

            List<ArquivosPrice> filtrada = new ArrayList<>();
            for (ArquivosPrice cfg : base) {
                String field = extrairCampoPesquisa(cfg, campoSelecionado);
                if (field == null) continue;

                String hay = field.toLowerCase(Locale.ROOT);
                boolean match = equal ? hay.equals(needle) : hay.contains(needle);
                if (match) filtrada.add(cfg);
            }

            filtrada.sort(Comparator.comparing((ArquivosPrice ap) -> {
                if (ap.getLoja() == null) return "";
                return safe(ap.getLoja().getCodLojaRms());
            }, String.CASE_INSENSITIVE_ORDER));

            this.lista = filtrada;
            this.mostrarFormulario = false;

        } catch (Exception e) {
            log.error("Erro ao pesquisar PRICE", e);
            addMsg(FacesMessage.SEVERITY_ERROR, "Erro", "Falha ao pesquisar: " + e.getMessage());
        }
    }

    private String extrairCampoPesquisa(ArquivosPrice cfg, String campo) {
        if (cfg == null || campo == null) return null;

        switch (campo) {
            case "loja.cod_loja_rms":
                return cfg.getLoja() != null ? cfg.getLoja().getCodLojaRms() : null;
            case "subpasta_remota":
                return cfg.getSubpastaRemota();
            case "tipo_destino":
                return cfg.getTipoDestino() != null ? cfg.getTipoDestino().name() : null;
            default:
                return null;
        }
    }

    public void limparFiltros() {
        this.campoSelecionado = null;
        this.condicaoSelecionada = null;
        this.valorPesquisa = null;
        atualizarLista();
        mostrarFormulario = false;

        addMsg(FacesMessage.SEVERITY_INFO, "Filtros limpos", "Todos os filtros foram removidos.");
    }

    private void addMsg(FacesMessage.Severity sev, String sum, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(sev, sum, detail));
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    // ✅ NOVO: valida/normaliza apenas o tipo selecionado, SEM apagar SMB/FS (para permitir fallback futuramente)
    private void validarENormalizarPorTipoDestino(ArquivosPrice p) {
        if (p == null || p.getTipoDestino() == null) {
            throw new IllegalArgumentException("Selecione o Tipo de Destino.");
        }

        switch (p.getTipoDestino()) {
            case FS:
                // FS montado (produção Linux): caminho é obrigatório
                p.setCaminhoFsDestino(trimToNull(p.getCaminhoFsDestino()));
                if (isBlank(p.getCaminhoFsDestino())) {
                    throw new IllegalArgumentException("Informe o Caminho FS destino.");
                }
                break;

            case SMB:
                // SMB: servidor/compartilhamento/usuário são o mínimo
                p.setSmbServidor(trimToNull(p.getSmbServidor()));
                p.setSmbCompartilhamento(trimToNull(p.getSmbCompartilhamento()));
                p.setSmbUsuario(trimToNull(p.getSmbUsuario()));
                p.setSmbDominio(trimToNull(p.getSmbDominio()));
                p.setSmbSubpasta(trimToNull(p.getSmbSubpasta()));

                if (isBlank(p.getSmbServidor())) {
                    throw new IllegalArgumentException("Para SMB, informe o Servidor.");
                }
                if (isBlank(p.getSmbCompartilhamento())) {
                    throw new IllegalArgumentException("Para SMB, informe o Compartilhamento.");
                }
                if (isBlank(p.getSmbUsuario())) {
                    throw new IllegalArgumentException("Para SMB, informe o Usuário.");
                }
                // senha pode ser obrigatória dependendo do ambiente; por enquanto não bloqueio
                break;

            case SFTP:
                // SFTP: host/usuário/dir são o mínimo; senha OU chave privada
                p.setDestHost(trimToNull(p.getDestHost()));
                p.setDestUsuario(trimToNull(p.getDestUsuario()));
                p.setDestDirRemoto(trimToNull(p.getDestDirRemoto()));
                p.setDestCaminhoChavePrivada(trimToNull(p.getDestCaminhoChavePrivada()));
                p.setDestKnownHostsPath(trimToNull(p.getDestKnownHostsPath()));

                if (p.getDestPort() == null) {
                    p.setDestPort(22);
                }

                if (isBlank(p.getDestHost())) {
                    throw new IllegalArgumentException("Para SFTP, informe o Host.");
                }
                if (isBlank(p.getDestUsuario())) {
                    throw new IllegalArgumentException("Para SFTP, informe o Usuário.");
                }
                if (isBlank(p.getDestDirRemoto())) {
                    throw new IllegalArgumentException("Para SFTP, informe o Dir Remoto.");
                }

                // exige autenticação mínima: senha OU chave
                boolean semSenha = isBlank(p.getDestSenha());
                boolean semChave = isBlank(p.getDestCaminhoChavePrivada());
                if (semSenha && semChave) {
                    throw new IllegalArgumentException("Para SFTP, informe a Senha ou o Caminho da Chave Privada.");
                }
                break;

            default:
                // nada
                break;
        }
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    public ArquivosPrice getPrice() {
        return price;
    }

    public void setPrice(ArquivosPrice price) {
        this.price = price;
    }

    public List<ArquivosPrice> getLista() {
        return lista;
    }

    public void setLista(List<ArquivosPrice> lista) {
        this.lista = lista;
    }

    public ArquivosPricePattern getNovoPattern() {
        return novoPattern;
    }

    public void setNovoPattern(ArquivosPricePattern novoPattern) {
        this.novoPattern = novoPattern;
    }

    public List<ArquivosPricePattern> getPatternsDaLoja() {
        return patternsDaLoja;
    }

    public void setPatternsDaLoja(List<ArquivosPricePattern> patternsDaLoja) {
        this.patternsDaLoja = patternsDaLoja;
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

    public List<Loja> getLojas() {
        return lojas;
    }

    public void setLojas(List<Loja> lojas) {
        this.lojas = lojas;
    }

    public String getSmbSenhaOriginal() {
        return smbSenhaOriginal;
    }

    public void setSmbSenhaOriginal(String smbSenhaOriginal) {
        this.smbSenhaOriginal = smbSenhaOriginal;
    }

    public String getDestSenhaOriginal() {
        return destSenhaOriginal;
    }

    public void setDestSenhaOriginal(String destSenhaOriginal) {
        this.destSenhaOriginal = destSenhaOriginal;
    }

    public boolean isEditarSmbSenha() {
        return editarSmbSenha;
    }

    public void setEditarSmbSenha(boolean editarSmbSenha) {
        this.editarSmbSenha = editarSmbSenha;
    }

    public boolean isEditarDestSenha() {
        return editarDestSenha;
    }

    public void setEditarDestSenha(boolean editarDestSenha) {
        this.editarDestSenha = editarDestSenha;
    }
}
