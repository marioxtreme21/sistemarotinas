// FILE: src/main/java/sistema/rotinas/primefaces/model/porteira/PorteiraEletronica.java
package sistema.rotinas.primefaces.model.porteira;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

import jakarta.persistence.*;

import sistema.rotinas.primefaces.model.Loja;

@Entity
@Table(name = "porteiraeletronica")
public class PorteiraEletronica implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "descricao", length = 255, nullable = false)
    private String descricao;

    @Column(name = "ip", length = 100, nullable = false)
    private String ip;

    @Column(name = "usuario_integracao", length = 100)
    private String usuarioIntegracao;

    @Column(name = "senha_integracao", length = 255)
    private String senhaIntegracao;

    @Column(name = "executar_rotina_desativacao_ativa")
    private Boolean executarRotinaDesativacaoAtiva;

    @Column(name = "data_inicio")
    private LocalDate dataInicio;

    @Column(name = "data_fim")
    private LocalDate dataFim;

    @Column(name = "hora_inicio")
    private LocalTime horaInicio;

    @Column(name = "hora_fim")
    private LocalTime horaFim;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "loja_id", referencedColumnName = "loja_id", nullable = false)
    private Loja loja;

    // Getters/Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public String getUsuarioIntegracao() { return usuarioIntegracao; }
    public void setUsuarioIntegracao(String usuarioIntegracao) { this.usuarioIntegracao = usuarioIntegracao; }

    public String getSenhaIntegracao() { return senhaIntegracao; }
    public void setSenhaIntegracao(String senhaIntegracao) { this.senhaIntegracao = senhaIntegracao; }

    public Boolean getExecutarRotinaDesativacaoAtiva() { return executarRotinaDesativacaoAtiva; }
    public void setExecutarRotinaDesativacaoAtiva(Boolean executarRotinaDesativacaoAtiva) {
        this.executarRotinaDesativacaoAtiva = executarRotinaDesativacaoAtiva;
    }

    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }

    public LocalDate getDataFim() { return dataFim; }
    public void setDataFim(LocalDate dataFim) { this.dataFim = dataFim; }

    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }

    public LocalTime getHoraFim() { return horaFim; }
    public void setHoraFim(LocalTime horaFim) { this.horaFim = horaFim; }

    public Loja getLoja() { return loja; }
    public void setLoja(Loja loja) { this.loja = loja; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PorteiraEletronica)) return false;
        PorteiraEletronica that = (PorteiraEletronica) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}