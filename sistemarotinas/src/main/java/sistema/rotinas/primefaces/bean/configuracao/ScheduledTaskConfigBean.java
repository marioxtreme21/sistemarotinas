package sistema.rotinas.primefaces.bean.configuracao;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import sistema.rotinas.primefaces.model.scheduled.ScheduledTaskConfig;
import sistema.rotinas.primefaces.service.interfaces.scheduled.IScheduledTaskEngineService;
import sistema.rotinas.primefaces.service.interfaces.scheduled.IScheduledTaskService;
import sistema.rotinas.primefaces.service.scheduled.ScheduledTaskHandlerCatalogService;

@Component
@Named
@ViewScoped
public class ScheduledTaskConfigBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
	private static final String ROTINA_EMPTY_LABEL = "(Nenhuma rotina cadastrada)";

	@Autowired
	private IScheduledTaskService scheduledTaskService;

	@Autowired
	private ScheduledTaskHandlerCatalogService handlerCatalog;

	@Autowired(required = false)
	private IScheduledTaskEngineService engineService;

	// =========================
	// Filtros (Pesquisa)
	// =========================
	/** PRICE | MGV | TARA | ALL */
	private String filtroRotina;

	/** "" (Todos) | PROD | DEV | ALL */
	private String filtroOwner;

	/** true => só enabled=true */
	private boolean somenteAtivas;

	/** busca em taskKey/descricao (case-insensitive) */
	private String filtroTexto;

	/** select: taskKey existente no banco (distinct) */
	private String filtroTaskKey;

	// =========================
	// Listas (UI)
	// =========================
	private List<ScheduledTaskConfig> tasks;
	private List<ScheduledTaskConfig> selecionadas;

	/** opções do combo de taskKeys cadastradas (banco) */
	private List<String> taskKeysCadastradas;

	/** opções do combo de handlers disponíveis (Spring) */
	private List<String> handlersDisponiveis;

	/** opções do combo de rotinas (derivadas das taskKeys do banco) */
	private List<String> rotinasDisponiveis;

	/** se não tem rotinas no banco, desabilita o combo */
	private boolean semRotinasCadastradas;

	// =========================
	// Edição
	// =========================
	private ScheduledTaskConfig editando;

	/** handler escolhido ao criar/editar */
	private String handlerSelecionado;

	private List<String> proximasExecucoes;
	private String erroSelecionado;

	@PostConstruct
	public void init() {
		this.filtroRotina = "ALL";
		this.filtroOwner = "";
		this.somenteAtivas = false;
		this.filtroTexto = "";
		this.filtroTaskKey = "";

		this.tasks = new ArrayList<>();
		this.selecionadas = new ArrayList<>();
		this.proximasExecucoes = new ArrayList<>();

		this.taskKeysCadastradas = new ArrayList<>();
		this.handlersDisponiveis = new ArrayList<>();
		this.rotinasDisponiveis = new ArrayList<>();
		this.semRotinasCadastradas = false;

		carregarCombos();
		recarregar();
	}

	// =========================
	// Carregamentos auxiliares
	// =========================

	public void carregarCombos() {
		// ---- taskKeys do banco (distinct) ----
		try {
			List<String> keysDb = scheduledTaskService.listarTaskKeysCadastradas();
			this.taskKeysCadastradas = (keysDb != null ? keysDb : new ArrayList<>());
		} catch (Exception e) {
			this.taskKeysCadastradas = new ArrayList<>();
			msgWarn("Não foi possível carregar taskKeys do banco: " + e.getMessage());
		}

		// ---- handlers disponíveis (Spring) ----
		try {
			List<String> keysHandlers = handlerCatalog.listHandlerKeys();
			this.handlersDisponiveis = (keysHandlers != null ? keysHandlers : new ArrayList<>());
		} catch (Exception e) {
			this.handlersDisponiveis = new ArrayList<>();
			msgWarn("Não foi possível carregar handlers disponíveis: " + e.getMessage());
		}

		montarRotinasDisponiveisSomenteBanco();
		normalizarFiltroRotinaAposCarregar();
	}

	private void montarRotinasDisponiveisSomenteBanco() {
		Set<String> set = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

		if (taskKeysCadastradas != null) {
			for (String key : taskKeysCadastradas) {
				String r = extrairRotina(key);
				if (r != null && !r.isBlank() && !"ALL".equalsIgnoreCase(r)) {
					set.add(r.toUpperCase(Locale.ROOT));
				}
			}
		}

		this.rotinasDisponiveis = new ArrayList<>(set);
		Collections.sort(this.rotinasDisponiveis, String::compareToIgnoreCase);

		// ✅ controle do estado "sem rotinas"
		this.semRotinasCadastradas = this.rotinasDisponiveis.isEmpty();

		// ✅ IMPORTANTE:
		// NÃO adiciona ROTINA_EMPTY_LABEL dentro da lista.
		// O placeholder deve ser controlado SOMENTE pela view via rendered="#{semRotinasCadastradas}"
	}

	private void normalizarFiltroRotinaAposCarregar() {
		if (semRotinasCadastradas) {
			this.filtroRotina = "ALL";
			return;
		}

		if (filtroRotina == null || filtroRotina.isBlank()) {
			this.filtroRotina = "ALL";
			return;
		}

		if ("ALL".equalsIgnoreCase(filtroRotina)) {
			return;
		}

		boolean existe = rotinasDisponiveis != null
				&& rotinasDisponiveis.stream().anyMatch(r -> r != null && r.equalsIgnoreCase(filtroRotina));

		if (!existe) {
			this.filtroRotina = "ALL";
		}
	}

	private static String extrairRotina(String taskKey) {
		if (taskKey == null)
			return null;
		String s = taskKey.trim();
		if (s.isBlank())
			return null;

		String[] parts = s.split("[_\\-:]");
		if (parts.length == 0)
			return null;

		if (parts.length >= 2 && "ROTINA".equalsIgnoreCase(parts[0])) {
			return parts[1];
		}

		return parts[0];
	}

	// =========================
	// Ações de tela
	// =========================

	public void recarregar() {
		try {
			if (semRotinasCadastradas) {
				filtroRotina = "ALL";
			}

			this.tasks = scheduledTaskService.pesquisar(filtroRotina, filtroOwner, somenteAtivas, filtroTexto);
			if (this.tasks == null)
				this.tasks = new ArrayList<>();

			if (filtroTaskKey != null && !filtroTaskKey.isBlank()) {
				final String tk = filtroTaskKey.trim();
				this.tasks = this.tasks.stream()
						.filter(t -> t != null && t.getTaskKey() != null && t.getTaskKey().equalsIgnoreCase(tk))
						.collect(Collectors.toList());
			}

			this.selecionadas = new ArrayList<>();
			this.editando = null;
			this.handlerSelecionado = null;
			this.proximasExecucoes = new ArrayList<>();
			this.erroSelecionado = null;

			carregarCombos();

		} catch (Exception e) {
			msgErro("Falha ao carregar tasks: " + e.getMessage());
		}
	}

	public void limparFiltros() {
		this.filtroRotina = "ALL";
		this.filtroOwner = "";
		this.somenteAtivas = false;
		this.filtroTexto = "";
		this.filtroTaskKey = "";
		recarregar();
	}

	public void fecharEdicao() {
		this.editando = null;
		this.handlerSelecionado = null;
		this.proximasExecucoes = new ArrayList<>();
		this.erroSelecionado = null;
	}

	public void prepararNovoCadastro() {
		ScheduledTaskConfig t = new ScheduledTaskConfig();
		t.setEnabled(Boolean.TRUE);
		t.setOwner(ScheduledTaskConfig.OWNER_PROD);
		t.setZoneId("America/Sao_Paulo");
		t.setCronExpr("0 0 4 * * *");
		t.setDescricao("");
		t.setTaskKey(null);

		this.editando = t;
		this.handlerSelecionado = null;
		previewNextRuns();
	}

	public void editar(ScheduledTaskConfig t) {
		if (t == null || t.getId() == null)
			return;

		try {
			this.editando = scheduledTaskService.findById(t.getId());
			this.handlerSelecionado = (editando != null ? editando.getTaskKey() : null);
			previewNextRuns();
		} catch (Exception e) {
			msgErro("Falha ao abrir task: " + e.getMessage());
		}
	}

	public void onHandlerSelecionado() {
		if (editando == null)
			return;

		String key = (handlerSelecionado == null ? "" : handlerSelecionado.trim());
		if (key.isBlank())
			return;

		if (!handlerCatalog.exists(key)) {
			msgWarn("Handler não encontrado para a key: " + key);
			return;
		}

		editando.setTaskKey(key);
	}

	public void salvar() {
		if (editando == null)
			return;

		try {
			if (editando.getTaskKey() == null || editando.getTaskKey().isBlank()) {
				msgWarn("Selecione um Handler (Task Key) antes de salvar.");
				return;
			}

			scheduledTaskService.salvar(editando);
			msgInfo("Task salva com sucesso.");
			recarregar();

		} catch (Exception e) {
			msgErro("Falha ao salvar: " + e.getMessage());
		}
	}

	public void toggleEnabled(ScheduledTaskConfig t) {
		if (t == null || t.getId() == null)
			return;

		try {
			boolean enabled = Boolean.TRUE.equals(t.getEnabled());
			scheduledTaskService.atualizarEnabled(t.getId(), enabled);

			msgInfo("Status atualizado: " + (enabled ? "ATIVA" : "INATIVA"));
			recarregar();

		} catch (Exception e) {
			msgErro("Falha ao atualizar status: " + e.getMessage());
		}
	}

	// ===== AÇÕES EM LOTE =====

	public void selecionarTodasVisiveis() {
		this.selecionadas = (tasks != null ? new ArrayList<>(tasks) : new ArrayList<>());
		msgInfo("Selecionadas: " + this.selecionadas.size());
	}

	public void limparSelecao() {
		this.selecionadas = new ArrayList<>();
		msgInfo("Seleção limpa.");
	}

	public void ativarSelecionadas() {
		atualizarEnabledSelecionadas(true);
	}

	public void desativarSelecionadas() {
		atualizarEnabledSelecionadas(false);
	}

	private void atualizarEnabledSelecionadas(boolean enabled) {
		if (selecionadas == null || selecionadas.isEmpty()) {
			msgWarn("Selecione pelo menos 1 task.");
			return;
		}

		try {
			List<Long> ids = selecionadas.stream().filter(x -> x != null && x.getId() != null)
					.map(ScheduledTaskConfig::getId).collect(Collectors.toList());

			if (ids.isEmpty()) {
				msgWarn("Seleção inválida.");
				return;
			}

			scheduledTaskService.atualizarEnabledEmLote(ids, enabled);
			msgInfo("Atualizado em lote: " + (enabled ? "ATIVADAS" : "DESATIVADAS") + " = " + ids.size());

			recarregar();

		} catch (Exception e) {
			msgErro("Falha ao atualizar em lote: " + e.getMessage());
		}
	}

	public void validarJson() {
		if (editando == null)
			return;

		try {
			scheduledTaskService.validarJson(editando.getParamsJson());
			msgInfo("JSON OK.");
		} catch (Exception e) {
			msgErro("JSON inválido: " + e.getMessage());
		}
	}

	public void previewNextRuns() {
		if (editando == null) {
			proximasExecucoes = new ArrayList<>();
			return;
		}

		try {
			String zone = nz(editando.getZoneId(), ZoneId.systemDefault().getId());
			List<LocalDateTime> datas = scheduledTaskService.previewNextRuns(editando.getCronExpr(), zone, 8);

			List<String> out = new ArrayList<>();
			for (LocalDateTime dt : datas) {
				out.add(dt != null ? dt.format(FMT) : "-");
			}
			this.proximasExecucoes = out;

		} catch (Exception e) {
			this.proximasExecucoes = new ArrayList<>();
			msgWarn("Não foi possível calcular preview: " + e.getMessage());
		}
	}

	public void executarAgora() {
		if (editando == null || editando.getId() == null)
			return;

		if (engineService == null) {
			msgWarn("Engine não disponível no momento (IScheduledTaskEngineService não injetado).");
			return;
		}

		try {
			engineService.executarUmaVez(editando.getId());
			msgInfo("Execução solicitada.");
			recarregar();
		} catch (Exception e) {
			msgErro("Falha ao executar agora: " + e.getMessage());
		}
	}

	public void verErro(ScheduledTaskConfig t) {
		this.erroSelecionado = (t != null ? t.getLastError() : null);
		if (erroSelecionado == null || erroSelecionado.isBlank()) {
			erroSelecionado = "(sem erro registrado)";
		}
	}

	public String fmtDt(LocalDateTime dt) {
		return dt == null ? "-" : dt.format(FMT);
	}

	public boolean isEnabledSafe(ScheduledTaskConfig t) {
		return t != null && Boolean.TRUE.equals(t.getEnabled());
	}

	public boolean isEditandoPersistida() {
		return editando != null && editando.getId() != null;
	}

	// =========================
	// Mensagens JSF
	// =========================
	private void msgInfo(String s) {
		FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "OK", s));
	}

	private void msgWarn(String s) {
		FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Atenção", s));
	}

	private void msgErro(String s) {
		FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", s));
	}

	private static String nz(String v, String def) {
		return (v == null || v.isBlank()) ? def : v.trim();
	}

	// =========================
	// Getters/Setters
	// =========================

	public String getFiltroRotina() {
		return filtroRotina;
	}

	public void setFiltroRotina(String filtroRotina) {
		this.filtroRotina = filtroRotina;
	}

	public String getFiltroOwner() {
		return filtroOwner;
	}

	public void setFiltroOwner(String filtroOwner) {
		this.filtroOwner = filtroOwner;
	}

	public boolean isSomenteAtivas() {
		return somenteAtivas;
	}

	public void setSomenteAtivas(boolean somenteAtivas) {
		this.somenteAtivas = somenteAtivas;
	}

	public String getFiltroTexto() {
		return filtroTexto;
	}

	public void setFiltroTexto(String filtroTexto) {
		this.filtroTexto = filtroTexto;
	}

	public String getFiltroTaskKey() {
		return filtroTaskKey;
	}

	public void setFiltroTaskKey(String filtroTaskKey) {
		this.filtroTaskKey = filtroTaskKey;
	}

	public List<ScheduledTaskConfig> getTasks() {
		return tasks;
	}

	public void setTasks(List<ScheduledTaskConfig> tasks) {
		this.tasks = tasks;
	}

	public List<ScheduledTaskConfig> getSelecionadas() {
		return selecionadas;
	}

	public void setSelecionadas(List<ScheduledTaskConfig> selecionadas) {
		this.selecionadas = selecionadas;
	}

	public ScheduledTaskConfig getEditando() {
		return editando;
	}

	public void setEditando(ScheduledTaskConfig editando) {
		this.editando = editando;
	}

	public String getHandlerSelecionado() {
		return handlerSelecionado;
	}

	public void setHandlerSelecionado(String handlerSelecionado) {
		this.handlerSelecionado = handlerSelecionado;
	}

	public List<String> getProximasExecucoes() {
		return proximasExecucoes;
	}

	public void setProximasExecucoes(List<String> proximasExecucoes) {
		this.proximasExecucoes = proximasExecucoes;
	}

	public String getErroSelecionado() {
		return erroSelecionado;
	}

	public void setErroSelecionado(String erroSelecionado) {
		this.erroSelecionado = erroSelecionado;
	}

	public List<String> getTaskKeysCadastradas() {
		return taskKeysCadastradas;
	}

	public void setTaskKeysCadastradas(List<String> taskKeysCadastradas) {
		this.taskKeysCadastradas = taskKeysCadastradas;
	}

	public List<String> getHandlersDisponiveis() {
		return handlersDisponiveis;
	}

	public void setHandlersDisponiveis(List<String> handlersDisponiveis) {
		this.handlersDisponiveis = handlersDisponiveis;
	}

	public List<String> getRotinasDisponiveis() {
		return rotinasDisponiveis;
	}

	public void setRotinasDisponiveis(List<String> rotinasDisponiveis) {
		this.rotinasDisponiveis = rotinasDisponiveis;
	}

	public boolean isSemRotinasCadastradas() {
		return semRotinasCadastradas;
	}
}