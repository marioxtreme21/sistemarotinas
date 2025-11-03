package sistema.rotinas.primefaces.bean;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import jakarta.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import sistema.rotinas.primefaces.model.Loja;
import sistema.rotinas.primefaces.model.PriceUpdate;
import sistema.rotinas.primefaces.service.ParallelPriceUpdateOrchestrator;
import sistema.rotinas.primefaces.service.interfaces.ILojaService;
import sistema.rotinas.primefaces.service.interfaces.IPriceUpdateService;

@Component
@Named("priceUpdateBean")
@SessionScoped
public class PriceUpdateBean implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final Logger log = LoggerFactory.getLogger(PriceUpdateBean.class);

	@Autowired
	private IPriceUpdateService priceUpdateService;

	@Autowired
	private ILojaService lojaService;

	// Orquestrador (4 lojas em paralelo)
	@Autowired
	private ParallelPriceUpdateOrchestrator parallelPriceUpdateOrchestrator;

	// seleção de loja (null = todas)
	private Long lojaId;
	private List<Loja> lojasPermitidas;

	private boolean executando;

	// Resumo
	private int totalPendentes;
	private int totalRegistros;
	private LocalDateTime ultimaExecucao;

	// Amostra/Tabela
	private List<PriceUpdate> ultimos;

	// ===== Campos de PESQUISA (padrão semelhante ao LojaBean) =====
	private String campoSelecionado;
	private String condicaoSelecionada; // "equal" | "contains"
	private String valorPesquisa;
	private List<SelectItem> camposPesquisa;

	@PostConstruct
	public void init() {
		carregarLojas();
		montarCamposPesquisa();
		refreshResumo();
	}

	private void montarCamposPesquisa() {
		camposPesquisa = List.of(new SelectItem("codigo", "SKU"), new SelectItem("descricao", "Descrição"),
				new SelectItem("loja_id", "Loja (ID)"), new SelectItem("status_envio_vtex", "Enviado VTEX (true/false)")
		// opcional: new SelectItem("data_ultimo_envio", "Data Último Envio (texto)")
		);
	}

	private void carregarLojas() {
		try {
			var doUsuario = lojaService.getLojasPermitidasDoUsuarioLogado(); // pode vir vazio
			var ativasUsuario = doUsuario.stream().filter(l -> Boolean.TRUE.equals(l.getEcommerceAtivo()))
					.sorted(Comparator.comparing(l -> (l.getNome() == null ? "" : l.getNome())))
					.collect(Collectors.toList());

			if (ativasUsuario.isEmpty()) {
				var todas = lojaService.getAllLojas();
				lojasPermitidas = todas.stream().filter(l -> Boolean.TRUE.equals(l.getEcommerceAtivo()))
						.sorted(Comparator.comparing(l -> (l.getNome() == null ? "" : l.getNome())))
						.collect(Collectors.toList());
				log.info("[PriceUpdateBean] Usuário sem lojas ativas vinculadas. Fallback -> {} loja(s) ativas.",
						lojasPermitidas.size());
			} else {
				lojasPermitidas = ativasUsuario;
				log.info("[PriceUpdateBean] Lojas ativas do usuário: {}", lojasPermitidas.size());
			}
		} catch (Exception e) {
			log.warn("[PriceUpdateBean] Falha ao carregar lojas; tentando fallback all-ativas.", e);
			try {
				var todas = lojaService.getAllLojas();
				lojasPermitidas = todas.stream().filter(l -> Boolean.TRUE.equals(l.getEcommerceAtivo()))
						.sorted(Comparator.comparing(l -> (l.getNome() == null ? "" : l.getNome())))
						.collect(Collectors.toList());
			} catch (Exception ex) {
				log.error("[PriceUpdateBean] Fallback também falhou.", ex);
				lojasPermitidas = List.of();
			}
		}
	}

	public void executarAtualizacaoPorLoja() {
		executando = true;
		try {
			priceUpdateService.atualizarPrecosPorLoja(lojaId); // null = todas ativas
			ultimaExecucao = LocalDateTime.now();
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", "Atualização disparada."));
		} catch (Exception ex) {
			log.error("Erro ao atualizar por loja", ex);
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", ex.getMessage()));
		} finally {
			executando = false;
			refreshResumo();
		}
	}

	public void reprocessarPendentes() {
		executando = true;
		try {
			priceUpdateService.reprocessarPendentes();
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_INFO, "OK", "Reprocessamento disparado."));
		} catch (Exception ex) {
			log.error("Erro ao reprocessar pendentes", ex);
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", ex.getMessage()));
		} finally {
			executando = false;
			refreshResumo();
		}
	}

	public void refreshResumo() {
		try {
			totalPendentes = priceUpdateService.countPendentesEnvio();
			List<PriceUpdate> all = priceUpdateService.getAll();
			totalRegistros = all.size();
			ultimos = all.stream()
					.sorted(Comparator.comparing(PriceUpdate::getDataUltimoEnvio,
							Comparator.nullsLast(Comparator.naturalOrder())))
					.collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
						// mais recente primeiro
						java.util.Collections.reverse(list);
						return list.stream().limit(30).collect(Collectors.toList());
					}));
		} catch (Exception e) {
			log.warn("Falha ao carregar resumo/amostra", e);
		}
	}

	// ================== PESQUISA (padrão LojaBean) ==================

	public void pesquisar() {
		log.info("🔍 Iniciando pesquisa de PriceUpdate...");

		boolean campoVazio = campoSelecionado == null || campoSelecionado.isEmpty();
		boolean condicaoVazia = condicaoSelecionada == null || condicaoSelecionada.isEmpty();
		boolean valorVazio = valorPesquisa == null || valorPesquisa.trim().isEmpty();

		// Tudo vazio => volta ao resumo padrão
		if (campoVazio && condicaoVazia && valorVazio) {
			log.info("🔄 Pesquisa vazia — restaurando resumo padrão (últimos 30).");
			refreshResumo();
			return;
		}

		// Validações simples (mesmas do LojaBean)
		if (campoVazio) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_WARN, "Atenção", "Selecione o campo para pesquisa."));
			return;
		}
		if (!valorVazio && condicaoVazia) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_WARN, "Atenção", "Selecione a condição da pesquisa."));
			return;
		}
		if (valorVazio && (!campoVazio && !condicaoVazia)) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_WARN, "Atenção", "Informe o valor para a pesquisa."));
			return;
		}

		try {
			// usa sua API existente — retorna no máximo 30, ordenado por data_ultimo_envio
			// desc
			List<PriceUpdate> result = priceUpdateService.findByCriteria(campoSelecionado, condicaoSelecionada,
					valorPesquisa, 0, 30, "data_ultimo_envio", false);
			ultimos = result;
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_INFO, "Pesquisa", "Resultados atualizados."));
		} catch (Exception e) {
			log.error("Erro na pesquisa de PriceUpdate", e);
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Falha ao pesquisar."));
		}
	}

	public void limparFiltros() {
		campoSelecionado = null;
		condicaoSelecionada = null;
		valorPesquisa = null;
		refreshResumo();
		FacesContext.getCurrentInstance().addMessage(null,
				new FacesMessage(FacesMessage.SEVERITY_INFO, "Filtros limpos", "Pesquisa restaurada."));
	}

	// ================== EXECUÇÃO PARALELA (4 lojas) ==================

	/**
	 * Dispara o envio em paralelo para TODAS as lojas ativas (até 4 simultâneas).
	 */
	public void executarParaleloTodasAsLojas() {
		System.out.println("Acessou metodos executarParaleloTodasAsLojas");
		try {
			parallelPriceUpdateOrchestrator.executarTodasAsLojasEmParalelo();
			ultimaExecucao = LocalDateTime.now();
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso",
					"Envio paralelo disparado para todas as lojas ativas."));
			refreshResumo();
		} catch (Exception ex) {
			log.error("Erro ao executar paralelo (todas as lojas)", ex);
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro",
					"Falha ao disparar envio paralelo: " + ex.getMessage()));
		}
	}

	/**
	 * Dispara o envio em paralelo somente para a loja selecionada (conveniência).
	 */
	public void executarParaleloLojaSelecionada() {
		if (lojaId == null) {
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Atenção",
					"Selecione uma loja para executar em paralelo."));
			return;
		}
		try {
			parallelPriceUpdateOrchestrator.executarLojasEmParalelo(List.of(lojaId));
			ultimaExecucao = LocalDateTime.now();
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso",
					"Envio paralelo disparado para a loja selecionada."));
			refreshResumo();
		} catch (Exception ex) {
			log.error("Erro ao executar paralelo (loja selecionada)", ex);
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro",
					"Falha ao disparar envio paralelo: " + ex.getMessage()));
		}
	}

	/* getters/setters */
	public Long getLojaId() {
		return lojaId;
	}

	public void setLojaId(Long lojaId) {
		this.lojaId = lojaId;
	}

	public List<Loja> getLojasPermitidas() {
		return lojasPermitidas;
	}

	public boolean isExecutando() {
		return executando;
	}

	public int getTotalPendentes() {
		return totalPendentes;
	}

	public int getTotalRegistros() {
		return totalRegistros;
	}

	public LocalDateTime getUltimaExecucao() {
		return ultimaExecucao;
	}

	public List<PriceUpdate> getUltimos() {
		return ultimos;
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
