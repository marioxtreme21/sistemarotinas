package sistema.rotinas.primefaces.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProdutoNivelPrecoDTO {

    private Integer codigoNivel;   // n.codigo_nivel ou p.codigo_loja (144)
    private String codigoEan;      // n.codigo_produto AS codigo_ean / p.codigo_produto
    private Integer codigoProduto; // p.codigo_produto
    private String descricao;      // p.descricao
    private Integer pesavel;       // p.pesavel
    private String embalagem;      // p.embalagem ou e.tipo_embalagem
    private BigDecimal preco;      // n.preco ou p.preco

    // NOVOS (quando origem = promoção 144)
    private LocalDateTime dataInicial; // p.data_inicial
    private LocalDateTime dataFinal;   // p.data_final

    // getters/setters
    public Integer getCodigoNivel() { return codigoNivel; }
    public void setCodigoNivel(Integer codigoNivel) { this.codigoNivel = codigoNivel; }

    public String getCodigoEan() { return codigoEan; }
    public void setCodigoEan(String codigoEan) { this.codigoEan = codigoEan; }

    public Integer getCodigoProduto() { return codigoProduto; }
    public void setCodigoProduto(Integer codigoProduto) { this.codigoProduto = codigoProduto; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public Integer getPesavel() { return pesavel; }
    public void setPesavel(Integer pesavel) { this.pesavel = pesavel; }

    public String getEmbalagem() { return embalagem; }
    public void setEmbalagem(String embalagem) { this.embalagem = embalagem; }

    public BigDecimal getPreco() { return preco; }
    public void setPreco(BigDecimal preco) { this.preco = preco; }

    public LocalDateTime getDataInicial() { return dataInicial; }
    public void setDataInicial(LocalDateTime dataInicial) { this.dataInicial = dataInicial; }

    public LocalDateTime getDataFinal() { return dataFinal; }
    public void setDataFinal(LocalDateTime dataFinal) { this.dataFinal = dataFinal; }
}
