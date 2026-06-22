package sistema.rotinas.primefaces.bean.loyalty;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import sistema.rotinas.primefaces.enums.OrigemExecucaoEnum;
import sistema.rotinas.primefaces.model.Loja;
import sistema.rotinas.primefaces.model.loyalty.RotinaExecucaoLoyalty;
import sistema.rotinas.primefaces.model.loyalty.RotinaExecucaoLoyaltyCupom;
import sistema.rotinas.primefaces.service.interfaces.ILojaService;
import sistema.rotinas.primefaces.service.interfaces.loyalty.ILoyaltyExecucaoService;
import sistema.rotinas.primefaces.service.loyalty.LoyaltyBatchProperties;

@Component
@Named("loyaltyExecucaoBean")
@SessionScoped
public class LoyaltyExecucaoBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final DateTimeFormatter FMT_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

	@Autowired
	private ILoyaltyExecucaoService loyaltyExecucaoService;

	@Autowired
	private ILojaService lojaService;

	@Autowired
	private LoyaltyBatchProperties loyaltyBatchProperties;

	private boolean selecionarTodas;
	private List<Long> lojaIdsSelecionadas;
	private List<Loja> lojas;

	private LocalDate dataInicial;
	private LocalDate dataFinal;

	private boolean executando;
	private Long ultimaExecucaoId;

	private List<RotinaExecucaoLoyalty> historico;
	private List<RotinaExecucaoLoyaltyCupom> pendencias;

	@PostConstruct
	public void init() {
		dataInicial = LocalDate.now();
		dataFinal = LocalDate.now();
		carregarLojas();
		recarregarTudo();
	}

	public void executar() {
		try {
			validarSelecao();

			executando = true;

			RotinaExecucaoLoyalty execucao = loyaltyExecucaoService.executarCargaNormal(
					lojaIdsSelecionadas,
					dataInicial,
					dataFinal,
					selecionarTodas,
					OrigemExecucaoEnum.MANUAL);

			ultimaExecucaoId = execucao != null ? execucao.getExecucaoLoyaltyId() : null;

			addMessage(FacesMessage.SEVERITY_INFO, "Sucesso", "Execução Loyalty finalizada com sucesso.");
			recarregarTudo();

		} catch (Exception e) {
			addMessage(FacesMessage.SEVERITY_ERROR, "Erro", e.getMessage());
		} finally {
			executando = false;
		}
	}

	public void executarEmLotes() {
		try {
			validarSelecao();

			executando = true;

			RotinaExecucaoLoyalty execucao = loyaltyExecucaoService.executarCargaEmLotes(
					lojaIdsSelecionadas,
					dataInicial,
					dataFinal,
					selecionarTodas,
					OrigemExecucaoEnum.MANUAL);

			ultimaExecucaoId = execucao != null ? execucao.getExecucaoLoyaltyId() : null;

			addMessage(FacesMessage.SEVERITY_INFO,
					"Sucesso",
					"Execução Loyalty em lotes finalizada com sucesso. Tamanho do lote: " + getBatchMaxCupons() + ".");

			recarregarTudo();

		} catch (Exception e) {
			addMessage(FacesMessage.SEVERITY_ERROR, "Erro", e.getMessage());
		} finally {
			executando = false;
		}
	}

	public void reprocessarFalhas() {
		try {
			executando = true;

			int total = loyaltyExecucaoService.reprocessarPendencias(lojaIdsSelecionadas, dataInicial, dataFinal);

			addMessage(FacesMessage.SEVERITY_INFO, "Reprocessamento",
					"Cupons reenviados com sucesso: " + total);
			recarregarTudo();

		} catch (Exception e) {
			addMessage(FacesMessage.SEVERITY_ERROR, "Erro", e.getMessage());
		} finally {
			executando = false;
		}
	}

	public void recarregarTudo() {
		historico = loyaltyExecucaoService.listarHistorico();
		pendencias = loyaltyExecucaoService.listarPendencias();
	}

	public void limparSelecao() {
		selecionarTodas = false;
		lojaIdsSelecionadas = null;
		dataInicial = LocalDate.now();
		dataFinal = LocalDate.now();

		addMessage(FacesMessage.SEVERITY_INFO, "Limpeza", "Seleção e período foram redefinidos.");
	}

	private void carregarLojas() {
		List<Loja> base = lojaService.getLojasPermitidasDoUsuarioLogado();

		if (base == null || base.isEmpty()) {
			base = lojaService.getAllLojas();
		}

		lojas = base.stream()
				.filter(l -> Boolean.TRUE.equals(l.getLoyaltyAtivo()))
				.filter(l -> l.getCodLojaEconect() != null && !l.getCodLojaEconect().isBlank())
				.sorted(
						Comparator
								.comparing(this::parseCodLojaRmsOrdenacao, Comparator.nullsLast(Integer::compareTo))
								.thenComparing(l -> safe(l.getNome()))
				)
				.collect(Collectors.toList());
	}

	private Integer parseCodLojaRmsOrdenacao(Loja loja) {
		try {
			if (loja == null || loja.getCodLojaRms() == null) {
				return null;
			}

			String valor = loja.getCodLojaRms().trim();
			if (valor.isEmpty()) {
				return null;
			}

			return Integer.valueOf(valor);
		} catch (Exception e) {
			return null;
		}
	}

	private String safe(String valor) {
		return valor == null ? "" : valor.trim();
	}

	private void validarSelecao() {
		if (dataInicial == null || dataFinal == null) {
			throw new IllegalArgumentException("Informe data inicial e data final.");
		}
		if (dataFinal.isBefore(dataInicial)) {
			throw new IllegalArgumentException("A data final não pode ser menor que a data inicial.");
		}
		if (!selecionarTodas && (lojaIdsSelecionadas == null || lojaIdsSelecionadas.isEmpty())) {
			throw new IllegalArgumentException("Selecione ao menos uma loja ou marque 'Selecionar todas'.");
		}
	}

	private void addMessage(jakarta.faces.application.FacesMessage.Severity severity, String summary, String detail) {
		FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, summary, detail));
	}

	public String formatarData(LocalDate data) {
		return data != null ? data.format(FMT_DATA) : "-";
	}

	public String formatarDataHora(LocalDateTime dataHora) {
		return dataHora != null ? dataHora.format(FMT_DATA_HORA) : "-";
	}

	public int getBatchMaxCupons() {
		return loyaltyBatchProperties.getMaxCupons();
	}

	public boolean isSelecionarTodas() {
		return selecionarTodas;
	}

	public void setSelecionarTodas(boolean selecionarTodas) {
		this.selecionarTodas = selecionarTodas;
	}

	public List<Long> getLojaIdsSelecionadas() {
		return lojaIdsSelecionadas;
	}

	public void setLojaIdsSelecionadas(List<Long> lojaIdsSelecionadas) {
		this.lojaIdsSelecionadas = lojaIdsSelecionadas;
	}

	public List<Loja> getLojas() {
		return lojas;
	}

	public LocalDate getDataInicial() {
		return dataInicial;
	}

	public void setDataInicial(LocalDate dataInicial) {
		this.dataInicial = dataInicial;
	}

	public LocalDate getDataFinal() {
		return dataFinal;
	}

	public void setDataFinal(LocalDate dataFinal) {
		this.dataFinal = dataFinal;
	}

	public boolean isExecutando() {
		return executando;
	}

	public Long getUltimaExecucaoId() {
		return ultimaExecucaoId;
	}

	public List<RotinaExecucaoLoyalty> getHistorico() {
		return historico;
	}

	public List<RotinaExecucaoLoyaltyCupom> getPendencias() {
		return pendencias;
	}
}