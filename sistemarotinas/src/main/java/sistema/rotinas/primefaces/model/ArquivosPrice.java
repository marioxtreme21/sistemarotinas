package sistema.rotinas.primefaces.model;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.*;

@Entity
@Table(name = "arquivos_price")
public class ArquivosPrice implements Serializable {

	private static final long serialVersionUID = 1L;

	public enum TipoDestino {
		FS, SFTP, SMB
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "price_id")
	private Long priceId;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "loja_id", nullable = false, unique = true)
	private Loja loja;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "remote_config_id", nullable = false)
	private LojaRemoteConfig remoteConfig;

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

	// ===== Destino PRICE (padrão: SFTP por loja)
	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_destino", nullable = false, length = 10)
	private TipoDestino tipoDestino = TipoDestino.SFTP;

	// SFTP destino (por loja)
	@Column(name = "dest_host", length = 255)
	private String destHost;

	@Column(name = "dest_port", nullable = true)
	private Integer destPort = 22;

	@Column(name = "dest_usuario", length = 255)
	private String destUsuario;

	@Column(name = "dest_senha", length = 255)
	private String destSenha;

	@Column(name = "dest_caminho_chave_privada", length = 255)
	private String destCaminhoChavePrivada;

	@Column(name = "dest_known_hosts_path", length = 255)
	private String destKnownHostsPath;

	@Column(name = "dest_dir_remoto", length = 500)
	private String destDirRemoto;

	// Alternativas: FS montado / SMB direto
	@Column(name = "caminho_fs_destino", length = 500)
	private String caminhoFsDestino;

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
	public Long getPriceId() {
		return priceId;
	}

	public void setPriceId(Long priceId) {
		this.priceId = priceId;
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

	public TipoDestino getTipoDestino() {
		return tipoDestino;
	}

	public void setTipoDestino(TipoDestino tipoDestino) {
		this.tipoDestino = tipoDestino;
	}

	public String getDestHost() {
		return destHost;
	}

	public void setDestHost(String destHost) {
		this.destHost = destHost;
	}

	public Integer getDestPort() {
		return destPort;
	}

	public void setDestPort(Integer destPort) {
		this.destPort = destPort;
	}

	public String getDestUsuario() {
		return destUsuario;
	}

	public void setDestUsuario(String destUsuario) {
		this.destUsuario = destUsuario;
	}

	public String getDestSenha() {
		return destSenha;
	}

	public void setDestSenha(String destSenha) {
		this.destSenha = destSenha;
	}

	public String getDestCaminhoChavePrivada() {
		return destCaminhoChavePrivada;
	}

	public void setDestCaminhoChavePrivada(String destCaminhoChavePrivada) {
		this.destCaminhoChavePrivada = destCaminhoChavePrivada;
	}

	public String getDestKnownHostsPath() {
		return destKnownHostsPath;
	}

	public void setDestKnownHostsPath(String destKnownHostsPath) {
		this.destKnownHostsPath = destKnownHostsPath;
	}

	public String getDestDirRemoto() {
		return destDirRemoto;
	}

	public void setDestDirRemoto(String destDirRemoto) {
		this.destDirRemoto = destDirRemoto;
	}

	public String getCaminhoFsDestino() {
		return caminhoFsDestino;
	}

	public void setCaminhoFsDestino(String caminhoFsDestino) {
		this.caminhoFsDestino = caminhoFsDestino;
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
		return Objects.hash(priceId);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof ArquivosPrice))
			return false;
		ArquivosPrice that = (ArquivosPrice) o;
		return Objects.equals(priceId, that.priceId);
	}

	@Override
	public String toString() {
		return "ArquivosPrice{id=" + priceId + ", lojaId=" + (loja != null ? loja.getLojaId() : null) + "}";
	}
}
