package sistema.rotinas.primefaces.model;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.*;

/**
 * Configuração remota por loja.
 * Quando {@code global = true}, a configuração é padrão do sistema (todas as lojas)
 * e o campo {@code loja} deve ficar {@code null}.
 */
@Entity
@Table(name = "loja_remote_config")
public class LojaRemoteConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Protocolo {
        FTP, FTPS, SFTP
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "remote_config_id")
    private Long remoteConfigId;

    /**
     * Pode ser null quando a configuração for global.
     * unique=true garante 1 configuração por loja.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "loja_id", nullable = true, unique = true)
    private Loja loja;

    /** Marca se é configuração global (padrão para todas as lojas). */
    @Column(name = "is_global", nullable = false)
    private Boolean global = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "protocolo", nullable = false, length = 10)
    private Protocolo protocolo = Protocolo.FTP;

    @Column(name = "host_remoto", nullable = false, length = 255)
    private String hostRemoto;

    @Column(name = "porta_remota", nullable = false)
    private Integer portaRemota = 21; // FTP/FTPS=21, SFTP=22

    @Column(name = "usuario_remoto", nullable = false, length = 255)
    private String usuarioRemoto;

    @Column(name = "senha_remota", length = 255)
    private String senhaRemota; // Ideal: Vault / criptografado

    @Column(name = "caminho_chave_privada", length = 255)
    private String caminhoChavePrivada; // SFTP opcional

    @Column(name = "base_dir_remoto", length = 500)
    private String baseDirRemoto;

    // FTP/FTPS
    @Column(name = "ftp_passive_mode", nullable = false)
    private Boolean ftpPassiveMode = true;

    @Column(name = "ftps_tls_explicit", nullable = false)
    private Boolean ftpsTlsExplicit = true;

    @Column(name = "validar_certificado", nullable = false)
    private Boolean validarCertificado = false;

    // Resiliência
    @Column(name = "connect_timeout_ms", nullable = false)
    private Integer connectTimeoutMs = 15000;

    @Column(name = "read_timeout_ms", nullable = false)
    private Integer readTimeoutMs = 30000;

    @Column(name = "retries", nullable = false)
    private Integer retries = 3;

    /* ==== Callbacks para manter consistência ==== */
    @PrePersist
    @PreUpdate
    private void ajustarConsistencia() {
        if (Boolean.TRUE.equals(this.global)) {
            // Global não pode ter loja vinculada
            this.loja = null;
        } else {
            // Não-global deve ter loja
            if (this.loja == null) {
                throw new IllegalStateException("Configuração não-global exige loja definida.");
            }
        }
    }

    /* ==== Getters/Setters ==== */
    public Long getRemoteConfigId() {
        return remoteConfigId;
    }

    public void setRemoteConfigId(Long remoteConfigId) {
        this.remoteConfigId = remoteConfigId;
    }

    public Loja getLoja() {
        return loja;
    }

    public void setLoja(Loja loja) {
        this.loja = loja;
        if (loja != null) {
            this.global = false; // se definiu loja, deixa de ser global
        }
    }

    public Boolean getGlobal() {
        return global;
    }

    public boolean isGlobal() {
        return Boolean.TRUE.equals(global);
    }

    public void setGlobal(Boolean global) {
        this.global = (global != null && global);
        if (this.global) {
            this.loja = null; // global zera a loja
        }
    }

    public Protocolo getProtocolo() {
        return protocolo;
    }

    public void setProtocolo(Protocolo protocolo) {
        this.protocolo = protocolo;
    }

    public String getHostRemoto() {
        return hostRemoto;
    }

    public void setHostRemoto(String hostRemoto) {
        this.hostRemoto = hostRemoto;
    }

    public Integer getPortaRemota() {
        return portaRemota;
    }

    public void setPortaRemota(Integer portaRemota) {
        this.portaRemota = portaRemota;
    }

    public String getUsuarioRemoto() {
        return usuarioRemoto;
    }

    public void setUsuarioRemoto(String usuarioRemoto) {
        this.usuarioRemoto = usuarioRemoto;
    }

    public String getSenhaRemota() {
        return senhaRemota;
    }

    public void setSenhaRemota(String senhaRemota) {
        this.senhaRemota = senhaRemota;
    }

    public String getCaminhoChavePrivada() {
        return caminhoChavePrivada;
    }

    public void setCaminhoChavePrivada(String caminhoChavePrivada) {
        this.caminhoChavePrivada = caminhoChavePrivada;
    }

    public String getBaseDirRemoto() {
        return baseDirRemoto;
    }

    public void setBaseDirRemoto(String baseDirRemoto) {
        this.baseDirRemoto = baseDirRemoto;
    }

    public Boolean getFtpPassiveMode() {
        return ftpPassiveMode;
    }

    public void setFtpPassiveMode(Boolean ftpPassiveMode) {
        this.ftpPassiveMode = ftpPassiveMode;
    }

    public Boolean getFtpsTlsExplicit() {
        return ftpsTlsExplicit;
    }

    public void setFtpsTlsExplicit(Boolean ftpsTlsExplicit) {
        this.ftpsTlsExplicit = ftpsTlsExplicit;
    }

    public Boolean getValidarCertificado() {
        return validarCertificado;
    }

    public void setValidarCertificado(Boolean validarCertificado) {
        this.validarCertificado = validarCertificado;
    }

    public Integer getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(Integer connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public Integer getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(Integer readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public Integer getRetries() {
        return retries;
    }

    public void setRetries(Integer retries) {
        this.retries = retries;
    }

    @Override
    public int hashCode() {
        return Objects.hash(remoteConfigId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LojaRemoteConfig)) return false;
        LojaRemoteConfig that = (LojaRemoteConfig) o;
        return Objects.equals(remoteConfigId, that.remoteConfigId);
    }

    @Override
    public String toString() {
        return "LojaRemoteConfig{" +
                "id=" + remoteConfigId +
                ", global=" + global +
                ", lojaId=" + (loja != null ? loja.getLojaId() : null) +
                '}';
    }
}
