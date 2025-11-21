package sistema.rotinas.primefaces.bean;

import java.io.Serializable;
import java.util.List;

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
		price = new ArquivosPrice();
		novoPattern = new ArquivosPricePattern();
		atualizarLista();
	}

	public void atualizarLista() {
		this.lista = service.findAll();
	}

	public void prepararNovoCadastro() {
		this.price = new ArquivosPrice();
		this.novoPattern = new ArquivosPricePattern();
		this.patternsDaLoja = null;
	}

	public void selecionarLoja(Loja loja) {
		this.price.setLoja(loja);
		LojaRemoteConfig cfg = remoteCfgService.findByLojaId(loja.getLojaId());
		if (cfg != null)
			this.price.setRemoteConfig(cfg);
		carregarPatterns();
	}

	public void carregarPatterns() {
		if (this.price != null && this.price.getPriceId() != null) {
			this.patternsDaLoja = patternService.listarPorPrice(this.price.getPriceId());
		}
	}

	public void salvar() {
		try {
			if (price.getPriceId() == null) {
				service.save(price);
			} else {
				service.update(price);
			}
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", "Configuração PRICE salva."));
			atualizarLista();
			prepararNovoCadastro();
		} catch (Exception e) {
			log.error("Erro ao salvar PRICE", e);
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", e.getMessage()));
		}
	}

	public void editar(ArquivosPrice cfg) {
		this.price = cfg;
		carregarPatterns();
	}

	public void excluir(Long id) {
		try {
			service.deleteById(id);
			atualizarLista();
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", "Excluído com sucesso."));
		} catch (Exception e) {
			log.error("Erro ao excluir PRICE", e);
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Não foi possível excluir."));
		}
	}

	// Patterns
	public void adicionarPattern() {
		try {
			if (this.price == null || this.price.getPriceId() == null) {
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN,
						"Atenção", "Salve a configuração PRICE antes de adicionar padrões."));
				return;
			}
			novoPattern.setPrice(this.price);
			patternService.save(novoPattern);
			this.novoPattern = new ArquivosPricePattern();
			carregarPatterns();
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", "Pattern adicionado."));
		} catch (Exception e) {
			log.error("Erro ao adicionar pattern", e);
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", e.getMessage()));
		}
	}

	public void removerPattern(Long patternId) {
		try {
			patternService.deleteById(patternId);
			carregarPatterns();
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", "Pattern removido."));
		} catch (Exception e) {
			log.error("Erro ao remover pattern", e);
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Não foi possível remover o pattern."));
		}
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

	public List<Loja> getLojas() {
		return lojaService.getAllLojas();
	}
}
