// src/main/java/sistema/ecommerce/primefaces/bean/InventoryUpdateBean.java
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

import org.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import sistema.rotinas.primefaces.model.InventoryUpdate;
import sistema.rotinas.primefaces.model.Loja;
import sistema.rotinas.primefaces.service.interfaces.IInventoryUpdateService;
import sistema.rotinas.primefaces.service.interfaces.ILojaService;

@Component
@Named("inventoryUpdateBean")
@SessionScoped
public class InventoryUpdateBean implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final Logger log = LoggerFactory.getLogger(InventoryUpdateBean.class);

	@Autowired
	private IInventoryUpdateService inventoryService;
	@Autowired
	private ILojaService lojaService;

	private Long lojaId;
	private List<Loja> lojasPermitidas;
	private boolean executando;

	private int totalPendentes;
	private int totalRegistros;
	private LocalDateTime ultimaExecucao;
	private List<InventoryUpdate> ultimos;

	private String campoSelecionado;
	private String condicaoSelecionada;
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
				new SelectItem("loja_id", "Loja (ID)"),
				new SelectItem("status_envio_vtex", "Enviado VTEX (true/false)"));
	}

	private void carregarLojas() {
		try {
			var doUsuario = lojaService.getLojasPermitidasDoUsuarioLogado();
			var ativasUsuario = doUsuario.stream().filter(l -> Boolean.TRUE.equals(l.getEcommerceAtivo()))
					.sorted(Comparator.comparing(l -> (l.getNome() == null ? "" : l.getNome())))
					.collect(Collectors.toList());

			if (ativasUsuario.isEmpty()) {
				var todas = lojaService.getAllLojas();
				lojasPermitidas = todas.stream().filter(l -> Boolean.TRUE.equals(l.getEcommerceAtivo()))
						.sorted(Comparator.comparing(l -> (l.getNome() == null ? "" : l.getNome())))
						.collect(Collectors.toList());
			} else {
				lojasPermitidas = ativasUsuario;
			}
		} catch (Exception e) {
			try {
				var todas = lojaService.getAllLojas();
				lojasPermitidas = todas.stream().filter(l -> Boolean.TRUE.equals(l.getEcommerceAtivo()))
						.sorted(Comparator.comparing(l -> (l.getNome() == null ? "" : l.getNome())))
						.collect(Collectors.toList());
			} catch (Exception ex) {
				log.error("[InventoryUpdateBean] Falha ao carregar lojas.", ex);
				lojasPermitidas = List.of();
			}
		}
	}

	public void executarAtualizacaoPorLoja() {
		executando = true;
		try {
			inventoryService.atualizarEstoquePorLoja(lojaId);
			ultimaExecucao = LocalDateTime.now();
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", "Atualização de estoque disparada."));
		} catch (Exception ex) {
			log.error("Erro ao atualizar estoque por loja", ex);
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
			inventoryService.reprocessarPendentes();
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_INFO, "OK", "Reprocessamento de estoque disparado."));
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
			totalPendentes = inventoryService.countPendentesEnvio();
			List<InventoryUpdate> all = inventoryService.getAll();
			totalRegistros = all.size();
			ultimos = all.stream()
					.sorted(Comparator.comparing(InventoryUpdate::getDataUltimoEnvio,
							Comparator.nullsLast(Comparator.naturalOrder())))
					.collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
						java.util.Collections.reverse(list);
						return list.stream().limit(30).collect(Collectors.toList());
					}));
		} catch (Exception e) {
			log.warn("Falha ao carregar resumo/amostra", e);
		}
	}

	public void pesquisar() {
		boolean campoVazio = campoSelecionado == null || campoSelecionado.isEmpty();
		boolean condicaoVazia = condicaoSelecionada == null || condicaoSelecionada.isEmpty();
		boolean valorVazio = valorPesquisa == null || valorPesquisa.trim().isEmpty();

		if (campoVazio && condicaoVazia && valorVazio) {
			refreshResumo();
			return;
		}

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
			List<InventoryUpdate> result = inventoryService.findByCriteria(campoSelecionado, condicaoSelecionada,
					valorPesquisa, 0, 30, "data_ultimo_envio", false);
			ultimos = result;
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_INFO, "Pesquisa", "Resultados atualizados."));
		} catch (Exception e) {
			log.error("Erro na pesquisa de InventoryUpdate", e);
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

	// getters/setters
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

	public List<InventoryUpdate> getUltimos() {
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
