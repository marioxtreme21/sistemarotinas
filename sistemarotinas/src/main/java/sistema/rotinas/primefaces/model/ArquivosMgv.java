package sistema.rotinas.primefaces.model;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.*;

@Entity
@Table(name = "arquivos_mgv")
public class ArquivosMgv implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "mgv_id")
	private Long mgvId;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "loja_id", nullable = false, unique = true)
	private Loja loja;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "remote_config_id", nullable = false)
	private LojaRemoteConfig remoteConfig;

	// Subpasta na origem (sob baseDirRemoto)
	@Column(name = "subpasta_remota", length = 500)
	private String subpastaRemota;

	// Verificação diária
	@Column(name = "verificacao_diaria_ativa", nullable = false)
	private Boolean verificacaoDiariaAtiva = true;

	@Column(name = "grace_minutes", nullable = false)
	private Integer graceMinutes = 0;

	@Column(name = "timezone", nullable = false, length = 60)
	private String timezone = "America/Bahia";

	// Pós-processamento remoto
	@Column(name = "mover_remoto_apos_copia", nullable = false)
	private Boolean moverRemotoAposCopia = false;

	@Column(name = "dir_remoto_processed", length = 500)
	private String dirRemotoProcessed;

	// ===== Destino 1: FS montado (primário)
	@Column(name = "usar_fs_montado", nullable = false)
	private Boolean usarFsMontado = true;

	@Column(name = "caminho_fs_destino", length = 500)
	private String caminhoFsDestino; // ex: /mnt/mgv/102

	// ===== Destino 2: SMB direto (fallback)
	@Column(name = "usar_smb_direto", nullable = false)
	private Boolean usarSmbDireto = true;

	@Column(name = "smb_servidor", length = 255)
	private String smbServidor;

	@Column(name = "smb_compartilhamento", length = 255)
	private String smbCompartilhamento;

	@Column(name = "smb_subpasta", length = 255)
	private String smbSubpasta;

	@Column(name = "smb_dominio", length = 255)
	private String smbDominio;

	@Column(name = "smb_usuario", length = 255)
	private String smbUsuario;

	@Column(name = "smb_senha", length = 255)
	private String smbSenha;

	@Column(name = "habilitado", nullable = false)
	private Boolean habilitado = true;

	// Getters/Setters/equals/hashCode/toString
	public Long getMgvId() {
		return mgvId;
	}

	public void setMgvId(Long mgvId) {
		this.mgvId = mgvId;
	}

	public Loja getLoja() {
		return loja;
	}

	public void setLoja(Loja loja) {
		this.loja = loja;
	}

	public LojaRemoteConfig getRemoteConfig() {
		return remoteConfig;
	}

	public void setRemoteConfig(LojaRemoteConfig remoteConfig) {
		this.remoteConfig = remoteConfig;
	}

	public String getSubpastaRemota() {
		return subpastaRemota;
	}

	public void setSubpastaRemota(String subpastaRemota) {
		this.subpastaRemota = subpastaRemota;
	}

	public Boolean getVerificacaoDiariaAtiva() {
		return verificacaoDiariaAtiva;
	}

	public void setVerificacaoDiariaAtiva(Boolean verificacaoDiariaAtiva) {
		this.verificacaoDiariaAtiva = verificacaoDiariaAtiva;
	}

	public Integer getGraceMinutes() {
		return graceMinutes;
	}

	public void setGraceMinutes(Integer graceMinutes) {
		this.graceMinutes = graceMinutes;
	}

	public String getTimezone() {
		return timezone;
	}

	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}

	public Boolean getMoverRemotoAposCopia() {
		return moverRemotoAposCopia;
	}

	public void setMoverRemotoAposCopia(Boolean moverRemotoAposCopia) {
		this.moverRemotoAposCopia = moverRemotoAposCopia;
	}

	public String getDirRemotoProcessed() {
		return dirRemotoProcessed;
	}

	public void setDirRemotoProcessed(String dirRemotoProcessed) {
		this.dirRemotoProcessed = dirRemotoProcessed;
	}

	public Boolean getUsarFsMontado() {
		return usarFsMontado;
	}

	public void setUsarFsMontado(Boolean usarFsMontado) {
		this.usarFsMontado = usarFsMontado;
	}

	public String getCaminhoFsDestino() {
		return caminhoFsDestino;
	}

	public void setCaminhoFsDestino(String caminhoFsDestino) {
		this.caminhoFsDestino = caminhoFsDestino;
	}

	public Boolean getUsarSmbDireto() {
		return usarSmbDireto;
	}

	public void setUsarSmbDireto(Boolean usarSmbDireto) {
		this.usarSmbDireto = usarSmbDireto;
	}

	public String getSmbServidor() {
		return smbServidor;
	}

	public void setSmbServidor(String smbServidor) {
		this.smbServidor = smbServidor;
	}

	public String getSmbCompartilhamento() {
		return smbCompartilhamento;
	}

	public void setSmbCompartilhamento(String smbCompartilhamento) {
		this.smbCompartilhamento = smbCompartilhamento;
	}

	public String getSmbSubpasta() {
		return smbSubpasta;
	}

	public void setSmbSubpasta(String smbSubpasta) {
		this.smbSubpasta = smbSubpasta;
	}

	public String getSmbDominio() {
		return smbDominio;
	}

	public void setSmbDominio(String smbDominio) {
		this.smbDominio = smbDominio;
	}

	public String getSmbUsuario() {
		return smbUsuario;
	}

	public void setSmbUsuario(String smbUsuario) {
		this.smbUsuario = smbUsuario;
	}

	public String getSmbSenha() {
		return smbSenha;
	}

	public void setSmbSenha(String smbSenha) {
		this.smbSenha = smbSenha;
	}

	public Boolean getHabilitado() {
		return habilitado;
	}

	public void setHabilitado(Boolean habilitado) {
		this.habilitado = habilitado;
	}

	@Override
	public int hashCode() {
		return Objects.hash(mgvId);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof ArquivosMgv))
			return false;
		ArquivosMgv that = (ArquivosMgv) o;
		return Objects.equals(mgvId, that.mgvId);
	}

	@Override
	public String toString() {
		return "ArquivosMgv{id=" + mgvId + ", lojaId=" + (loja != null ? loja.getLojaId() : null) + "}";
	}
}
