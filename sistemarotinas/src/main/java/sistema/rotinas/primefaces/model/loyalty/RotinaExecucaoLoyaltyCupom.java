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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import sistema.rotinas.primefaces.enums.StatusExecucaoEnum;
import sistema.rotinas.primefaces.model.Loja;

@Entity
@Table(name = "rotina_execucao_loyalty_cupom", indexes = {
        @Index(name = "idx_loyalty_cupom_execucao", columnList = "execucao_loyalty_id"),
        @Index(name = "idx_loyalty_cupom_lote", columnList = "lote_id"),
        @Index(name = "idx_loyalty_cupom_loja_data", columnList = "loja_id,data_movimento"),
        @Index(name = "idx_loyalty_cupom_reproc", columnList = "reprocessamento_pendente,status_envio"),
        @Index(name = "idx_loyalty_cupom_negocio", columnList = "data_movimento,id_pdv,num_cupom")
})
public class RotinaExecucaoLoyaltyCupom implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "execucao_loyalty_cupom_id")
    private Long execucaoLoyaltyCupomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execucao_loyalty_id", nullable = false)
    private RotinaExecucaoLoyalty execucao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id", nullable = false)
    private RotinaExecucaoLoyaltyLote lote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loja_id", nullable = false)
    private Loja loja;

    @Column(name = "data_movimento", nullable = false)
    private LocalDate dataMovimento;

    @Column(name = "id_pdv", nullable = false)
    private Integer idPdv;

    @Column(name = "num_cupom", nullable = false)
    private Long numCupom;

    @Column(name = "id_cliente_md5", length = 32)
    private String idClienteMd5;

    @Lob
    @Column(name = "payload_json", columnDefinition = "LONGTEXT")
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_envio", nullable = false, length = 30)
    private StatusExecucaoEnum statusEnvio;

    @Column(name = "reprocessamento_pendente", nullable = false)
    private Boolean reprocessamentoPendente = true;

    @Column(name = "tentativas_envio", nullable = false)
    private Integer tentativasEnvio = 0;

    @Column(name = "data_ultimo_envio")
    private LocalDateTime dataUltimoEnvio;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "mensagem", columnDefinition = "TEXT")
    private String mensagem;

    @Column(name = "erro", columnDefinition = "LONGTEXT")
    private String erro;

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
        idClienteMd5 = trimToNull(idClienteMd5);
        payloadJson = trimToNull(payloadJson);
        mensagem = trimToNull(mensagem);
        erro = trimToNull(erro);

        if (statusEnvio == null) statusEnvio = StatusExecucaoEnum.FALHA;
        if (reprocessamentoPendente == null) reprocessamentoPendente = true;
        if (tentativasEnvio == null) tentativasEnvio = 0;
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    public Long getExecucaoLoyaltyCupomId() {
        return execucaoLoyaltyCupomId;
    }

    public RotinaExecucaoLoyalty getExecucao() {
        return execucao;
    }

    public void setExecucao(RotinaExecucaoLoyalty execucao) {
        this.execucao = execucao;
    }

    public RotinaExecucaoLoyaltyLote getLote() {
        return lote;
    }

    public void setLote(RotinaExecucaoLoyaltyLote lote) {
        this.lote = lote;
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

    public Integer getIdPdv() {
        return idPdv;
    }

    public void setIdPdv(Integer idPdv) {
        this.idPdv = idPdv;
    }

    public Long getNumCupom() {
        return numCupom;
    }

    public void setNumCupom(Long numCupom) {
        this.numCupom = numCupom;
    }

    public String getIdClienteMd5() {
        return idClienteMd5;
    }

    public void setIdClienteMd5(String idClienteMd5) {
        this.idClienteMd5 = idClienteMd5;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public StatusExecucaoEnum getStatusEnvio() {
        return statusEnvio;
    }

    public void setStatusEnvio(StatusExecucaoEnum statusEnvio) {
        this.statusEnvio = statusEnvio;
    }

    public Boolean getReprocessamentoPendente() {
        return reprocessamentoPendente;
    }

    public void setReprocessamentoPendente(Boolean reprocessamentoPendente) {
        this.reprocessamentoPendente = reprocessamentoPendente;
    }

    public Integer getTentativasEnvio() {
        return tentativasEnvio;
    }

    public void setTentativasEnvio(Integer tentativasEnvio) {
        this.tentativasEnvio = tentativasEnvio;
    }

    public LocalDateTime getDataUltimoEnvio() {
        return dataUltimoEnvio;
    }

    public void setDataUltimoEnvio(LocalDateTime dataUltimoEnvio) {
        this.dataUltimoEnvio = dataUltimoEnvio;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(Integer httpStatus) {
        this.httpStatus = httpStatus;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public int hashCode() {
        return Objects.hash(execucaoLoyaltyCupomId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RotinaExecucaoLoyaltyCupom other)) return false;
        return Objects.equals(execucaoLoyaltyCupomId, other.execucaoLoyaltyCupomId);
    }

    @Override
    public String toString() {
        return "RotinaExecucaoLoyaltyCupom{" +
                "execucaoLoyaltyCupomId=" + execucaoLoyaltyCupomId +
                ", dataMovimento=" + dataMovimento +
                ", lojaId=" + (loja != null ? loja.getLojaId() : null) +
                ", idPdv=" + idPdv +
                ", numCupom=" + numCupom +
                ", statusEnvio=" + statusEnvio +
                ", reprocessamentoPendente=" + reprocessamentoPendente +
                ", tentativasEnvio=" + tentativasEnvio +
                '}';
    }
}