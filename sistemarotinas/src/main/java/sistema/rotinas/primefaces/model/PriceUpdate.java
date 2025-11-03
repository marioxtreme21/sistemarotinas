package sistema.rotinas.primefaces.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "price_update")
public class PriceUpdate implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "price_update_id")
    private Long priceUpdateId;

    @Column(name = "codigo", nullable = false)
    private Integer codigo;

    @Column(name = "descricao")
    private String descricao;

    // Valores de preço (mantendo Double como no legado por ora)
    @Column(name = "value")
    private Double value;

    @Column(name = "list_price")
    private Double listPrice;

    @Column(name = "min_quantity")
    private Integer minQuantity;

    // Datas modernas (sem @Temporal)
    @Column(name = "data_inicial")
    private LocalDate dataInicial;

    @Column(name = "data_final")
    private LocalDate dataFinal;

    @Column(name = "data_ultimo_envio")
    private LocalDateTime dataUltimoEnvio;

    @Column(name = "status_envio_vtex")
    private Boolean statusEnvioVtex;

    @Column(name = "reprocessamento")
    private Boolean reprocessamento;

    @ManyToOne
    @JoinColumn(name = "loja_id", referencedColumnName = "loja_id")
    private Loja loja;

    // Getters e Setters
    public Long getPriceUpdateId() {
        return priceUpdateId;
    }

    public void setPriceUpdateId(Long priceUpdateId) {
        this.priceUpdateId = priceUpdateId;
    }

    public Integer getCodigo() {
        return codigo;
    }

    public void setCodigo(Integer codigo) {
        this.codigo = codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public Double getListPrice() {
        return listPrice;
    }

    public void setListPrice(Double listPrice) {
        this.listPrice = listPrice;
    }

    public Integer getMinQuantity() {
        return minQuantity;
    }

    public void setMinQuantity(Integer minQuantity) {
        this.minQuantity = minQuantity;
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

    public LocalDateTime getDataUltimoEnvio() {
        return dataUltimoEnvio;
    }

    public void setDataUltimoEnvio(LocalDateTime dataUltimoEnvio) {
        this.dataUltimoEnvio = dataUltimoEnvio;
    }

    public Boolean getStatusEnvioVtex() {
        return statusEnvioVtex;
    }

    public void setStatusEnvioVtex(Boolean statusEnvioVtex) {
        this.statusEnvioVtex = statusEnvioVtex;
    }

    public Boolean getReprocessamento() {
        return reprocessamento;
    }

    public void setReprocessamento(Boolean reprocessamento) {
        this.reprocessamento = reprocessamento;
    }

    public Loja getLoja() {
        return loja;
    }

    public void setLoja(Loja loja) {
        this.loja = loja;
    }

    // equals/hashCode por id
    @Override
    public int hashCode() {
        return Objects.hash(priceUpdateId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if ((obj == null) || getClass() != obj.getClass()) return false;
        PriceUpdate other = (PriceUpdate) obj;
        return Objects.equals(priceUpdateId, other.priceUpdateId);
    }

    @Override
    public String toString() {
        return "PriceUpdate{" +
                "priceUpdateId=" + priceUpdateId +
                ", codigo=" + codigo +
                ", descricao='" + descricao + '\'' +
                ", value=" + value +
                ", listPrice=" + listPrice +
                ", minQuantity=" + minQuantity +
                ", dataInicial=" + dataInicial +
                ", dataFinal=" + dataFinal +
                ", dataUltimoEnvio=" + dataUltimoEnvio +
                ", statusEnvioVtex=" + statusEnvioVtex +
                ", reprocessamento=" + reprocessamento +
                ", loja=" + (loja != null ? loja.getLojaId() : null) +
                '}';
    }
}
