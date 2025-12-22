package sistema.rotinas.primefaces.dto;

import java.math.BigDecimal;

public class DivergenciaPrecoCrmDto {

    private Long loja;
    private Long codigoProduto;
    private String codigoEan;
    private String descricao;
    private BigDecimal precoCrm;
    private BigDecimal precoNormal;

    public Long getLoja() {
        return loja;
    }

    public void setLoja(Long loja) {
        this.loja = loja;
    }

    public Long getCodigoProduto() {
        return codigoProduto;
    }

    public void setCodigoProduto(Long codigoProduto) {
        this.codigoProduto = codigoProduto;
    }

    public String getCodigoEan() {
        return codigoEan;
    }

    public void setCodigoEan(String codigoEan) {
        this.codigoEan = codigoEan;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getPrecoCrm() {
        return precoCrm;
    }

    public void setPrecoCrm(BigDecimal precoCrm) {
        this.precoCrm = precoCrm;
    }

    public BigDecimal getPrecoNormal() {
        return precoNormal;
    }

    public void setPrecoNormal(BigDecimal precoNormal) {
        this.precoNormal = precoNormal;
    }
}
