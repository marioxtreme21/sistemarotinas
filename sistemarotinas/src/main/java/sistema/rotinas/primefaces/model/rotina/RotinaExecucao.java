package sistema.rotinas.primefaces.model.rotina;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.*;
import sistema.rotinas.primefaces.enums.OrigemExecucaoEnum;
import sistema.rotinas.primefaces.enums.StatusExecucaoEnum;
import sistema.rotinas.primefaces.enums.TipoRotinaEnum;

@Entity
@Table(name = "rotina_execucao")
public class RotinaExecucao implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "execucao_id")
    private Long execucaoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_rotina", nullable = false, length = 20)
    private TipoRotinaEnum tipoRotina;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem_execucao", nullable = false, length = 20)
    private OrigemExecucaoEnum origemExecucao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusExecucaoEnum status = StatusExecucaoEnum.EM_ANDAMENTO;

    @Column(name = "inicio_em", nullable = false)
    private LocalDateTime inicioEm;

    @Column(name = "fim_em")
    private LocalDateTime fimEm;

    @Column(name = "tempo_total_ms")
    private Long tempoTotalMs;

    // Quem executou (login/usuário) - útil pra auditoria (manual)
    @Column(name = "solicitante", length = 120)
    private String solicitante;

    // Resumo numérico
    @Column(name = "total_lojas")
    private Integer totalLojas = 0;

    @Column(name = "lojas_sucesso")
    private Integer lojasSucesso = 0;

    @Column(name = "lojas_falha")
    private Integer lojasFalha = 0;

    @Column(name = "total_arquivos")
    private Integer totalArquivos = 0;

    @Column(name = "arquivos_sucesso")
    private Integer arquivosSucesso = 0;

    @Column(name = "arquivos_falha")
    private Integer arquivosFalha = 0;

    @Column(name = "mensagem_resumo", columnDefinition = "TEXT")
    private String mensagemResumo;

    @Column(name = "erro_geral", columnDefinition = "TEXT")
    private String erroGeral;

    // ========== Getters/Setters ==========

    public Long getExecucaoId() {
        return execucaoId;
    }

    public void setExecucaoId(Long execucaoId) {
        this.execucaoId = execucaoId;
    }

    public TipoRotinaEnum getTipoRotina() {
        return tipoRotina;
    }

    public void setTipoRotina(TipoRotinaEnum tipoRotina) {
        this.tipoRotina = tipoRotina;
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

    public String getSolicitante() {
        return solicitante;
    }

    public void setSolicitante(String solicitante) {
        this.solicitante = solicitante;
    }

    public Integer getTotalLojas() {
        return totalLojas;
    }

    public void setTotalLojas(Integer totalLojas) {
        this.totalLojas = totalLojas;
    }

    public Integer getLojasSucesso() {
        return lojasSucesso;
    }

    public void setLojasSucesso(Integer lojasSucesso) {
        this.lojasSucesso = lojasSucesso;
    }

    public Integer getLojasFalha() {
        return lojasFalha;
    }

    public void setLojasFalha(Integer lojasFalha) {
        this.lojasFalha = lojasFalha;
    }

    public Integer getTotalArquivos() {
        return totalArquivos;
    }

    public void setTotalArquivos(Integer totalArquivos) {
        this.totalArquivos = totalArquivos;
    }

    public Integer getArquivosSucesso() {
        return arquivosSucesso;
    }

    public void setArquivosSucesso(Integer arquivosSucesso) {
        this.arquivosSucesso = arquivosSucesso;
    }

    public Integer getArquivosFalha() {
        return arquivosFalha;
    }

    public void setArquivosFalha(Integer arquivosFalha) {
        this.arquivosFalha = arquivosFalha;
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

    // ========== equals/hashCode/toString ==========

    @Override
    public int hashCode() {
        return Objects.hash(execucaoId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RotinaExecucao)) return false;
        RotinaExecucao other = (RotinaExecucao) obj;
        return Objects.equals(execucaoId, other.execucaoId);
    }

    @Override
    public String toString() {
        return "RotinaExecucao{id=" + execucaoId + ", tipo=" + tipoRotina + ", status=" + status + "}";
    }
}
