package sistema.rotinas.primefaces.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

public class ProdutoEcommerceExternoDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer loja;
    private Integer cod;
    private Integer dg;
    private String ean;                       // preservar zeros à esquerda
    private String descricao;                 // NOT NULL
    private Integer secao;
    private Integer grupo;
    private Integer sgrupo;
    private BigDecimal estq;
    private String marca;                     // NOT NULL

    private BigDecimal fretePeso;
    private BigDecimal freteAltura;
    private BigDecimal freteLargura;
    private BigDecimal freteComprimento;

    private BigDecimal realPeso;
    private BigDecimal realAltura;
    private BigDecimal realLargura;
    private BigDecimal realComprimento;

    private BigDecimal precoPor;
    private BigDecimal precoDe;

    private Integer pesavel;                  // 0/1
    private Integer fracionado;               // 0/1

    private Integer codigoFabricante;         // NOT NULL
    private String embalagem;                 // NOT NULL (2)
    private BigDecimal quantidadeEmbalagem;   // NUMBER(12,6)
    private Integer multiplicadorUnidade;
    private String descrCompl;                // até 2000
    private String situacao;                  // 1 char

    // Getters/Setters
    public Integer getLoja() { return loja; }
    public void setLoja(Integer loja) { this.loja = loja; }

    public Integer getCod() { return cod; }
    public void setCod(Integer cod) { this.cod = cod; }

    public Integer getDg() { return dg; }
    public void setDg(Integer dg) { this.dg = dg; }

    public String getEan() { return ean; }
    public void setEan(String ean) { this.ean = ean; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public Integer getSecao() { return secao; }
    public void setSecao(Integer secao) { this.secao = secao; }

    public Integer getGrupo() { return grupo; }
    public void setGrupo(Integer grupo) { this.grupo = grupo; }

    public Integer getSgrupo() { return sgrupo; }
    public void setSgrupo(Integer sgrupo) { this.sgrupo = sgrupo; }

    public BigDecimal getEstq() { return estq; }
    public void setEstq(BigDecimal estq) { this.estq = estq; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public BigDecimal getFretePeso() { return fretePeso; }
    public void setFretePeso(BigDecimal fretePeso) { this.fretePeso = fretePeso; }

    public BigDecimal getFreteAltura() { return freteAltura; }
    public void setFreteAltura(BigDecimal freteAltura) { this.freteAltura = freteAltura; }

    public BigDecimal getFreteLargura() { return freteLargura; }
    public void setFreteLargura(BigDecimal freteLargura) { this.freteLargura = freteLargura; }

    public BigDecimal getFreteComprimento() { return freteComprimento; }
    public void setFreteComprimento(BigDecimal freteComprimento) { this.freteComprimento = freteComprimento; }

    public BigDecimal getRealPeso() { return realPeso; }
    public void setRealPeso(BigDecimal realPeso) { this.realPeso = realPeso; }

    public BigDecimal getRealAltura() { return realAltura; }
    public void setRealAltura(BigDecimal realAltura) { this.realAltura = realAltura; }

    public BigDecimal getRealLargura() { return realLargura; }
    public void setRealLargura(BigDecimal realLargura) { this.realLargura = realLargura; }

    public BigDecimal getRealComprimento() { return realComprimento; }
    public void setRealComprimento(BigDecimal realComprimento) { this.realComprimento = realComprimento; }

    public BigDecimal getPrecoPor() { return precoPor; }
    public void setPrecoPor(BigDecimal precoPor) { this.precoPor = precoPor; }

    public BigDecimal getPrecoDe() { return precoDe; }
    public void setPrecoDe(BigDecimal precoDe) { this.precoDe = precoDe; }

    public Integer getPesavel() { return pesavel; }
    public void setPesavel(Integer pesavel) { this.pesavel = pesavel; }

    public Integer getFracionado() { return fracionado; }
    public void setFracionado(Integer fracionado) { this.fracionado = fracionado; }

    public Integer getCodigoFabricante() { return codigoFabricante; }
    public void setCodigoFabricante(Integer codigoFabricante) { this.codigoFabricante = codigoFabricante; }

    public String getEmbalagem() { return embalagem; }
    public void setEmbalagem(String embalagem) { this.embalagem = embalagem; }

    public BigDecimal getQuantidadeEmbalagem() { return quantidadeEmbalagem; }
    public void setQuantidadeEmbalagem(BigDecimal quantidadeEmbalagem) { this.quantidadeEmbalagem = quantidadeEmbalagem; }

    public Integer getMultiplicadorUnidade() { return multiplicadorUnidade; }
    public void setMultiplicadorUnidade(Integer multiplicadorUnidade) { this.multiplicadorUnidade = multiplicadorUnidade; }

    public String getDescrCompl() { return descrCompl; }
    public void setDescrCompl(String descrCompl) { this.descrCompl = descrCompl; }

    public String getSituacao() { return situacao; }
    public void setSituacao(String situacao) { this.situacao = situacao; }

    @Override
    public int hashCode() {
        return Objects.hash(loja, cod, dg);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ProdutoEcommerceExternoDTO other = (ProdutoEcommerceExternoDTO) obj;
        return Objects.equals(loja, other.loja)
            && Objects.equals(cod, other.cod)
            && Objects.equals(dg, other.dg);
    }

    @Override
    public String toString() {
        return "ProdutoEcommerceExternoDTO [loja=" + loja + ", cod=" + cod + ", dg=" + dg +
               ", ean=" + ean + ", descricao=" + descricao + "]";
    }
}
