package sistema.rotinas.primefaces.bean.rotina;

import java.io.Serializable;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import sistema.rotinas.primefaces.enums.OrigemExecucaoEnum;
import sistema.rotinas.primefaces.enums.TipoRotinaEnum;
import sistema.rotinas.primefaces.model.Loja;
import sistema.rotinas.primefaces.model.rotina.RotinaExecucao;
import sistema.rotinas.primefaces.model.rotina.RotinaExecucaoArquivo;
import sistema.rotinas.primefaces.model.rotina.RotinaExecucaoArquivoEtapa;
import sistema.rotinas.primefaces.model.rotina.RotinaExecucaoLoja;
import sistema.rotinas.primefaces.repository.RotinaExecucaoArquivoEtapaRepository;
import sistema.rotinas.primefaces.repository.RotinaExecucaoArquivoRepository;
import sistema.rotinas.primefaces.repository.RotinaExecucaoLojaRepository;
import sistema.rotinas.primefaces.repository.RotinaExecucaoRepository;
import sistema.rotinas.primefaces.service.interfaces.ILojaService;
import sistema.rotinas.primefaces.service.interfaces.IRotinaMgvRunnerService;

@Component
@Named
@SessionScoped
public class RotinaMgvExecucaoBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Autowired
    private ILojaService lojaService;

    @Autowired
    private IRotinaMgvRunnerService runner;

    // ✅ Repos para histórico/detalhe
    @Autowired
    private RotinaExecucaoRepository execRepo;

    @Autowired
    private RotinaExecucaoLojaRepository execLojaRepo;

    @Autowired
    private RotinaExecucaoArquivoRepository execArqRepo;

    // ✅ etapas
    @Autowired
    private RotinaExecucaoArquivoEtapaRepository execArqEtapaRepo;

    // ======= Execução =======
    private List<Loja> lojas;
    private List<Long> lojaIdsSelecionadas;

    private boolean selecionarTodas;
    private Long ultimaExecucaoId;

    // ======= Histórico =======
    private List<RotinaExecucao> historico;
    private Integer limiteHistorico = 20;

    // ======= Detalhe (dialog) =======
    private RotinaExecucao execucaoSelecionada;
    private List<RotinaExecucaoLoja> detalheLojas;
    private List<RotinaExecucaoArquivo> detalheArquivos;

    // ✅ detalhe das etapas
    private List<RotinaExecucaoArquivoEtapa> detalheEtapas;

    @PostConstruct
    public void init() {
        this.lojas = new ArrayList<>(lojaService.getAllLojas());
        this.lojaIdsSelecionadas = new ArrayList<>();
        this.selecionarTodas = true;

        this.historico = new ArrayList<>();
        this.detalheLojas = new ArrayList<>();
        this.detalheArquivos = new ArrayList<>();
        this.detalheEtapas = new ArrayList<>();

        carregarHistorico();
    }

    public void executar() {
        try {
            List<Long> ids = (selecionarTodas ? null : lojaIdsSelecionadas);
            String usuario = resolveUsuarioLogado();

            this.ultimaExecucaoId = runner.executar(ids, OrigemExecucaoEnum.MANUAL, usuario);

            addMsg(FacesMessage.SEVERITY_INFO, "Rotina MGV", "Execução registrada. ID: " + ultimaExecucaoId);

            // ✅ atualiza histórico
            carregarHistorico();

            // ✅ opcional: já abre o detalhe da última execução
            abrirDetalhe(this.ultimaExecucaoId);

        } catch (Exception e) {
            addMsg(FacesMessage.SEVERITY_ERROR, "Rotina MGV", "Falha ao executar: " + e.getMessage());
        }
    }

    // =========================
    // Histórico
    // =========================
    public void carregarHistorico() {
        try {
            List<RotinaExecucao> all = execRepo.findAll();

            List<RotinaExecucao> mgv = all.stream()
                    .filter(e -> e != null && e.getTipoRotina() == TipoRotinaEnum.MGV)
                    .sorted(Comparator.comparing((RotinaExecucao e) -> {
                        LocalDateTime ini = e.getInicioEm();
                        return (ini != null ? ini : LocalDateTime.MIN);
                    }).reversed())
                    .toList();

            int lim = (limiteHistorico != null && limiteHistorico > 0) ? limiteHistorico : 20;
            this.historico = (mgv.size() > lim ? new ArrayList<>(mgv.subList(0, lim)) : new ArrayList<>(mgv));

        } catch (Exception e) {
            this.historico = new ArrayList<>();
        }
    }

    // aliases (se algum xhtml usou outro nome)
    public void atualizarHistorico() {
        carregarHistorico();
    }

    // ✅ compatibilidade com xhtml: caso exista “recarregarHistorico”
    public void recarregarHistorico() {
        carregarHistorico();
        addMsg(FacesMessage.SEVERITY_INFO, "Rotina MGV", "Histórico recarregado.");
    }

    // ✅ compatibilidade com xhtml: botão “Limpar seleção”
    public void limparSelecao() {
        try {
            this.selecionarTodas = true;
            if (this.lojaIdsSelecionadas == null) {
                this.lojaIdsSelecionadas = new ArrayList<>();
            } else {
                this.lojaIdsSelecionadas.clear();
            }
            addMsg(FacesMessage.SEVERITY_INFO, "Rotina MGV", "Seleção limpa.");
        } catch (Exception e) {
            addMsg(FacesMessage.SEVERITY_WARN, "Rotina MGV", "Não consegui limpar a seleção: " + e.getMessage());
        }
    }

    // ✅ o DataTable da página usa getExecucoes()
    public List<RotinaExecucao> getExecucoes() {
        return historico;
    }

    // =========================
    // Detalhe (dialog)
    // =========================
    public void abrirDetalhe(Long execucaoId) {
        if (execucaoId == null) return;

        try {
            this.execucaoSelecionada = execRepo.findById(execucaoId).orElse(null);

            if (this.execucaoSelecionada == null) {
                this.detalheLojas = new ArrayList<>();
                this.detalheArquivos = new ArrayList<>();
                this.detalheEtapas = new ArrayList<>();
                return;
            }

            // lojas da execução
            List<RotinaExecucaoLoja> lojasExec = execLojaRepo.findAll().stream()
                    .filter(x -> x != null
                            && x.getExecucao() != null
                            && execucaoId.equals(x.getExecucao().getExecucaoId()))
                    .toList();

            // ordena por codLojaRms
            List<RotinaExecucaoLoja> lojasOrdenadas = new ArrayList<>(lojasExec);
            lojasOrdenadas.sort(Comparator.comparing(
                    RotinaExecucaoLoja::getCodLojaRms,
                    Comparator.nullsLast(String::compareToIgnoreCase)
            ));
            this.detalheLojas = lojasOrdenadas;

            // arquivos por lojas
            List<Long> lojasIds = lojasExec.stream()
                    .map(RotinaExecucaoLoja::getExecucaoLojaId)
                    .toList();

            List<RotinaExecucaoArquivo> arqs = execArqRepo.findAll().stream()
                    .filter(a -> a != null
                            && a.getExecucaoLoja() != null
                            && a.getExecucaoLoja().getExecucaoLojaId() != null
                            && lojasIds.contains(a.getExecucaoLoja().getExecucaoLojaId()))
                    .toList();

            // ordena por loja e depois id
            List<RotinaExecucaoArquivo> arqsOrdenados = new ArrayList<>(arqs);
            arqsOrdenados.sort(
                    Comparator
                            .comparing((RotinaExecucaoArquivo a) -> {
                                try {
                                    return a.getExecucaoLoja() != null ? a.getExecucaoLoja().getExecucaoLojaId() : null;
                                } catch (Exception ex) {
                                    return null;
                                }
                            }, Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(RotinaExecucaoArquivo::getExecucaoArquivoId,
                                    Comparator.nullsLast(Comparator.naturalOrder()))
            );
            this.detalheArquivos = arqsOrdenados;

            // ✅ carrega etapas (rotina_execucao_arquivo_etapa)
            List<Long> arquivoIds = arqsOrdenados.stream()
                    .map(RotinaExecucaoArquivo::getExecucaoArquivoId)
                    .filter(id -> id != null)
                    .toList();

            if (arquivoIds.isEmpty()) {
                this.detalheEtapas = new ArrayList<>();
            } else {
                List<RotinaExecucaoArquivoEtapa> etapas = execArqEtapaRepo.findAll().stream()
                        .filter(t -> t != null
                                && t.getExecucaoArquivo() != null
                                && t.getExecucaoArquivo().getExecucaoArquivoId() != null
                                && arquivoIds.contains(t.getExecucaoArquivo().getExecucaoArquivoId()))
                        .toList();

                List<RotinaExecucaoArquivoEtapa> etapasOrdenadas = new ArrayList<>(etapas);
                etapasOrdenadas.sort(
                        Comparator
                                .comparing(this::safeArquivoIdFromEtapa, Comparator.nullsLast(Comparator.naturalOrder()))
                                .thenComparing(RotinaMgvExecucaoBean::safeInicioEtapa, Comparator.nullsLast(Comparator.naturalOrder()))
                                .thenComparing(this::safeEtapaPk, Comparator.nullsLast(Comparator.naturalOrder()))
                );

                this.detalheEtapas = etapasOrdenadas;
            }

        } catch (Exception e) {
            this.execucaoSelecionada = null;
            this.detalheLojas = new ArrayList<>();
            this.detalheArquivos = new ArrayList<>();
            this.detalheEtapas = new ArrayList<>();
        }
    }

    // se algum xhtml chama passando o objeto da linha
    public void abrirDetalhe(RotinaExecucao exec) {
        if (exec == null) return;
        abrirDetalhe(exec.getExecucaoId());
    }

    public void fecharDetalhe() {
        this.execucaoSelecionada = null;
        this.detalheLojas = new ArrayList<>();
        this.detalheArquivos = new ArrayList<>();
        this.detalheEtapas = new ArrayList<>();
    }

    /**
     * ✅ Para o dialog:
     * Retorna os arquivos da loja expandida.
     */
    public List<RotinaExecucaoArquivo> arquivosPorLoja(Long execucaoLojaId) {
        if (execucaoLojaId == null || detalheArquivos == null || detalheArquivos.isEmpty()) {
            return new ArrayList<>();
        }

        return detalheArquivos.stream()
                .filter(a -> a != null
                        && a.getExecucaoLoja() != null
                        && a.getExecucaoLoja().getExecucaoLojaId() != null
                        && execucaoLojaId.equals(a.getExecucaoLoja().getExecucaoLojaId()))
                .sorted(Comparator.comparing(RotinaExecucaoArquivo::getExecucaoArquivoId,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    /**
     * ✅ Etapas por arquivo (para a expansão dentro da linha do arquivo)
     */
    public List<RotinaExecucaoArquivoEtapa> etapasPorArquivo(Long execucaoArquivoId) {
        if (execucaoArquivoId == null || detalheEtapas == null || detalheEtapas.isEmpty()) {
            return new ArrayList<>();
        }

        return detalheEtapas.stream()
                .filter(t -> t != null
                        && t.getExecucaoArquivo() != null
                        && t.getExecucaoArquivo().getExecucaoArquivoId() != null
                        && execucaoArquivoId.equals(t.getExecucaoArquivo().getExecucaoArquivoId()))
                .sorted(
                        Comparator
                                .comparing(RotinaMgvExecucaoBean::safeInicioEtapa, Comparator.nullsLast(Comparator.naturalOrder()))
                                .thenComparing(this::safeEtapaPk, Comparator.nullsLast(Comparator.naturalOrder()))
                )
                .toList();
    }

    // =========================
    // Helpers para ordenação segura
    // =========================
    private Long safeArquivoIdFromEtapa(RotinaExecucaoArquivoEtapa t) {
        try {
            return t != null && t.getExecucaoArquivo() != null ? t.getExecucaoArquivo().getExecucaoArquivoId() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static LocalDateTime safeInicioEtapa(RotinaExecucaoArquivoEtapa t) {
        try {
            return t != null ? t.getInicioEm() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * ✅ PK da etapa: usa etapaId (e mantém fallback por reflexão).
     */
    private Long safeEtapaPk(RotinaExecucaoArquivoEtapa t) {
        if (t == null) return null;

        try {
            if (t.getEtapaId() != null) return t.getEtapaId();
        } catch (Exception ignore) { }

        String[] getters = {"getEtapaId", "getExecucaoArquivoEtapaId", "getId"};
        for (String g : getters) {
            try {
                Object v = t.getClass().getMethod(g).invoke(t);
                if (v instanceof Long) return (Long) v;
            } catch (Exception ignore) { }
        }
        return null;
    }

    // =========================

    private String resolveUsuarioLogado() {
        try {
            FacesContext fc = FacesContext.getCurrentInstance();
            if (fc == null) return null;

            Principal p = fc.getExternalContext().getUserPrincipal();
            if (p != null && p.getName() != null && !p.getName().isBlank()) {
                return p.getName();
            }

            String remoteUser = fc.getExternalContext().getRemoteUser();
            return (remoteUser != null && !remoteUser.isBlank()) ? remoteUser : null;

        } catch (Exception e) {
            return null;
        }
    }

    private void addMsg(FacesMessage.Severity sev, String sum, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(sev, sum, detail));
    }

    // ======= Getters/Setters (mantidos) =======

    public List<Loja> getLojas() {
        return lojas;
    }

    public List<Long> getLojaIdsSelecionadas() {
        return lojaIdsSelecionadas;
    }

    public void setLojaIdsSelecionadas(List<Long> lojaIdsSelecionadas) {
        this.lojaIdsSelecionadas = lojaIdsSelecionadas;
    }

    public boolean isSelecionarTodas() {
        return selecionarTodas;
    }

    public void setSelecionarTodas(boolean selecionarTodas) {
        this.selecionarTodas = selecionarTodas;
    }

    public Long getUltimaExecucaoId() {
        return ultimaExecucaoId;
    }

    // ======= Histórico/Detalhe getters =======

    public List<RotinaExecucao> getHistorico() {
        return historico;
    }

    public void setHistorico(List<RotinaExecucao> historico) {
        this.historico = historico;
    }

    public Integer getLimiteHistorico() {
        return limiteHistorico;
    }

    public void setLimiteHistorico(Integer limiteHistorico) {
        this.limiteHistorico = limiteHistorico;
    }

    public RotinaExecucao getExecucaoSelecionada() {
        return execucaoSelecionada;
    }

    public void setExecucaoSelecionada(RotinaExecucao execucaoSelecionada) {
        this.execucaoSelecionada = execucaoSelecionada;
    }

    public List<RotinaExecucaoLoja> getDetalheLojas() {
        return detalheLojas;
    }

    public void setDetalheLojas(List<RotinaExecucaoLoja> detalheLojas) {
        this.detalheLojas = detalheLojas;
    }

    public List<RotinaExecucaoArquivo> getDetalheArquivos() {
        return detalheArquivos;
    }

    public void setDetalheArquivos(List<RotinaExecucaoArquivo> detalheArquivos) {
        this.detalheArquivos = detalheArquivos;
    }

    public List<RotinaExecucaoArquivoEtapa> getDetalheEtapas() {
        return detalheEtapas;
    }

    public void setDetalheEtapas(List<RotinaExecucaoArquivoEtapa> detalheEtapas) {
        this.detalheEtapas = detalheEtapas;
    }

    // aliases extras (caso algum xhtml use outro nome)
    public RotinaExecucao getDetalheExecucao() {
        return execucaoSelecionada;
    }

    public List<RotinaExecucaoLoja> getLojasDetalhe() {
        return detalheLojas;
    }

    public List<RotinaExecucaoArquivo> getArquivosDetalhe() {
        return detalheArquivos;
    }

    public List<RotinaExecucaoArquivoEtapa> getEtapasDetalhe() {
        return detalheEtapas;
    }
}