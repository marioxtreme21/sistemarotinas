package sistema.rotinas.primefaces.model.loyalty;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import sistema.rotinas.primefaces.enums.OrigemExecucaoEnum;
import sistema.rotinas.primefaces.enums.StatusExecucaoEnum;

@Entity
@Table(name = "rotina_execucao_loyalty", indexes = {
        @Index(name = "idx_loyalty_execucao_inicio", columnList = "inicio_em"),
        @Index(name = "idx_loyalty_execucao_status", columnList = "status"),
        @Index(name = "idx_loyalty_execucao_origem", columnList = "origem_execucao")
})
public class RotinaExecucaoLoyalty implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "execucao_loyalty_id")
    private Long execucaoLoyaltyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem_execucao", nullable = false, length = 30)
    private OrigemExecucaoEnum origemExecucao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StatusExecucaoEnum status;

    @Column(name = "selecionar_todas", nullable = false)
    private Boolean selecionarTodas = false;

    @Column(name = "data_inicial", nullable = false)
    private LocalDate dataInicial;

    @Column(name = "data_final", nullable = false)
    private LocalDate dataFinal;

    @Column(name = "total_lojas", nullable = false)
    private Integer totalLojas = 0;

    @Column(name = "total_lotes", nullable = false)
    private Integer totalLotes = 0;

    @Column(name = "total_cupons_consultados", nullable = false)
    private Integer totalCuponsConsultados = 0;

    @Column(name = "total_cupons_enviados", nullable = false)
    private Integer totalCuponsEnviados = 0;

    @Column(name = "total_cupons_falha", nullable = false)
    private Integer totalCuponsFalha = 0;

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
        if (createdAt == null) {
            createdAt = now;
        }
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

        if (selecionarTodas == null) selecionarTodas = false;
        if (status == null) status = StatusExecucaoEnum.FALHA;
        if (totalLojas == null) totalLojas = 0;
        if (totalLotes == null) totalLotes = 0;
        if (totalCuponsConsultados == null) totalCuponsConsultados = 0;
        if (totalCuponsEnviados == null) totalCuponsEnviados = 0;
        if (totalCuponsFalha == null) totalCuponsFalha = 0;
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    public Long getExecucaoLoyaltyId() {
        return execucaoLoyaltyId;
    }

    public OrigemExecucaoEnum getOrigemExecucao() {
        return origemExecucao;
    }

    public void setOrigemExecucao(OrigemExecucaoEnum origemExecucao) {
        this.origemExecucao = origemExecucao;
    }

    public StatusExecucaoEnum getStatus() {
        return status;
    }

    public void setStatus(StatusExecucaoEnum status) {
        this.status = status;
    }

    public Boolean getSelecionarTodas() {
        return selecionarTodas;
    }

    public void setSelecionarTodas(Boolean selecionarTodas) {
        this.selecionarTodas = selecionarTodas;
    }

    public LocalDate getDataInicial() {
        return dataInicial;
    }

    public void setDataInicial(LocalDate dataInicial) {
        this.dataInicial = dataInicial;
    }

    public LocalDate getDataFinal() {
        return dataFinal;
    }

    public void setDataFinal(LocalDate dataFinal) {
        this.dataFinal = dataFinal;
    }

    public Integer getTotalLojas() {
        return totalLojas;
    }

    public void setTotalLojas(Integer totalLojas) {
        this.totalLojas = totalLojas;
    }

    public Integer getTotalLotes() {
        return totalLotes;
    }

    public void setTotalLotes(Integer totalLotes) {
        this.totalLotes = totalLotes;
    }

    public Integer getTotalCuponsConsultados() {
        return totalCuponsConsultados;
    }

    public void setTotalCuponsConsultados(Integer totalCuponsConsultados) {
        this.totalCuponsConsultados = totalCuponsConsultados;
    }

    public Integer getTotalCuponsEnviados() {
        return totalCuponsEnviados;
    }

    public void setTotalCuponsEnviados(Integer totalCuponsEnviados) {
        this.totalCuponsEnviados = totalCuponsEnviados;
    }

    public Integer getTotalCuponsFalha() {
        return totalCuponsFalha;
    }

    public void setTotalCuponsFalha(Integer totalCuponsFalha) {
        this.totalCuponsFalha = totalCuponsFalha;
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
        return Objects.hash(execucaoLoyaltyId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RotinaExecucaoLoyalty other)) return false;
        return Objects.equals(execucaoLoyaltyId, other.execucaoLoyaltyId);
    }

    @Override
    public String toString() {
        return "RotinaExecucaoLoyalty{" +
                "execucaoLoyaltyId=" + execucaoLoyaltyId +
                ", origemExecucao=" + origemExecucao +
                ", status=" + status +
                ", dataInicial=" + dataInicial +
                ", dataFinal=" + dataFinal +
                ", totalLojas=" + totalLojas +
                ", totalLotes=" + totalLotes +
                ", totalCuponsConsultados=" + totalCuponsConsultados +
                ", totalCuponsEnviados=" + totalCuponsEnviados +
                ", totalCuponsFalha=" + totalCuponsFalha +
                '}';
    }
}