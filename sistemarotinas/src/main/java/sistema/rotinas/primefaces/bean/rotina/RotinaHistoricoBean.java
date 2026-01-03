package sistema.rotinas.primefaces.bean.rotina;

import java.io.Serializable;
import java.time.LocalDate;
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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import sistema.rotinas.primefaces.enums.StatusExecucaoEnum;
import sistema.rotinas.primefaces.enums.TipoRotinaEnum;
import sistema.rotinas.primefaces.model.rotina.RotinaExecucao;
import sistema.rotinas.primefaces.model.rotina.RotinaExecucaoArquivo;
import sistema.rotinas.primefaces.model.rotina.RotinaExecucaoLoja;
import sistema.rotinas.primefaces.repository.RotinaExecucaoRepository;

@Component
@Named
@SessionScoped
public class RotinaHistoricoBean implements Serializable {

	private static final long serialVersionUID = 1L;

	@Autowired
	private RotinaExecucaoRepository execucaoRepo;

	@PersistenceContext
	private EntityManager em;

	private TipoRotinaEnum tipoFiltro;
	private StatusExecucaoEnum statusFiltro;

	private LocalDate dataIni;
	private LocalDate dataFim;

	private List<RotinaExecucao> execucoes;

	private RotinaExecucao execucaoSelecionada;
	private List<RotinaExecucaoLoja> lojasDaExecucao;
	private List<RotinaExecucaoArquivo> arquivosDaExecucao;

	@PostConstruct
	public void init() {
		dataFim = LocalDate.now();
		dataIni = dataFim.minusDays(10);
		execucoes = new ArrayList<>();
		lojasDaExecucao = new ArrayList<>();
		arquivosDaExecucao = new ArrayList<>();
		pesquisar();
	}

	public void pesquisar() {
		List<RotinaExecucao> all = execucaoRepo.findAll();

		LocalDateTime ini = (dataIni != null ? dataIni.atStartOfDay() : null);
		LocalDateTime fim = (dataFim != null ? dataFim.plusDays(1).atStartOfDay() : null);

		List<RotinaExecucao> filtrada = new ArrayList<>();
		for (RotinaExecucao e : all) {
			if (tipoFiltro != null && e.getTipoRotina() != tipoFiltro)
				continue;
			if (statusFiltro != null && e.getStatus() != statusFiltro)
				continue;

			LocalDateTime inicio = e.getInicioEm();
			if (ini != null && inicio != null && inicio.isBefore(ini))
				continue;
			if (fim != null && inicio != null && !inicio.isBefore(fim))
				continue;

			filtrada.add(e);
		}

		filtrada.sort(Comparator.comparing(RotinaExecucao::getExecucaoId).reversed());
		this.execucoes = filtrada;

		// limpa detalhe
		this.execucaoSelecionada = null;
		this.lojasDaExecucao = new ArrayList<>();
		this.arquivosDaExecucao = new ArrayList<>();
	}

	public void selecionarExecucao(RotinaExecucao e) {
		if (e == null || e.getExecucaoId() == null)
			return;

		this.execucaoSelecionada = e;

		try {
			this.lojasDaExecucao = em
					.createQuery("select l from RotinaExecucaoLoja l " + "where l.execucao.execucaoId = :id "
							+ "order by l.codLojaRms asc", RotinaExecucaoLoja.class)
					.setParameter("id", e.getExecucaoId()).getResultList();

			this.arquivosDaExecucao = em
					.createQuery("select a from RotinaExecucaoArquivo a " + "where a.execucao.execucaoId = :id "
							+ "order by a.codLojaRms asc, a.patternEsperado asc", RotinaExecucaoArquivo.class)
					.setParameter("id", e.getExecucaoId()).getResultList();

		} catch (Exception ex) {
			addMsg(FacesMessage.SEVERITY_ERROR, "Histórico", "Falha ao carregar detalhe: " + ex.getMessage());
			this.lojasDaExecucao = new ArrayList<>();
			this.arquivosDaExecucao = new ArrayList<>();
		}
	}

	public void limparFiltros() {
		this.tipoFiltro = null;
		this.statusFiltro = null;
		this.dataFim = LocalDate.now();
		this.dataIni = this.dataFim.minusDays(10);
		pesquisar();
	}

	private void addMsg(FacesMessage.Severity sev, String sum, String detail) {
		FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(sev, sum, detail));
	}

	// Getters/Setters

	public TipoRotinaEnum getTipoFiltro() {
		return tipoFiltro;
	}

	public void setTipoFiltro(TipoRotinaEnum tipoFiltro) {
		this.tipoFiltro = tipoFiltro;
	}

	public StatusExecucaoEnum getStatusFiltro() {
		return statusFiltro;
	}

	public void setStatusFiltro(StatusExecucaoEnum statusFiltro) {
		this.statusFiltro = statusFiltro;
	}

	public LocalDate getDataIni() {
		return dataIni;
	}

	public void setDataIni(LocalDate dataIni) {
		this.dataIni = dataIni;
	}

	public LocalDate getDataFim() {
		return dataFim;
	}

	public void setDataFim(LocalDate dataFim) {
		this.dataFim = dataFim;
	}

	public List<RotinaExecucao> getExecucoes() {
		return execucoes;
	}

	public RotinaExecucao getExecucaoSelecionada() {
		return execucaoSelecionada;
	}

	public List<RotinaExecucaoLoja> getLojasDaExecucao() {
		return lojasDaExecucao;
	}

	public List<RotinaExecucaoArquivo> getArquivosDaExecucao() {
		return arquivosDaExecucao;
	}
}
