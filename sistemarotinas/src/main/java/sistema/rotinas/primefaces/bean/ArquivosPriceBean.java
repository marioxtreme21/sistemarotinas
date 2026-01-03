package sistema.rotinas.primefaces.bean;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
import sistema.rotinas.primefaces.service.PriceTransferService;
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

    // serviço responsável por executar o teste (download SFTP + cópia FS/SMB)
    @Autowired
    private PriceTransferService priceTransferService;

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

        // importante: não deixar a senha "viva" no model durante a edição
        if (this.price != null) {
            this.price.setSmbSenha(null);
            this.price.setDestSenha(null);
        }

        this.novoPattern = new ArquivosPricePattern();
        carregarPatterns();
    }

    // ========= Controle de edição das senhas =========

    public void habilitarEdicaoSmbSenha() {
        this.editarSmbSenha = true;
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

            // Preserva senha original quando:
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

            // valida e normaliza campos conforme tipo selecionado (sem apagar SMB/FS do outro modo)
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

    // ================================
    // ✅ Ação do botão "Testar" (com logs detalhados)
    // ================================

    public void testar(ArquivosPrice cfg) {
        Long id = (cfg != null ? cfg.getPriceId() : null);
        String lojaCod = (cfg != null && cfg.getLoja() != null ? cfg.getLoja().getCodLojaRms() : null);

        long t0 = System.nanoTime();
        log.info("[TESTE-PRICE] Início - priceId={} loja={}", id, lojaCod);

        try {
            if (cfg == null || cfg.getPriceId() == null) {
                log.warn("[TESTE-PRICE] Abortado: cfg nulo ou sem priceId");
                addMsg(FacesMessage.SEVERITY_WARN, "Teste", "Salve a configuração antes de testar.");
                return;
            }

            log.info("[TESTE-PRICE] Preparando chamada ao PriceTransferService - priceId={}", id);

            Object result = invocarTesteNoService(cfg);

            long ms = (System.nanoTime() - t0) / 1_000_000;
            log.info("[TESTE-PRICE] Retorno do service em {} ms - priceId={}", ms, id);

            Boolean downloadOk = getBoolean(result, "isDownloadOk", "getDownloadOk");
            Boolean fsOk = getBoolean(result, "isFsOk", "getFsOk");
            Boolean smbOk = getBoolean(result, "isSmbOk", "getSmbOk");
            Object arqRemoto = getObject(result, "getArquivoRemoto", "getNomeArquivoRemoto", "getRemoteFile");
            Object arqLocal = getObject(result, "getArquivoLocal", "getLocalFile", "getPathLocal");

            log.info("[TESTE-PRICE] Resumo - priceId={} downloadOk={} fsOk={} smbOk={} arquivoRemoto={} arquivoLocal={}",
                    id, downloadOk, fsOk, smbOk, arqRemoto, arqLocal);

            publicarMensagensDoResultado(result);

        } catch (IllegalArgumentException ex) {
            log.warn("[TESTE-PRICE] Validação - priceId={} msg={}", id, ex.getMessage());
            addMsg(FacesMessage.SEVERITY_WARN, "Validação", ex.getMessage());
        } catch (Exception e) {
            log.error("[TESTE-PRICE] Erro - priceId={}", id, e);
            addMsg(FacesMessage.SEVERITY_ERROR, "Teste", "Falha no teste: " + e.getMessage());
        } finally {
            long msTotal = (System.nanoTime() - t0) / 1_000_000;
            log.info("[TESTE-PRICE] Fim - priceId={} tempoTotal={} ms", id, msTotal);
        }
    }

    public void testarAtual() {
        Long id = (this.price != null ? this.price.getPriceId() : null);
        log.info("[TESTE-PRICE] testarAtual chamado - priceId={}", id);
        testar(this.price);
    }

    private Object invocarTesteNoService(ArquivosPrice cfg) throws Exception {
        Long id = cfg.getPriceId();
        log.info("[TESTE-PRICE] invocarTesteNoService - tentativas por ID e/ou objeto - priceId={}", id);

        if (invokeIfExists(priceTransferService, "testar", new Class<?>[]{ Long.class }, new Object[]{ id })) {
            log.info("[TESTE-PRICE] Método usado: PriceTransferService.testar(Long) - priceId={}", id);
            return lastReturn;
        }

        if (invokeIfExists(priceTransferService, "testarTransfer", new Class<?>[]{ Long.class }, new Object[]{ id })) {
            log.info("[TESTE-PRICE] Método usado: PriceTransferService.testarTransfer(Long) - priceId={}", id);
            return lastReturn;
        }

        if (invokeIfExists(priceTransferService, "testarTransferencia", new Class<?>[]{ Long.class }, new Object[]{ id })) {
            log.info("[TESTE-PRICE] Método usado: PriceTransferService.testarTransferencia(Long) - priceId={}", id);
            return lastReturn;
        }

        if (invokeIfExists(priceTransferService, "testar", new Class<?>[]{ ArquivosPrice.class }, new Object[]{ cfg })) {
            log.info("[TESTE-PRICE] Método usado: PriceTransferService.testar(ArquivosPrice) - priceId={}", id);
            return lastReturn;
        }

        if (invokeIfExists(priceTransferService, "testarTransfer", new Class<?>[]{ ArquivosPrice.class }, new Object[]{ cfg })) {
            log.info("[TESTE-PRICE] Método usado: PriceTransferService.testarTransfer(ArquivosPrice) - priceId={}", id);
            return lastReturn;
        }

        log.error("[TESTE-PRICE] Nenhuma assinatura compatível encontrada - priceId={}", id);

        throw new IllegalStateException(
            "Não encontrei um método de teste compatível em PriceTransferService. " +
            "Esperado: testar(Long) ou testar(ArquivosPrice) (ou variantes)."
        );
    }

    private transient Object lastReturn;

    private boolean invokeIfExists(Object target, String methodName, Class<?>[] paramTypes, Object[] args) throws Exception {
        try {
            Method m = target.getClass().getMethod(methodName, paramTypes);

            log.debug("[TESTE-PRICE] Tentando invoke: {}({})",
                    methodName,
                    (paramTypes != null && paramTypes.length > 0 ? paramTypes[0].getSimpleName() : "sem params"));

            try {
                lastReturn = m.invoke(target, args);
                return true;
            } catch (InvocationTargetException ite) {
                Throwable cause = (ite.getCause() != null ? ite.getCause() : ite);
                log.error("[TESTE-PRICE] Erro dentro do método {}: {}", methodName, cause.getMessage(), cause);

                if (cause instanceof Exception) throw (Exception) cause;
                throw new RuntimeException(cause);
            }

        } catch (NoSuchMethodException nsme) {
            log.debug("[TESTE-PRICE] Método não encontrado: {}({})",
                    methodName,
                    (paramTypes != null && paramTypes.length > 0 ? paramTypes[0].getSimpleName() : "sem params"));
            return false;
        }
    }

    private void publicarMensagensDoResultado(Object r) {
        if (r == null) {
            addMsg(FacesMessage.SEVERITY_ERROR, "Teste", "Resultado do teste veio vazio.");
            log.warn("[TESTE-PRICE] Resultado nulo retornado do service");
            return;
        }

        Boolean okGeral = getBoolean(r, "isOk", "getOk", "isSuccess", "getSuccess");
        Boolean downloadOk = getBoolean(r, "isDownloadOk", "getDownloadOk");
        Boolean fsOk = getBoolean(r, "isFsOk", "getFsOk");
        Boolean smbOk = getBoolean(r, "isSmbOk", "getSmbOk");

        @SuppressWarnings("unchecked")
        List<String> msgs = (List<String>) getObject(r, "getMsgs", "getMensagens", "getMessages");

        StringBuilder detail = new StringBuilder();
        if (downloadOk != null) detail.append("Download SFTP: ").append(downloadOk ? "OK" : "FALHOU").append("\n");
        if (fsOk != null) detail.append("Cópia FS: ").append(fsOk ? "OK" : "FALHOU").append("\n");
        if (smbOk != null) detail.append("Cópia SMB: ").append(smbOk ? "OK" : "FALHOU").append("\n");

        Object arqRemoto = getObject(r, "getArquivoRemoto", "getNomeArquivoRemoto", "getRemoteFile");
        Object arqLocal = getObject(r, "getArquivoLocal", "getLocalFile", "getPathLocal");
        if (arqRemoto != null) detail.append("Arquivo remoto: ").append(arqRemoto).append("\n");
        if (arqLocal != null) detail.append("Arquivo local: ").append(arqLocal).append("\n");

        if (msgs != null && !msgs.isEmpty()) {
            detail.append("\nDetalhes:\n");
            for (String m : msgs) {
                if (m == null) continue;
                detail.append("- ").append(m).append("\n");
            }
        }

        log.info("[TESTE-PRICE] Resultado interpretado - okGeral={} downloadOk={} fsOk={} smbOk={} msgs={}",
                okGeral, downloadOk, fsOk, smbOk, (msgs != null ? msgs.size() : 0));

        FacesMessage.Severity sev;
        if (okGeral != null) {
            sev = okGeral ? FacesMessage.SEVERITY_INFO : FacesMessage.SEVERITY_WARN;
        } else {
            boolean algumFalhou =
                    (downloadOk != null && !downloadOk) ||
                    (fsOk != null && !fsOk) ||
                    (smbOk != null && !smbOk);
            sev = algumFalhou ? FacesMessage.SEVERITY_WARN : FacesMessage.SEVERITY_INFO;
        }

        addMsg(sev, "Teste transferência PRICE", detail.toString().trim());
    }

    private Boolean getBoolean(Object target, String... getters) {
        Object v = getObject(target, getters);
        if (v instanceof Boolean) return (Boolean) v;
        return null;
    }

    private Object getObject(Object target, String... getters) {
        if (target == null) return null;
        for (String g : getters) {
            try {
                Method m = target.getClass().getMethod(g);
                return m.invoke(target);
            } catch (Exception ignore) {
                // tenta próximo
            }
        }
        return null;
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

    // valida/normaliza apenas o tipo selecionado, SEM apagar SMB/FS (para permitir fallback futuramente)
    // ✅ Agora também normaliza os 4 novos campos de MessageFiles, sem tornar obrigatório ainda.
    private void validarENormalizarPorTipoDestino(ArquivosPrice p) {
        if (p == null || p.getTipoDestino() == null) {
            throw new IllegalArgumentException("Selecione o Tipo de Destino.");
        }

        // ✅ novos campos (independe do tipo de destino)
        // (não valida obrigatório aqui — só prepara para cadastro correto)
        p.setMsgCopyAtivo(p.getMsgCopyAtivo() != null ? p.getMsgCopyAtivo() : true);
        p.setMsgFileNomeLocal(trimToNull(p.getMsgFileNomeLocal()));
        p.setMsgSmbCompartilhamento(trimToNull(p.getMsgSmbCompartilhamento()));
        p.setMsgSmbSubpasta(trimToNull(p.getMsgSmbSubpasta()));

        switch (p.getTipoDestino()) {
            case FS:
                p.setCaminhoFsDestino(trimToNull(p.getCaminhoFsDestino()));
                if (isBlank(p.getCaminhoFsDestino())) {
                    throw new IllegalArgumentException("Informe o Caminho FS destino.");
                }
                break;

            case SMB:
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
                break;

            case SFTP:
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

                boolean semSenha = isBlank(p.getDestSenha());
                boolean semChave = isBlank(p.getDestCaminhoChavePrivada());
                if (semSenha && semChave) {
                    throw new IllegalArgumentException("Para SFTP, informe a Senha ou o Caminho da Chave Privada.");
                }
                break;

            default:
                break;
        }
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    // Getters/Setters

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
