// FILE: src/main/java/sistema/rotinas/primefaces/bean/porteira/PorteiraBackupBean.java
package sistema.rotinas.primefaces.bean.porteira;

import java.io.Serializable;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import sistema.rotinas.primefaces.model.porteira.PorteiraBackup;
import sistema.rotinas.primefaces.model.porteira.PorteiraBackupUsuario;
import sistema.rotinas.primefaces.model.porteira.PorteiraEletronica;
import sistema.rotinas.primefaces.service.interfaces.porteira.IPorteiraBackupService;
import sistema.rotinas.primefaces.service.interfaces.porteira.IPorteiraEletronicaService;

@Component
@Named
@ViewScoped
public class PorteiraBackupBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

	private final IPorteiraBackupService backupService;
	private final IPorteiraEletronicaService porteiraService;

	// UI data
	private List<PorteiraEletronica> porteiras;
	private List<PorteiraBackup> backups;

	// seleção restore
	private Long backupSelecionadoId;
	private Long porteiraDestinoId;
	private boolean dryRunRestore;

	// seleção backup manual
	private Long porteiraBackupId;

	// detalhe
	private PorteiraBackup backupDetalhe;
	private List<PorteiraBackupUsuario> usuariosBackup;

	// log restore (pra dialog)
	private String ultimoLogRestore;

	public PorteiraBackupBean(IPorteiraBackupService backupService, IPorteiraEletronicaService porteiraService) {
		this.backupService = backupService;
		this.porteiraService = porteiraService;
	}

	@PostConstruct
	public void init() {
		this.porteiras = new ArrayList<>();
		this.backups = new ArrayList<>();
		this.usuariosBackup = new ArrayList<>();
		this.dryRunRestore = false;

		recarregar();
	}

	public void recarregar() {
		try {
			this.porteiras = porteiraService.getAllPorteiras();
			if (this.porteiras == null)
				this.porteiras = new ArrayList<>();

			this.backups = backupService.listar();
			if (this.backups == null)
				this.backups = new ArrayList<>();

			this.backupDetalhe = null;
			this.usuariosBackup = new ArrayList<>();

		} catch (Exception e) {
			msgErro("Falha ao carregar: " + e.getMessage());
		}
	}

	// =========================
	// Ações da tela
	// =========================

	public void executarBackupManual() {
		if (porteiraBackupId == null) {
			msgWarn("Selecione a porteira para backup.");
			return;
		}

		try {
			PorteiraBackup b = backupService.executarBackup(porteiraBackupId);
			msgInfo("Backup executado. status=" + (b != null ? b.getStatus() : "-"));
			recarregar();
		} catch (Exception e) {
			msgErro("Falha no backup: " + e.getMessage());
		}
	}

	public void abrirDetalheBackup(PorteiraBackup b) {
		if (b == null || b.getId() == null)
			return;

		this.backupDetalhe = b;
		this.usuariosBackup = backupService.listarUsuariosDoBackup(b.getId());
		if (this.usuariosBackup == null)
			this.usuariosBackup = new ArrayList<>();
	}

	public void restaurarBackupSelecionado() {
		if (backupSelecionadoId == null) {
			msgWarn("Selecione um backup.");
			return;
		}
		if (porteiraDestinoId == null) {
			msgWarn("Selecione a porteira destino.");
			return;
		}

		try {
			IPorteiraBackupService.RestoreResult rr = backupService.restaurarBackupParaPorteira(backupSelecionadoId,
					porteiraDestinoId, dryRunRestore);

			this.ultimoLogRestore = rr != null ? rr.log : "(sem log)";

			if (rr != null && rr.falha == 0) {
				msgInfo("Restore concluído. total=" + rr.total + " ok=" + rr.ok);
			} else {
				msgWarn("Restore concluído com falhas. total=" + (rr != null ? rr.total : 0) + " ok="
						+ (rr != null ? rr.ok : 0) + " falha=" + (rr != null ? rr.falha : 0));
			}

		} catch (Exception e) {
			msgErro("Falha no restore: " + e.getMessage());
		}
	}

	// ✅ NOVO: Excluir backup selecionado na tabela
	public void excluirBackup(PorteiraBackup b) {
		if (b == null || b.getId() == null)
			return;

		try {
			Long backupId = b.getId();
			backupService.excluirBackup(backupId);

			// se estava selecionado no restore, limpa pra não ficar ID inválido
			if (backupSelecionadoId != null && backupSelecionadoId.equals(backupId)) {
				backupSelecionadoId = null;
			}
			// se estava no detalhe, limpa
			if (backupDetalhe != null && backupId.equals(backupDetalhe.getId())) {
				backupDetalhe = null;
				usuariosBackup = new ArrayList<>();
			}

			msgInfo("Backup excluído com sucesso!");
			recarregar();

		} catch (Exception e) {
			msgErro("Erro ao excluir backup: " + e.getMessage());
		}
	}

	// =========================
	// Helpers
	// =========================

	public String fmt(Object dt) {
		if (dt == null)
			return "-";
		if (dt instanceof java.time.LocalDateTime) {
			return ((java.time.LocalDateTime) dt).format(FMT);
		}
		return String.valueOf(dt);
	}

	private void msgInfo(String s) {
		FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "OK", s));
	}

	private void msgWarn(String s) {
		FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Atenção", s));
	}

	private void msgErro(String s) {
		FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", s));
	}

	// =========================
	// Getters/Setters
	// =========================

	public List<PorteiraEletronica> getPorteiras() {
		return porteiras;
	}

	public void setPorteiras(List<PorteiraEletronica> porteiras) {
		this.porteiras = porteiras;
	}

	public List<PorteiraBackup> getBackups() {
		return backups;
	}

	public void setBackups(List<PorteiraBackup> backups) {
		this.backups = backups;
	}

	public Long getBackupSelecionadoId() {
		return backupSelecionadoId;
	}

	public void setBackupSelecionadoId(Long backupSelecionadoId) {
		this.backupSelecionadoId = backupSelecionadoId;
	}

	public Long getPorteiraDestinoId() {
		return porteiraDestinoId;
	}

	public void setPorteiraDestinoId(Long porteiraDestinoId) {
		this.porteiraDestinoId = porteiraDestinoId;
	}

	public boolean isDryRunRestore() {
		return dryRunRestore;
	}

	public void setDryRunRestore(boolean dryRunRestore) {
		this.dryRunRestore = dryRunRestore;
	}

	public Long getPorteiraBackupId() {
		return porteiraBackupId;
	}

	public void setPorteiraBackupId(Long porteiraBackupId) {
		this.porteiraBackupId = porteiraBackupId;
	}

	public PorteiraBackup getBackupDetalhe() {
		return backupDetalhe;
	}

	public void setBackupDetalhe(PorteiraBackup backupDetalhe) {
		this.backupDetalhe = backupDetalhe;
	}

	public List<PorteiraBackupUsuario> getUsuariosBackup() {
		return usuariosBackup;
	}

	public void setUsuariosBackup(List<PorteiraBackupUsuario> usuariosBackup) {
		this.usuariosBackup = usuariosBackup;
	}

	public String getUltimoLogRestore() {
		return ultimoLogRestore;
	}

	public void setUltimoLogRestore(String ultimoLogRestore) {
		this.ultimoLogRestore = ultimoLogRestore;
	}
}