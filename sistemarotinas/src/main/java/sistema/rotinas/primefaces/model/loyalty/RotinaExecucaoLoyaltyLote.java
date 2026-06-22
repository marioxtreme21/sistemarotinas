package sistema.rotinas.primefaces.model.loyalty;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import sistema.rotinas.primefaces.enums.StatusExecucaoEnum;
import sistema.rotinas.primefaces.model.Loja;

@Entity
@Table(name = "rotina_execucao_loyalty_lote", indexes = {
        @Index(name = "idx_loyalty_lote_execucao", columnList = "execucao_loyalty_id"),
        @Index(name = "idx_loyalty_lote_loja_data", columnList = "loja_id,data_movimento"),
        @Index(name = "idx_loyalty_lote_status", columnList = "status")
})
public class RotinaExecucaoLoyaltyLote implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lote_id")
    private Long loteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execucao_loyalty_id", nullable = false)
    private RotinaExecucaoLoyalty execucao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loja_id", nullable = false)
    private Loja loja;

    @Column(name = "data_movimento", nullable = false)
    private LocalDate dataMovimento;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StatusExecucaoEnum status;

    @Column(name = "qtd_cupons_consultados", nullable = false)
    private Integer qtdCuponsConsultados = 0;

    @Column(name = "qtd_cupons_enviados", nullable = false)
    private Integer qtdCuponsEnviados = 0;

    @Column(name = "qtd_cupons_falha", nullable = false)
    private Integer qtdCuponsFalha = 0;

    @Column(name = "qtd_pendentes_reprocessamento", nullable = false)
    private Integer qtdPendentesReprocessamento = 0;

    @Column(name = "inicio_em")
    private LocalDateTime inicioEm;

    @Column(name = "fim_em")
    private LocalDateTime fimEm;

    @Column(name = "tempo_total_ms")
    private Long tempoTotalMs;

    @Column(name = "mensagem_resumo", length = 1000)
    private String mensagemResumo;

    @Column(name = "erro_geral", length = 2000)
    private String erroGeral;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        normalize();
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        normalize();
        updatedAt = LocalDateTime.now();
    }

    private void normalize() {
        mensagemResumo = trimToNull(mensagemResumo);
        erroGeral = trimToNull(erroGeral);

        if (status == null) status = StatusExecucaoEnum.FALHA;
        if (qtdCuponsConsultados == null) qtdCuponsConsultados = 0;
        if (qtdCuponsEnviados == null) qtdCuponsEnviados = 0;
        if (qtdCuponsFalha == null) qtdCuponsFalha = 0;
        if (qtdPendentesReprocessamento == null) qtdPendentesReprocessamento = 0;
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    public Long getLoteId() {
        return loteId;
    }

    public RotinaExecucaoLoyalty getExecucao() {
        return execucao;
    }

    public void setExecucao(RotinaExecucaoLoyalty execucao) {
        this.execucao = execucao;
    }

    public Loja getLoja() {
        return loja;
    }

    public void setLoja(Loja loja) {
        this.loja = loja;
    }

    public LocalDate getDataMovimento() {
        return dataMovimento;
    }

    public void setDataMovimento(LocalDate dataMovimento) {
        this.dataMovimento = dataMovimento;
    }

    public StatusExecucaoEnum getStatus() {
        return status;
    }

    public void setStatus(StatusExecucaoEnum status) {
        this.status = status;
    }

    public Integer getQtdCuponsConsultados() {
        return qtdCuponsConsultados;
    }

    public void setQtdCuponsConsultados(Integer qtdCuponsConsultados) {
        this.qtdCuponsConsultados = qtdCuponsConsultados;
    }

    public Integer getQtdCuponsEnviados() {
        return qtdCuponsEnviados;
    }

    public void setQtdCuponsEnviados(Integer qtdCuponsEnviados) {
        this.qtdCuponsEnviados = qtdCuponsEnviados;
    }

    public Integer getQtdCuponsFalha() {
        return qtdCuponsFalha;
    }

    public void setQtdCuponsFalha(Integer qtdCuponsFalha) {
        this.qtdCuponsFalha = qtdCuponsFalha;
    }

    public Integer getQtdPendentesReprocessamento() {
        return qtdPendentesReprocessamento;
    }

    public void setQtdPendentesReprocessamento(Integer qtdPendentesReprocessamento) {
        this.qtdPendentesReprocessamento = qtdPendentesReprocessamento;
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

    public String getMensagemResumo() {
        return mensagemResumo;
    }

    public void setMensagemResumo(String mensagemResumo) {
        this.mensagemResumo = mensagemResumo;
    }

    public String getErroGeral() {
        return erroGeral;
    }

    public void setErroGeral(String erroGeral) {
        this.erroGeral = erroGeral;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public int hashCode() {
        return Objects.hash(loteId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RotinaExecucaoLoyaltyLote other)) return false;
        return Objects.equals(loteId, other.loteId);
    }

    @Override
    public String toString() {
        return "RotinaExecucaoLoyaltyLote{" +
                "loteId=" + loteId +
                ", dataMovimento=" + dataMovimento +
                ", lojaId=" + (loja != null ? loja.getLojaId() : null) +
                ", status=" + status +
                ", qtdCuponsConsultados=" + qtdCuponsConsultados +
                ", qtdCuponsEnviados=" + qtdCuponsEnviados +
                ", qtdCuponsFalha=" + qtdCuponsFalha +
                ", qtdPendentesReprocessamento=" + qtdPendentesReprocessamento +
                '}';
    }
}