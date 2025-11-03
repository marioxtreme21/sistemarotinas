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
import sistema.rotinas.primefaces.model.Loja;
import sistema.rotinas.primefaces.service.interfaces.ILojaService;

@Component
@Named
@SessionScoped
public class LojaBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger log = LoggerFactory.getLogger(LojaBean.class);

	private Loja loja;
	private CarregamentoLazyListForObject<Loja> lojasLazy;

	// Campos para pesquisa
	private String campoSelecionado;
	private String condicaoSelecionada; // "equal" | "contains"
	private String valorPesquisa;
	private List<SelectItem> camposPesquisa;
	private boolean mostrarFormulario;

	@Autowired
	private ILojaService lojaService;

	@PostConstruct
	public void init() {
		log.info("Inicializando LojaBean");
		mostrarFormulario = false;
		loja = new Loja();
		carregarLojasSobDemanda();

		// ⚠️ Use exatamente os nomes das colunas da tabela "loja"
		camposPesquisa = new ArrayList<>();
		camposPesquisa.add(new SelectItem("nome", "Nome"));
		camposPesquisa.add(new SelectItem("cnpj", "CNPJ"));
		camposPesquisa.add(new SelectItem("politica_comercial", "Política Comercial"));
		camposPesquisa.add(new SelectItem("ecommerce_ativo", "E-commerce Ativo"));
		camposPesquisa.add(new SelectItem("pick_and_pack_ativo", "Pick and Pack Ativo"));
		// ✅ Novos no filtro:
		camposPesquisa.add(new SelectItem("horario_price_update", "Horário Price Update"));
		camposPesquisa.add(new SelectItem("warehouse", "Warehouse"));
		camposPesquisa.add(new SelectItem("prioridade_envio_ativo", "Prioridade Ativa"));
		camposPesquisa.add(new SelectItem("prioridade_envio_ranking", "Ranking Prioridade"));
	}

	public void carregarLojasSobDemanda() {
		log.debug("Iniciando carregamento sob demanda de lojas...");
		lojasLazy = new CarregamentoLazyListForObject<>((first, pageSize) -> {
			List<Loja> lojas = lojaService.findAllLojas(first, pageSize, null, true);
			log.debug("Lojas carregadas: {}", lojas.size());
			return lojas;
		}, () -> {
			int total = lojaService.countLojas();
			log.debug("Total de lojas: {}", total);
			return total;
		});
	}

	public void prepararNovoCadastro() {
		log.info("Preparando novo cadastro de loja");
		this.loja = new Loja();
		this.mostrarFormulario = true;
	}

	public void salvar() {
		try {
			log.info("🔧 Salvando loja: {}", loja);

			lojaService.save(loja);
			carregarLojasSobDemanda();
			loja = new Loja();

			mostrarFormulario = false;

			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", "Loja salva com sucesso!"));
		} catch (IllegalArgumentException ex) {
			log.error("⚠️ Erro ao salvar loja: {}", ex.getMessage());
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", ex.getMessage()));
			mostrarFormulario = true;
		} catch (Exception ex) {
			log.error("❌ Erro inesperado ao salvar loja.", ex);
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro",
					"Ocorreu um erro ao salvar. Verifique os dados e tente novamente."));
			mostrarFormulario = true;
		}
	}

	public void prepararEditar(Loja loja) {
		log.info("Preparando edição da loja: {}", loja);
		this.loja = loja;
		editar(loja);
		this.mostrarFormulario = true;
	}

	public void editar(Loja loja) {
		log.info("Editando loja: {}", loja);
		this.loja = loja;
	}

	public void excluir(Long id) {
		log.warn("Excluindo loja com ID: {}", id);
		lojaService.deleteById(id);
		carregarLojasSobDemanda();
		FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Loja excluída com sucesso!"));
	}

	public void pesquisar() {
		log.info("🔍 Iniciando pesquisa de Loja...");

		boolean campoVazio = campoSelecionado == null || campoSelecionado.isEmpty();
		boolean condicaoVazia = condicaoSelecionada == null || condicaoSelecionada.isEmpty();
		boolean valorVazio = valorPesquisa == null || valorPesquisa.trim().isEmpty();

		if (campoVazio && condicaoVazia && valorVazio) {
			log.info("🔄 Pesquisa vazia — carregando todos os registros.");
			carregarLojasSobDemanda();
			mostrarFormulario = false;
			return;
		}

		if (campoVazio) {
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Atenção",
					"Selecione o campo para realizar a pesquisa."));
			return;
		}

		if (!valorVazio && condicaoVazia) {
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Atenção",
					"Selecione a condição para realizar a pesquisa."));
			return;
		}

		if (valorVazio && (!campoVazio && !condicaoVazia)) {
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Atenção",
					"Informe o valor para realizar a pesquisa."));
			return;
		}

		log.info(String.format("🎯 Executando pesquisa — Campo: %s | Condição: %s | Valor: %s", campoSelecionado,
				condicaoSelecionada, valorPesquisa));

		lojasLazy = new CarregamentoLazyListForObject<>(
				(first, pageSize) -> lojaService.findLojasByCriteria(campoSelecionado, condicaoSelecionada,
						valorPesquisa, first, pageSize, null, true),
				() -> lojaService.countLojasByCriteria(campoSelecionado, condicaoSelecionada, valorPesquisa));

		mostrarFormulario = false;
	}

	public void limparFiltros() {
		campoSelecionado = null;
		condicaoSelecionada = null;
		valorPesquisa = null;
		carregarLojasSobDemanda();
		FacesContext.getCurrentInstance().addMessage(null,
				new FacesMessage(FacesMessage.SEVERITY_INFO, "Filtros limpos", "Todos os filtros foram removidos."));
	}

	// Getters e Setters
	public Loja getLoja() {
		return loja;
	}

	public void setLoja(Loja loja) {
		this.loja = loja;
	}

	public CarregamentoLazyListForObject<Loja> getLojasLazy() {
		return lojasLazy;
	}

	public void setLojasLazy(CarregamentoLazyListForObject<Loja> lojasLazy) {
		this.lojasLazy = lojasLazy;
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
