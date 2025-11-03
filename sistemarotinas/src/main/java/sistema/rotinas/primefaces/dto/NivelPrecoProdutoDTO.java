package sistema.rotinas.primefaces.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * DTO correspondente ao SELECT atualizado:
 *
 *  select
 *    n.codigo_nivel   as codigo_nivel,
 *    e.codigo_produto as codigo_produto,  -- código interno do produto
 *    n.codigo_produto as codigo_ean,      -- EAN/PLU (preservar zeros à esquerda)
 *    p.descricao      as descricao,
 *    p.pesavel        as pesavel,
 *    p.embalagem      as embalagem,
 *    n.preco          as preco
 *  from nivel_preco_produto n
 *  inner join ean e on (n.codigo_produto = e.codigo_ean)
 *  inner join produto p on (p.codigo_produto = e.codigo_produto)
 *  where n.codigo_nivel = ?
 *  order by p.descricao
 *
 * Observações:
 * - codigoEan e codigoProduto como String (para preservar zeros à esquerda quando houver).
 * - Construtor legado mantido para compatibilidade.
 */
public class NivelPrecoProdutoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer codigoNivel;

    /** e.codigo_produto (código interno do produto) */
    private String  codigoProduto;

    /** n.codigo_produto (EAN/PLU) — manter como String para preservar zeros à esquerda */
    private String  codigoEan;

    /** p.descricao */
    private String  descricao;

    private Boolean pesavel;
    private String  embalagem;
    private BigDecimal preco;

    public NivelPrecoProdutoDTO() {}

    /**
     * Construtor completo (recomendado com o SELECT novo).
     */
    public NivelPrecoProdutoDTO(Integer codigoNivel,
                                String codigoProduto,
                                String codigoEan,
                                String descricao,
                                Boolean pesavel,
                                String embalagem,
                                BigDecimal preco) {
        this.codigoNivel = codigoNivel;
        this.codigoProduto = codigoProduto;
        this.codigoEan = codigoEan;
        this.descricao = descricao;
        this.pesavel = pesavel;
        this.embalagem = embalagem;
        this.preco = preco;
    }

    /**
     * Construtor LEGADO (compatibilidade):
     * interpreta o parametro 'codigoProduto' como EAN/PLU original.
     * Também preenche codigoProduto com o mesmo valor como fallback.
     */
    public NivelPrecoProdutoDTO(Integer codigoNivel,
                                String codigoProduto,
                                Boolean pesavel,
                                String embalagem,
                                BigDecimal preco) {
        this.codigoNivel = codigoNivel;
        this.codigoProduto = codigoProduto; // fallback
        this.codigoEan = codigoProduto;     // legado tratava como EAN
        this.pesavel = pesavel;
        this.embalagem = embalagem;
        this.preco = preco;
    }

    public Integer getCodigoNivel() { return codigoNivel; }
    public void setCodigoNivel(Integer codigoNivel) { this.codigoNivel = codigoNivel; }

    public String getCodigoProduto() { return codigoProduto; }
    public void setCodigoProduto(String codigoProduto) { this.codigoProduto = codigoProduto; }

    public String getCodigoEan() { return codigoEan; }
    public void setCodigoEan(String codigoEan) { this.codigoEan = codigoEan; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public Boolean getPesavel() { return pesavel; }
    public void setPesavel(Boolean pesavel) { this.pesavel = pesavel; }

    public String getEmbalagem() { return embalagem; }
    public void setEmbalagem(String embalagem) { this.embalagem = embalagem; }

    public BigDecimal getPreco() { return preco; }
    public void setPreco(BigDecimal preco) { this.preco = preco; }

    @Override
    public int hashCode() {
        // Identidade preferencial: nivel + EAN; se EAN nulo, cai para nivel + codigoProduto.
        return Objects.hash(codigoNivel, (codigoEan != null ? codigoEan : codigoProduto));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof NivelPrecoProdutoDTO)) return false;
        NivelPrecoProdutoDTO other = (NivelPrecoProdutoDTO) obj;

        String thisId = (this.codigoEan != null ? this.codigoEan : this.codigoProduto);
        String thatId = (other.codigoEan != null ? other.codigoEan : other.codigoProduto);

        return Objects.equals(this.codigoNivel, other.codigoNivel)
            && Objects.equals(thisId, thatId);
    }

    @Override
    public String toString() {
        return "NivelPrecoProdutoDTO{" +
                "codigoNivel=" + codigoNivel +
                ", codigoProduto='" + codigoProduto + '\'' +
                ", codigoEan='" + codigoEan + '\'' +
                ", descricao='" + descricao + '\'' +
                ", pesavel=" + pesavel +
                ", embalagem='" + embalagem + '\'' +
                ", preco=" + preco +
                '}';
    }
}
