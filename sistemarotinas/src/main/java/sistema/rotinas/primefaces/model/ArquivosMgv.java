package sistema.rotinas.primefaces.model;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.*;
import sistema.rotinas.primefaces.crypto.CryptoStringAttributeConverter;

@Entity
@Table(name = "arquivos_mgv")
public class ArquivosMgv implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum TipoDestino {
        FS, SMB
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mgv_id")
    private Long mgvId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loja_id", nullable = false, unique = true)
    private Loja loja;

    /**
     * Config remota de ORIGEM (onde baixa os arquivos do Consinco/SFTP/FTP etc.)
     * Mantém o mesmo padrão do PRICE.
     */
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

    // ===== Destino MGV (normalmente share/FS)
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_destino", nullable = false, length = 10)
    private TipoDestino tipoDestino = TipoDestino.FS;

    /**
     * ✅ COMPAT (DB legado):
     * Alguns bancos têm colunas NOT NULL "usar_fs_montado" e "usar_smb_direto".
     * Mesmo que você use "tipo_destino", precisamos mapear para não quebrar INSERT.
     */
    @Column(name = "usar_fs_montado", nullable = false)
    private Boolean usarFsMontado = true; // default compatível com "FS" (você pode deixar false se preferir)

    @Column(name = "usar_smb_direto", nullable = false)
    private Boolean usarSmbDireto = false;

    /**
     * Alternativa 1: FS montado (ex.: \\servidor\share montado como unidade/pasta no servidor)
     */
    @Column(name = "caminho_fs_destino", length = 500)
    private String caminhoFsDestino;

    /**
     * Alternativa 2: SMB direto (quando não houver montagem)
     */
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
    @Convert(converter = CryptoStringAttributeConverter.class)
    private String smbSenha;

    @Column(name = "habilitado", nullable = false)
    private Boolean habilitado = true;

    // =========================
    // ✅ Hooks para manter coerência entre tipoDestino e flags legados
    // =========================

    @PrePersist
    @PreUpdate
    private void syncLegacyFlags() {
        if (tipoDestino == null) tipoDestino = TipoDestino.FS;

        // se vier nulo, garante false/true pra não quebrar NOT NULL
        if (usarFsMontado == null) usarFsMontado = false;
        if (usarSmbDireto == null) usarSmbDireto = false;

        // Mantém os legados coerentes com tipoDestino
        if (tipoDestino == TipoDestino.FS) {
            usarFsMontado = true;
            usarSmbDireto = false;
        } else if (tipoDestino == TipoDestino.SMB) {
            usarFsMontado = false;
            usarSmbDireto = true;
        }
    }

    // =========================
    // Getters/Setters/equals/hashCode/toString
    // =========================

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

    public TipoDestino getTipoDestino() {
        return tipoDestino;
    }

    public void setTipoDestino(TipoDestino tipoDestino) {
        this.tipoDestino = tipoDestino;
    }

    public Boolean getUsarFsMontado() {
        return usarFsMontado;
    }

    public void setUsarFsMontado(Boolean usarFsMontado) {
        this.usarFsMontado = usarFsMontado;
    }

    public Boolean getUsarSmbDireto() {
        return usarSmbDireto;
    }

    public void setUsarSmbDireto(Boolean usarSmbDireto) {
        this.usarSmbDireto = usarSmbDireto;
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
        return Objects.hash(mgvId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArquivosMgv)) return false;
        ArquivosMgv that = (ArquivosMgv) o;
        return Objects.equals(mgvId, that.mgvId);
    }

    @Override
    public String toString() {
        return "ArquivosMgv{id=" + mgvId + ", lojaId=" + (loja != null ? loja.getLojaId() : null) + "}";
    }
}