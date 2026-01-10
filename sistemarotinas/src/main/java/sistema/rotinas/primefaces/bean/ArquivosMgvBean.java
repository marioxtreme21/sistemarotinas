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
import sistema.rotinas.primefaces.model.ArquivosMgv;
import sistema.rotinas.primefaces.model.ArquivosMgvPattern;
import sistema.rotinas.primefaces.model.Loja;
import sistema.rotinas.primefaces.model.LojaRemoteConfig;
import sistema.rotinas.primefaces.service.MgvTransferService;
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

    private boolean mostrarFormulario;

    private String campoSelecionado;
    private String condicaoSelecionada;
    private String valorPesquisa;

    private List<Loja> lojas;

    // Mantém a senha SMB original ao editar
    private String smbSenhaOriginal;

    // Controle de “modo edição” de senha (máscara ******** + botão Alterar/Cancelar)
    private boolean editarSmbSenha;

    @Autowired
    private IArquivosMgvService service;

    @Autowired
    private IArquivosMgvPatternService patternService;

    @Autowired
    private ILojaService lojaService;

    @Autowired
    private ILojaRemoteConfigService remoteCfgService;

    // serviço responsável por executar o teste (download + cópias FS/SMB)
    @Autowired
    private MgvTransferService mgvTransferService;

    @PostConstruct
    public void init() {
        log.info("Inicializando ArquivosMgvBean");
        this.mgv = new ArquivosMgv();
        garantirDefaultsMgv(this.mgv);

        this.novoPattern = new ArquivosMgvPattern();
        this.patternsDaLoja = new ArrayList<>();
        this.mostrarFormulario = false;

        this.smbSenhaOriginal = null;
        this.editarSmbSenha = false;

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
            List<ArquivosMgv> all = service.findAll();
            all.sort(Comparator.comparing((ArquivosMgv am) -> {
                if (am.getLoja() == null) return "";
                return safe(am.getLoja().getCodLojaRms());
            }, String.CASE_INSENSITIVE_ORDER));
            this.lista = all;
        } catch (Exception e) {
            log.error("Erro ao listar ArquivosMgv", e);
            addMsg(FacesMessage.SEVERITY_ERROR, "Erro", "Falha ao listar registros.");
            this.lista = new ArrayList<>();
        }
    }

    public void prepararNovoCadastro() {
        this.mgv = new ArquivosMgv();
        garantirDefaultsMgv(this.mgv);

        this.novoPattern = new ArquivosMgvPattern();
        this.patternsDaLoja = new ArrayList<>();
        this.mostrarFormulario = true;

        this.smbSenhaOriginal = null;
        this.editarSmbSenha = false;
    }

    public void prepararEditar(ArquivosMgv cfg) {
        editar(cfg);
        this.mostrarFormulario = true;
    }

    public void editar(ArquivosMgv cfg) {
        this.mgv = cfg;
        garantirDefaultsMgv(this.mgv);

        this.smbSenhaOriginal = (cfg != null ? cfg.getSmbSenha() : null);
        this.editarSmbSenha = false;

        // não deixar a senha "viva" no model durante a edição
        if (this.mgv != null) {
            this.mgv.setSmbSenha(null);
        }

        this.novoPattern = new ArquivosMgvPattern();
        carregarPatterns();
    }

    // ========= Controle de edição da senha SMB =========

    public void habilitarEdicaoSmbSenha() {
        this.editarSmbSenha = true;
        if (this.mgv != null) {
            this.mgv.setSmbSenha(null);
        }
    }

    public void cancelarEdicaoSmbSenha() {
        this.editarSmbSenha = false;
        if (this.mgv != null) {
            this.mgv.setSmbSenha(this.smbSenhaOriginal);
        }
    }

    public void salvar() {
        try {
            if (mgv == null) {
                addMsg(FacesMessage.SEVERITY_ERROR, "Erro", "Objeto MGV inválido.");
                this.mostrarFormulario = true;
                return;
            }

            // ✅ garante defaults antes de qualquer coisa (principalmente usarFsMontado)
            garantirDefaultsMgv(mgv);

            if (mgv.getLoja() == null || mgv.getLoja().getLojaId() == null) {
                addMsg(FacesMessage.SEVERITY_WARN, "Validação", "Loja é obrigatória.");
                this.mostrarFormulario = true;
                return;
            }

            // se não informou remoteConfig, tenta resolver efetivo (loja ou GLOBAL)
            if (mgv.getRemoteConfig() == null || mgv.getRemoteConfig().getRemoteConfigId() == null) {
                LojaRemoteConfig efetiva = remoteCfgService.resolveEffectiveForLoja(mgv.getLoja().getLojaId());
                if (efetiva == null) {
                    addMsg(
                        FacesMessage.SEVERITY_WARN,
                        "Validação",
                        "Não existe Configuração Remota para esta loja (nem GLOBAL). Cadastre em Configurações → Configuração Remota (Loja)."
                    );
                    this.mostrarFormulario = true;
                    return;
                }
                mgv.setRemoteConfig(efetiva);
            }

            boolean novo = (mgv.getMgvId() == null);

            // preserva senha original quando:
            // - usuário NÃO clicou em “Alterar”
            // - ou clicou, mas deixou em branco
            if (!novo) {
                if (!this.editarSmbSenha) {
                    mgv.setSmbSenha(this.smbSenhaOriginal);
                } else if (isBlank(mgv.getSmbSenha()) && !isBlank(this.smbSenhaOriginal)) {
                    mgv.setSmbSenha(this.smbSenhaOriginal);
                }
            }

            // valida e normaliza campos conforme tipo selecionado (sem apagar o outro modo)
            validarENormalizarPorTipoDestino(mgv);

            if (novo) {
                service.save(mgv);
            } else {
                service.update(mgv);
            }

            atualizarLista();

            addMsg(
                FacesMessage.SEVERITY_INFO,
                "Sucesso",
                novo ? "Configuração MGV salva." : "Configuração MGV atualizada."
            );

            // fecha e limpa o form (mantém o comportamento atual)
            this.mostrarFormulario = false;
            this.mgv = new ArquivosMgv();
            garantirDefaultsMgv(this.mgv);

            this.novoPattern = new ArquivosMgvPattern();
            this.patternsDaLoja = new ArrayList<>();

            this.smbSenhaOriginal = null;
            this.editarSmbSenha = false;

        } catch (IllegalArgumentException ex) {
            addMsg(FacesMessage.SEVERITY_WARN, "Validação", ex.getMessage());
            this.mostrarFormulario = true;
        } catch (Exception e) {
            log.error("Erro ao salvar MGV", e);
            addMsg(FacesMessage.SEVERITY_ERROR, "Erro", e.getMessage());
            this.mostrarFormulario = true;
        }
    }

    public void excluir(Long id) {
        try {
            List<ArquivosMgvPattern> pats = patternService.listarPorMgv(id);
            int total = (pats == null) ? 0 : pats.size();

            if (pats != null) {
                for (ArquivosMgvPattern p : pats) {
                    patternService.deleteById(p.getPatternId());
                }
            }

            service.deleteById(id);
            atualizarLista();

            if (this.mgv != null && Objects.equals(this.mgv.getMgvId(), id)) {
                this.mgv = new ArquivosMgv();
                garantirDefaultsMgv(this.mgv);

                this.novoPattern = new ArquivosMgvPattern();
                this.patternsDaLoja = new ArrayList<>();
                this.mostrarFormulario = false;

                this.smbSenhaOriginal = null;
                this.editarSmbSenha = false;
            }

            addMsg(
                FacesMessage.SEVERITY_INFO,
                "Sucesso",
                total > 0
                    ? "Configuração excluída e " + total + " pattern(s) removido(s)."
                    : "Configuração excluída com sucesso."
            );

        } catch (Exception e) {
            log.error("Erro ao excluir MGV id={}", id, e);
            addMsg(FacesMessage.SEVERITY_ERROR, "Erro", "Não foi possível excluir. " + e.getMessage());
        }
    }

    public void carregarPatterns() {
        if (this.mgv != null && this.mgv.getMgvId() != null) {
            this.patternsDaLoja = patternService.listarPorMgv(this.mgv.getMgvId());
        } else {
            this.patternsDaLoja = new ArrayList<>();
        }
    }

    public void adicionarPattern() {
        try {
            if (this.mgv == null || this.mgv.getMgvId() == null) {
                addMsg(FacesMessage.SEVERITY_WARN, "Atenção", "Salve a configuração MGV antes de adicionar padrões.");
                return;
            }

            if (novoPattern == null) {
                novoPattern = new ArquivosMgvPattern();
            }

            if (novoPattern.getPattern() == null || novoPattern.getPattern().trim().isEmpty()) {
                addMsg(FacesMessage.SEVERITY_WARN, "Validação", "Informe o Pattern.");
                return;
            }

            novoPattern.setMgv(this.mgv);
            patternService.save(novoPattern);

            this.novoPattern = new ArquivosMgvPattern();
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

    public boolean isMgvPersistido() {
        return this.mgv != null && this.mgv.getMgvId() != null;
    }

    public void pesquisar() {
        log.info("Pesquisa MGV — Campo: {} | Condição: {} | Valor: {}", campoSelecionado, condicaoSelecionada, valorPesquisa);

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
            List<ArquivosMgv> base = service.findAll();
            String needle = valorPesquisa.trim().toLowerCase(Locale.ROOT);
            boolean equal = "equal".equalsIgnoreCase(condicaoSelecionada);

            List<ArquivosMgv> filtrada = new ArrayList<>();
            for (ArquivosMgv cfg : base) {
                String field = extrairCampoPesquisa(cfg, campoSelecionado);
                if (field == null) continue;

                String hay = field.toLowerCase(Locale.ROOT);
                boolean match = equal ? hay.equals(needle) : hay.contains(needle);
                if (match) filtrada.add(cfg);
            }

            filtrada.sort(Comparator.comparing((ArquivosMgv am) -> {
                if (am.getLoja() == null) return "";
                return safe(am.getLoja().getCodLojaRms());
            }, String.CASE_INSENSITIVE_ORDER));

            this.lista = filtrada;
            this.mostrarFormulario = false;

        } catch (Exception e) {
            log.error("Erro ao pesquisar MGV", e);
            addMsg(FacesMessage.SEVERITY_ERROR, "Erro", "Falha ao pesquisar: " + e.getMessage());
        }
    }

    private String extrairCampoPesquisa(ArquivosMgv cfg, String campo) {
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

    public void testar(ArquivosMgv cfg) {
        Long id = (cfg != null ? cfg.getMgvId() : null);
        String lojaCod = (cfg != null && cfg.getLoja() != null ? cfg.getLoja().getCodLojaRms() : null);

        long t0 = System.nanoTime();
        log.info("[TESTE-MGV] Início - mgvId={} loja={}", id, lojaCod);

        try {
            if (cfg == null || cfg.getMgvId() == null) {
                log.warn("[TESTE-MGV] Abortado: cfg nulo ou sem mgvId");
                addMsg(FacesMessage.SEVERITY_WARN, "Teste", "Salve a configuração antes de testar.");
                return;
            }

            log.info("[TESTE-MGV] Preparando chamada ao MgvTransferService - mgvId={}", id);

            Object result = invocarTesteNoService(cfg);

            long ms = (System.nanoTime() - t0) / 1_000_000;
            log.info("[TESTE-MGV] Retorno do service em {} ms - mgvId={}", ms, id);

            Boolean downloadOk = getBoolean(result, "isDownloadOk", "getDownloadOk");
            Boolean fsOk = getBoolean(result, "isFsOk", "getFsOk");
            Boolean smbOk = getBoolean(result, "isSmbOk", "getSmbOk");
            Object arqs = getObject(result, "getArquivos", "getFiles", "getListaArquivos", "getArquivosCopiados");

            log.info("[TESTE-MGV] Resumo - mgvId={} downloadOk={} fsOk={} smbOk={} arquivos={}",
                    id, downloadOk, fsOk, smbOk, arqs);

            publicarMensagensDoResultado(result);

        } catch (IllegalArgumentException ex) {
            log.warn("[TESTE-MGV] Validação - mgvId={} msg={}", id, ex.getMessage());
            addMsg(FacesMessage.SEVERITY_WARN, "Validação", ex.getMessage());
        } catch (Exception e) {
            log.error("[TESTE-MGV] Erro - mgvId={}", id, e);
            addMsg(FacesMessage.SEVERITY_ERROR, "Teste", "Falha no teste: " + e.getMessage());
        } finally {
            long msTotal = (System.nanoTime() - t0) / 1_000_000;
            log.info("[TESTE-MGV] Fim - mgvId={} tempoTotal={} ms", id, msTotal);
        }
    }

    public void testarAtual() {
        Long id = (this.mgv != null ? this.mgv.getMgvId() : null);
        log.info("[TESTE-MGV] testarAtual chamado - mgvId={}", id);
        testar(this.mgv);
    }

    private Object invocarTesteNoService(ArquivosMgv cfg) throws Exception {
        Long id = cfg.getMgvId();
        log.info("[TESTE-MGV] invocarTesteNoService - tentativas por ID e/ou objeto - mgvId={}", id);

        if (invokeIfExists(mgvTransferService, "testar", new Class<?>[]{ Long.class }, new Object[]{ id })) {
            log.info("[TESTE-MGV] Método usado: MgvTransferService.testar(Long) - mgvId={}", id);
            return lastReturn;
        }

        if (invokeIfExists(mgvTransferService, "testarTransfer", new Class<?>[]{ Long.class }, new Object[]{ id })) {
            log.info("[TESTE-MGV] Método usado: MgvTransferService.testarTransfer(Long) - mgvId={}", id);
            return lastReturn;
        }

        if (invokeIfExists(mgvTransferService, "testarTransferencia", new Class<?>[]{ Long.class }, new Object[]{ id })) {
            log.info("[TESTE-MGV] Método usado: MgvTransferService.testarTransferencia(Long) - mgvId={}", id);
            return lastReturn;
        }

        if (invokeIfExists(mgvTransferService, "testar", new Class<?>[]{ ArquivosMgv.class }, new Object[]{ cfg })) {
            log.info("[TESTE-MGV] Método usado: MgvTransferService.testar(ArquivosMgv) - mgvId={}", id);
            return lastReturn;
        }

        if (invokeIfExists(mgvTransferService, "testarTransfer", new Class<?>[]{ ArquivosMgv.class }, new Object[]{ cfg })) {
            log.info("[TESTE-MGV] Método usado: MgvTransferService.testarTransfer(ArquivosMgv) - mgvId={}", id);
            return lastReturn;
        }

        log.error("[TESTE-MGV] Nenhuma assinatura compatível encontrada - mgvId={}", id);

        throw new IllegalStateException(
            "Não encontrei um método de teste compatível em MgvTransferService. " +
            "Esperado: testar(Long) ou testar(ArquivosMgv) (ou variantes)."
        );
    }

    private transient Object lastReturn;

    private boolean invokeIfExists(Object target, String methodName, Class<?>[] paramTypes, Object[] args) throws Exception {
        try {
            Method m = target.getClass().getMethod(methodName, paramTypes);

            log.debug("[TESTE-MGV] Tentando invoke: {}({})",
                    methodName,
                    (paramTypes != null && paramTypes.length > 0 ? paramTypes[0].getSimpleName() : "sem params"));

            try {
                lastReturn = m.invoke(target, args);
                return true;
            } catch (InvocationTargetException ite) {
                Throwable cause = (ite.getCause() != null ? ite.getCause() : ite);
                log.error("[TESTE-MGV] Erro dentro do método {}: {}", methodName, cause.getMessage(), cause);

                if (cause instanceof Exception) throw (Exception) cause;
                throw new RuntimeException(cause);
            }

        } catch (NoSuchMethodException nsme) {
            log.debug("[TESTE-MGV] Método não encontrado: {}({})",
                    methodName,
                    (paramTypes != null && paramTypes.length > 0 ? paramTypes[0].getSimpleName() : "sem params"));
            return false;
        }
    }

    private void publicarMensagensDoResultado(Object r) {
        if (r == null) {
            addMsg(FacesMessage.SEVERITY_ERROR, "Teste", "Resultado do teste veio vazio.");
            log.warn("[TESTE-MGV] Resultado nulo retornado do service");
            return;
        }

        Boolean okGeral = getBoolean(r, "isOk", "getOk", "isSuccess", "getSuccess");
        Boolean downloadOk = getBoolean(r, "isDownloadOk", "getDownloadOk");
        Boolean fsOk = getBoolean(r, "isFsOk", "getFsOk");
        Boolean smbOk = getBoolean(r, "isSmbOk", "getSmbOk");

        @SuppressWarnings("unchecked")
        List<String> msgs = (List<String>) getObject(r, "getMsgs", "getMensagens", "getMessages");

        StringBuilder detail = new StringBuilder();
        if (downloadOk != null) detail.append("Download remoto: ").append(downloadOk ? "OK" : "FALHOU").append("\n");
        if (fsOk != null) detail.append("Cópia FS: ").append(fsOk ? "OK" : "FALHOU").append("\n");
        if (smbOk != null) detail.append("Cópia SMB: ").append(smbOk ? "OK" : "FALHOU").append("\n");

        Object arqs = getObject(r, "getArquivos", "getFiles", "getListaArquivos", "getArquivosCopiados");
        if (arqs != null) detail.append("Arquivos: ").append(arqs).append("\n");

        if (msgs != null && !msgs.isEmpty()) {
            detail.append("\nDetalhes:\n");
            for (String m : msgs) {
                if (m == null) continue;
                detail.append("- ").append(m).append("\n");
            }
        }

        log.info("[TESTE-MGV] Resultado interpretado - okGeral={} downloadOk={} fsOk={} smbOk={} msgs={}",
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

        addMsg(sev, "Teste transferência MGV", detail.toString().trim());
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

    // ✅ NOVO: garante defaults (principalmente usarFsMontado NOT NULL)
    private void garantirDefaultsMgv(ArquivosMgv p) {
        if (p == null) return;

        if (p.getHabilitado() == null) p.setHabilitado(true);
        if (p.getVerificacaoDiariaAtiva() == null) p.setVerificacaoDiariaAtiva(true);
        if (p.getMoverRemotoAposCopia() == null) p.setMoverRemotoAposCopia(false);
        if (p.getGraceMinutes() == null) p.setGraceMinutes(0);
        if (isBlank(p.getTimezone())) p.setTimezone("America/Bahia");

        // ✅ chave do seu erro do banco
        if (p.getUsarFsMontado() == null) p.setUsarFsMontado(false);
    }

    // valida/normaliza apenas o tipo selecionado, SEM apagar SMB/FS (para permitir fallback futuramente)
    private void validarENormalizarPorTipoDestino(ArquivosMgv p) {
        if (p == null || p.getTipoDestino() == null) {
            throw new IllegalArgumentException("Selecione o Tipo de Destino.");
        }

        // defaults básicos
        garantirDefaultsMgv(p);

        switch (p.getTipoDestino()) {
            case FS:
                // ✅ coerência: se o destino é FS, então “usar FS montado” deve ficar true
                p.setUsarFsMontado(true);

                p.setCaminhoFsDestino(trimToNull(p.getCaminhoFsDestino()));
                if (isBlank(p.getCaminhoFsDestino())) {
                    throw new IllegalArgumentException("Informe o Caminho FS destino.");
                }
                break;

            case SMB:
                // ✅ coerência: se o destino é SMB, não usa FS montado
                p.setUsarFsMontado(false);

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

            default:
                // proteção extra
                if (p.getUsarFsMontado() == null) p.setUsarFsMontado(false);
                break;
        }

        // campos remotos opcionais
        p.setSubpastaRemota(trimToNull(p.getSubpastaRemota()));
        p.setDirRemotoProcessed(trimToNull(p.getDirRemotoProcessed()));
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    // =====================
    // Getters / Setters
    // =====================

    public ArquivosMgv getMgv() {
        return mgv;
    }

    public void setMgv(ArquivosMgv mgv) {
        this.mgv = mgv;
        garantirDefaultsMgv(this.mgv);
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

    public boolean isEditarSmbSenha() {
        return editarSmbSenha;
    }

    public void setEditarSmbSenha(boolean editarSmbSenha) {
        this.editarSmbSenha = editarSmbSenha;
    }
}