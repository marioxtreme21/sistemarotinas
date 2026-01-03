package sistema.rotinas.primefaces.model.rotina;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.*;
import sistema.rotinas.primefaces.enums.StatusExecucaoEnum;
import sistema.rotinas.primefaces.model.Loja;

@Entity
@Table(name = "rotina_execucao_loja")
public class RotinaExecucaoLoja implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "execucao_loja_id")
    private Long execucaoLojaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execucao_id", nullable = false)
    private RotinaExecucao execucao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loja_id", nullable = false)
    private Loja loja;

    @Column(name = "cod_loja_rms", length = 30)
    private String codLojaRms;

    @Column(name = "nome_loja", length = 255)
    private String nomeLoja;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StatusExecucaoEnum status = StatusExecucaoEnum.PENDENTE;

    @Column(name = "mensagem", length = 1000)
    private String mensagem;

    @Column(name = "erro", length = 2000)
    private String erro;

    @Column(name = "inicio_em")
    private LocalDateTime inicioEm;

    @Column(name = "fim_em")
    private LocalDateTime fimEm;

    // ✅ NOVO: usado pelo RotinaExecucaoService.el.setTempoTotalMs(ms)
    @Column(name = "tempo_total_ms")
    private Long tempoTotalMs;

    // ======================
    // Getters / Setters
    // ======================

    public Long getExecucaoLojaId() {
        return execucaoLojaId;
    }

    public void setExecucaoLojaId(Long execucaoLojaId) {
        this.execucaoLojaId = execucaoLojaId;
    }

    public RotinaExecucao getExecucao() {
        return execucao;
    }

    public void setExecucao(RotinaExecucao execucao) {
        this.execucao = execucao;
    }

    public Loja getLoja() {
        return loja;
    }

    public void setLoja(Loja loja) {
        this.loja = loja;
    }

    public String getCodLojaRms() {
        return codLojaRms;
    }

    public void setCodLojaRms(String codLojaRms) {
        this.codLojaRms = codLojaRms;
    }

    public String getNomeLoja() {
        return nomeLoja;
    }

    public void setNomeLoja(String nomeLoja) {
        this.nomeLoja = nomeLoja;
    }

    public StatusExecucaoEnum getStatus() {
        return status;
    }

    public void setStatus(StatusExecucaoEnum status) {
        this.status = status;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getErro() {
        return erro;
    }

    public void setErro(String erro) {
        this.erro = erro;
    }

    public LocalDateTime getInicioEm() {
        return inicioEm;
    }

    public void setInicioEm(LocalDateTime inicioEm) {
        this.inicioEm = inicioEm;
    }

    public LocalDateTime getFimEm() {
        return fimEm;
    }

    public void setFimEm(LocalDateTime fimEm) {
        this.fimEm = fimEm;
    }

    public Long getTempoTotalMs() {
        return tempoTotalMs;
    }

    public void setTempoTotalMs(Long tempoTotalMs) {
        this.tempoTotalMs = tempoTotalMs;
    }

    // ======================
    // equals / hashCode
    // ======================

    @Override
    public int hashCode() {
        return Objects.hash(execucaoLojaId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RotinaExecucaoLoja)) return false;
        RotinaExecucaoLoja that = (RotinaExecucaoLoja) o;
        return Objects.equals(execucaoLojaId, that.execucaoLojaId);
    }

    @Override
    public String toString() {
        return "RotinaExecucaoLoja{execucaoLojaId=" + execucaoLojaId +
                ", execucaoId=" + (execucao != null ? execucao.getExecucaoId() : null) +
                ", lojaId=" + (loja != null ? loja.getLojaId() : null) +
                ", codLojaRms=" + codLojaRms +
                ", status=" + status + "}";
    }
}
