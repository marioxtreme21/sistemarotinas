package sistema.rotinas.primefaces.bean;

import java.io.Serializable;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
import sistema.rotinas.primefaces.model.Loja;
import sistema.rotinas.primefaces.model.LojaRemoteConfig;
import sistema.rotinas.primefaces.model.LojaRemoteConfig.Protocolo;
import sistema.rotinas.primefaces.service.interfaces.ILojaRemoteConfigService;
import sistema.rotinas.primefaces.service.interfaces.ILojaService;

@Component
@Named
@SessionScoped
public class LojaRemoteConfigBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger log = LoggerFactory.getLogger(LojaRemoteConfigBean.class);

	@Autowired
	private ILojaRemoteConfigService lojaRemoteConfigService;
	@Autowired
	private ILojaService lojaService;

	/** Registro em edição. */
	private LojaRemoteConfig cfg;

	/** Lista para a tabela. */
	private List<LojaRemoteConfig> lista;

	/** Lojas para o combo. */
	private List<Loja> lojas;

	/** ID da loja selecionada no form. */
	private Long selectedLojaId;

	/** Itens do combo de protocolo. */
	private List<SelectItem> protocolos;

	/** Campo apenas da tela. Se informado, substitui a senha do cfg ao salvar. */
	private String senhaInput;

	/** Checkbox da tela. Mantemos sincronizado com cfg.global. */
	private boolean globalFlag;

	// ----------------------------------------------------------------------

	@PostConstruct
	public void init() {
		log.info("Inicializando LojaRemoteConfigBean");
		this.cfg = new LojaRemoteConfig();
		this.globalFlag = false;
		this.cfg.setGlobal(false); // manter entidade coerente com a tela
		this.senhaInput = "";
		carregarLojas();
		carregarProtocolos();
		atualizarLista();
	}

	private void carregarLojas() {
		try {
			this.lojas = lojaService.getAllLojas();
			if (this.lojas != null) {
				this.lojas = this.lojas.stream().sorted((a, b) -> a.getNome().compareToIgnoreCase(b.getNome()))
						.collect(Collectors.toList());
			}
		} catch (Exception e) {
			log.error("Erro ao carregar lojas", e);
			addMsg(FacesMessage.SEVERITY_ERROR, "Erro", "Falha ao carregar lojas.");
		}
	}

	private void carregarProtocolos() {
		this.protocolos = Arrays.stream(Protocolo.values()).map(p -> new SelectItem(p, p.name()))
				.collect(Collectors.toList());
	}

	private void atualizarLista() {
		try {
			this.lista = lojaRemoteConfigService.findAll();
		} catch (Exception e) {
			log.error("Erro ao listar configurações remotas", e);
			addMsg(FacesMessage.SEVERITY_ERROR, "Erro", "Falha ao listar registros.");
		}
	}

	// ----------------------------------------------------------------------
	// Ações da toolbar / formulário
	// ----------------------------------------------------------------------

	/** Novo registro por LOJA. */
	public void prepararNovo() {
		this.cfg = new LojaRemoteConfig();
		this.globalFlag = false;
		this.cfg.setGlobal(false);
		this.selectedLojaId = null;
		this.cfg.setLoja(null);
		this.senhaInput = "";
	}

	/** Novo registro GLOBAL. */
	public void prepararNovoGlobal() {
		this.cfg = new LojaRemoteConfig();
		this.globalFlag = true;
		this.cfg.setGlobal(true);
		this.selectedLojaId = null;
		this.cfg.setLoja(null);
		this.senhaInput = "";
	}

	/**
	 * Opcional – se quiser carregar automaticamente a config ao escolher a loja. Só
	 * terá efeito se você ligar este método via <p:ajax listener=...> no
	 * selectOneMenu.
	 */
	public void onSelecionarLoja() {
		try {
			if (selectedLojaId == null) {
				// usuário limpou a loja; não forçamos global automaticamente
				this.senhaInput = "";
				return;
			}
			LojaRemoteConfig existente = lojaRemoteConfigService.findByLojaId(selectedLojaId);
			if (existente != null) {
				this.cfg = existente;
				this.globalFlag = Boolean.TRUE.equals(existente.getGlobal());
				// se veio por loja, garantir coerência
				if (!this.globalFlag && existente.getLoja() != null) {
					this.selectedLojaId = existente.getLoja().getLojaId();
				}
			} else {
				this.cfg = new LojaRemoteConfig();
				Loja loja = lojas.stream().filter(l -> l.getLojaId().equals(selectedLojaId)).findFirst().orElse(null);
				this.cfg.setLoja(loja);
				this.globalFlag = false;
				this.cfg.setGlobal(false);
			}
			this.senhaInput = "";
		} catch (Exception e) {
			log.error("Erro ao selecionar loja", e);
			addMsg(FacesMessage.SEVERITY_ERROR, "Erro", "Falha ao carregar a configuração da loja.");
		}
	}

	public void editar(LojaRemoteConfig c) {
		this.cfg = c;
		this.globalFlag = Boolean.TRUE.equals(c.getGlobal());
		this.selectedLojaId = (c.getLoja() == null ? null : c.getLoja().getLojaId());
		this.senhaInput = "";
	}

	public void excluir(Long id) {
		try {
			lojaRemoteConfigService.deleteById(id);
			atualizarLista();
			addMsg(FacesMessage.SEVERITY_INFO, "Sucesso", "Excluído com sucesso.");
		} catch (Exception e) {
			log.error("Erro ao excluir configuração remota", e);
			addMsg(FacesMessage.SEVERITY_ERROR, "Erro", "Não foi possível excluir.");
		}
	}

	public void salvar() {
		try {
			// 1) Sincroniza a entidade com o checkbox da tela
			cfg.setGlobal(globalFlag);

			// 2) Resolve GLOBAL x POR-LOJA coerente para o serviço validar
			if (Boolean.TRUE.equals(cfg.getGlobal())) {
				cfg.setLoja(null); // Global => loja NULL
				this.selectedLojaId = null; // espelha no form
			} else {
				if (selectedLojaId == null) {
					addMsg(FacesMessage.SEVERITY_WARN, "Validação",
							"Loja é obrigatória quando a configuração não for global.");
					return;
				}
				Loja loja = lojas.stream().filter(l -> l.getLojaId().equals(selectedLojaId)).findFirst().orElse(null);
				if (loja == null) {
					addMsg(FacesMessage.SEVERITY_WARN, "Validação", "Loja inválida.");
					return;
				}
				cfg.setLoja(loja);
			}

			// 3) Senha: só atualiza se foi digitada
			if (senhaInput != null && !senhaInput.isBlank()) {
				cfg.setSenhaRemota(senhaInput);
			}

			boolean novo = (cfg.getRemoteConfigId() == null);
			LojaRemoteConfig salvo = (novo ? lojaRemoteConfigService.save(cfg) : lojaRemoteConfigService.update(cfg));

			this.cfg = salvo;
			// mantém a UI em sincronia
			this.globalFlag = Boolean.TRUE.equals(salvo.getGlobal());
			this.selectedLojaId = (salvo.getLoja() != null ? salvo.getLoja().getLojaId() : null);

			this.senhaInput = "";
			atualizarLista();

			addMsg(FacesMessage.SEVERITY_INFO, "Sucesso", "Configuração salva.");
		} catch (Exception e) {
			log.error("Erro ao salvar configuração remota", e);
			addMsg(FacesMessage.SEVERITY_ERROR, "Erro", "Falha ao salvar: " + e.getMessage());
		}
	}

	// ----------------------------------------------------------------------
	// Teste de Conexão
	// ----------------------------------------------------------------------

	public void testarConexao() {
		try {
			LojaRemoteConfig paraTeste = cloneParaTeste(cfg, senhaInput);
			int timeoutMs = (paraTeste.getConnectTimeoutMs() != null && paraTeste.getConnectTimeoutMs() > 0)
					? paraTeste.getConnectTimeoutMs()
					: 20000;

			String resultado = lojaRemoteConfigService.testConnection(paraTeste, Duration.ofMillis(timeoutMs));
			showResultadoTeste(resultado);
		} catch (Exception e) {
			log.error("Erro ao testar conexão (toolbar)", e);
			addMsg(FacesMessage.SEVERITY_ERROR, "Falha de conexão", e.getMessage());
		}
	}

	public void testarConexao(LojaRemoteConfig salvo) {
		try {
			int timeoutMs = (salvo.getConnectTimeoutMs() != null && salvo.getConnectTimeoutMs() > 0)
					? salvo.getConnectTimeoutMs()
					: 20000;
			String resultado = lojaRemoteConfigService.testConnection(salvo, Duration.ofMillis(timeoutMs));
			showResultadoTeste(resultado);
		} catch (Exception e) {
			log.error("Erro ao testar conexão (linha)", e);
			addMsg(FacesMessage.SEVERITY_ERROR, "Falha de conexão", e.getMessage());
		}
	}

	private LojaRemoteConfig cloneParaTeste(LojaRemoteConfig base, String senhaDigitada) {
		LojaRemoteConfig x = new LojaRemoteConfig();
		x.setRemoteConfigId(base.getRemoteConfigId());
		x.setLoja(base.getLoja()); // se for global, continuará null
		x.setGlobal(base.getGlobal());

		x.setProtocolo(base.getProtocolo());
		x.setHostRemoto(base.getHostRemoto());
		x.setPortaRemota(base.getPortaRemota());
		x.setUsuarioRemoto(base.getUsuarioRemoto());

		String efetiva = (senhaDigitada != null && !senhaDigitada.isBlank()) ? senhaDigitada : base.getSenhaRemota();
		if (efetiva != null && !efetiva.isBlank()) {
			x.setSenhaRemota(efetiva);
		}

		x.setCaminhoChavePrivada(base.getCaminhoChavePrivada());
		x.setBaseDirRemoto(base.getBaseDirRemoto());
		x.setFtpPassiveMode(Boolean.TRUE.equals(base.getFtpPassiveMode()));
		x.setFtpsTlsExplicit(Boolean.TRUE.equals(base.getFtpsTlsExplicit()));
		x.setValidarCertificado(Boolean.TRUE.equals(base.getValidarCertificado()));
		x.setConnectTimeoutMs(base.getConnectTimeoutMs());
		x.setReadTimeoutMs(base.getReadTimeoutMs());
		x.setRetries(base.getRetries());
		return x;
	}

	private void showResultadoTeste(String resultado) {
		if (resultado == null) {
			addMsg(FacesMessage.SEVERITY_WARN, "Aviso", "Sem retorno do teste.");
			return;
		}
		String r = resultado.trim();
		if (r.toUpperCase().startsWith("OK")) {
			addMsg(FacesMessage.SEVERITY_INFO, "Conexão OK", r);
		} else {
			addMsg(FacesMessage.SEVERITY_WARN, "Falha de conexão", r);
		}
	}

	private void addMsg(FacesMessage.Severity sev, String sum, String detail) {
		FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(sev, sum, detail));
	}

	// ----------------------------------------------------------------------
	// GETTERS / SETTERS
	// ----------------------------------------------------------------------

	public LojaRemoteConfig getCfg() {
		return cfg;
	}

	public void setCfg(LojaRemoteConfig cfg) {
		this.cfg = cfg;
	}

	public List<LojaRemoteConfig> getLista() {
		return lista;
	}

	public void setLista(List<LojaRemoteConfig> lista) {
		this.lista = lista;
	}

	public List<Loja> getLojas() {
		return lojas;
	}

	public void setLojas(List<Loja> lojas) {
		this.lojas = lojas;
	}

	public Long getSelectedLojaId() {
		return selectedLojaId;
	}

	public void setSelectedLojaId(Long selectedLojaId) {
		this.selectedLojaId = selectedLojaId;
	}

	public List<SelectItem> getProtocolos() {
		return protocolos;
	}

	public void setProtocolos(List<SelectItem> protocolos) {
		this.protocolos = protocolos;
	}

	public String getSenhaInput() {
		return senhaInput;
	}

	public void setSenhaInput(String senhaInput) {
		this.senhaInput = senhaInput;
	}

	/** Usado na view (checkbox Global). */
	public boolean isGlobalFlag() {
		return globalFlag;
	}

	public void setGlobalFlag(boolean globalFlag) {
		this.globalFlag = globalFlag;
		if (this.cfg == null)
			this.cfg = new LojaRemoteConfig();
		this.cfg.setGlobal(globalFlag); // *** manter a ENTIDADE em sincronia ***
		if (globalFlag) {
			// se virar Global na tela, limpamos a loja selecionada e no entity
			this.selectedLojaId = null;
			this.cfg.setLoja(null);
		}
	}
}
