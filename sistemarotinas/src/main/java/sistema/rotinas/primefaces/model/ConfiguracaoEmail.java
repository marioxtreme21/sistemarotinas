package sistema.rotinas.primefaces.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "configuracao_email")
public class ConfiguracaoEmail implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔹 SMTP - Envio
    @Column(name = "servidor_smtp", nullable = false, length = 150)
    private String servidorSmtp;

    @Column(name = "porta_smtp", nullable = false)
    private Integer portaSmtp;

    @Column(name = "usar_tls_smtp", nullable = false)
    private Boolean usarTlsSmtp = true;

    @Column(name = "usar_ssl_smtp", nullable = false)
    private Boolean usarSslSmtp = false;

    // 🔹 Leitura IMAP/POP
    @Column(name = "servidor_imap_pop", nullable = false, length = 150)
    private String servidorLeitura;

    @Column(name = "porta_imap_pop", nullable = false)
    private Integer portaLeitura;

    @Column(name = "usar_tls_leitura", nullable = false)
    private Boolean usarTlsLeitura = true;

    @Column(name = "usar_ssl_leitura", nullable = false)
    private Boolean usarSslLeitura = true;

    @Column(name = "protocolo_leitura", nullable = false, length = 10)
    private String protocoloLeitura; // IMAP ou POP3

    // 🔹 Acesso comum
    @Column(name = "usuario_email", nullable = false, length = 150)
    private String usuarioEmail;

    @Column(name = "senha_email", nullable = false, length = 255)
    private String senhaEmail; // 🔒 Guardar criptografado se desejar

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    // ✅ Getters e Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getServidorSmtp() {
        return servidorSmtp;
    }

    public void setServidorSmtp(String servidorSmtp) {
        this.servidorSmtp = servidorSmtp;
    }

    public Integer getPortaSmtp() {
        return portaSmtp;
    }

    public void setPortaSmtp(Integer portaSmtp) {
        this.portaSmtp = portaSmtp;
    }

    public Boolean getUsarTlsSmtp() {
        return usarTlsSmtp;
    }

    public void setUsarTlsSmtp(Boolean usarTlsSmtp) {
        this.usarTlsSmtp = usarTlsSmtp;
    }

    public Boolean getUsarSslSmtp() {
        return usarSslSmtp;
    }

    public void setUsarSslSmtp(Boolean usarSslSmtp) {
        this.usarSslSmtp = usarSslSmtp;
    }

    public String getServidorLeitura() {
        return servidorLeitura;
    }

    public void setServidorLeitura(String servidorLeitura) {
        this.servidorLeitura = servidorLeitura;
    }

    public Integer getPortaLeitura() {
        return portaLeitura;
    }

    public void setPortaLeitura(Integer portaLeitura) {
        this.portaLeitura = portaLeitura;
    }

    public Boolean getUsarTlsLeitura() {
        return usarTlsLeitura;
    }

    public void setUsarTlsLeitura(Boolean usarTlsLeitura) {
        this.usarTlsLeitura = usarTlsLeitura;
    }

    public Boolean getUsarSslLeitura() {
        return usarSslLeitura;
    }

    public void setUsarSslLeitura(Boolean usarSslLeitura) {
        this.usarSslLeitura = usarSslLeitura;
    }

    public String getProtocoloLeitura() {
        return protocoloLeitura;
    }

    public void setProtocoloLeitura(String protocoloLeitura) {
        this.protocoloLeitura = protocoloLeitura;
    }

    public String getUsuarioEmail() {
        return usuarioEmail;
    }

    public void setUsuarioEmail(String usuarioEmail) {
        this.usuarioEmail = usuarioEmail;
    }

    public String getSenhaEmail() {
        return senhaEmail;
    }

    public void setSenhaEmail(String senhaEmail) {
        this.senhaEmail = senhaEmail;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    // ✅ hashCode, equals, toString

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof ConfiguracaoEmail))
            return false;
        ConfiguracaoEmail other = (ConfiguracaoEmail) obj;
        return Objects.equals(id, other.id);
    }

    @Override
    public String toString() {
        return "ConfiguracaoEmail [id=" + id + ", servidorSmtp=" + servidorSmtp + ", portaSmtp=" + portaSmtp
                + ", usarTlsSmtp=" + usarTlsSmtp + ", usarSslSmtp=" + usarSslSmtp + ", servidorLeitura="
                + servidorLeitura + ", portaLeitura=" + portaLeitura + ", usarTlsLeitura=" + usarTlsLeitura
                + ", usarSslLeitura=" + usarSslLeitura + ", protocoloLeitura=" + protocoloLeitura + ", usuarioEmail="
                + usuarioEmail + ", ativo=" + ativo + "]";
    }
}
