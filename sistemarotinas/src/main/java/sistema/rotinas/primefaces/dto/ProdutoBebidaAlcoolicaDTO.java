package sistema.rotinas.primefaces.dto;

import java.io.Serializable;

public class ProdutoBebidaAlcoolicaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codigoProduto;
    private Integer codigoSecao;
    private Integer codigoGrupo;
    private Integer codigoSubGrupo;
    private String descricao;
    private Integer bebidaAlcoolica; // 0/1

    public Long getCodigoProduto() {
        return codigoProduto;
    }

    public void setCodigoProduto(Long codigoProduto) {
        this.codigoProduto = codigoProduto;
    }

    public Integer getCodigoSecao() {
        return codigoSecao;
    }

    public void setCodigoSecao(Integer codigoSecao) {
        this.codigoSecao = codigoSecao;
    }

    public Integer getCodigoGrupo() {
        return codigoGrupo;
    }

    public void setCodigoGrupo(Integer codigoGrupo) {
        this.codigoGrupo = codigoGrupo;
    }

    public Integer getCodigoSubGrupo() {
        return codigoSubGrupo;
    }

    public void setCodigoSubGrupo(Integer codigoSubGrupo) {
        this.codigoSubGrupo = codigoSubGrupo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Integer getBebidaAlcoolica() {
        return bebidaAlcoolica;
    }

    public void setBebidaAlcoolica(Integer bebidaAlcoolica) {
        this.bebidaAlcoolica = bebidaAlcoolica;
    }

    @Override
    public String toString() {
        return "ProdutoBebidaAlcoolicaDTO{" +
                "codigoProduto=" + codigoProduto +
                ", codigoSecao=" + codigoSecao +
                ", codigoGrupo=" + codigoGrupo +
                ", codigoSubGrupo=" + codigoSubGrupo +
                ", descricao='" + descricao + '\'' +
                ", bebidaAlcoolica=" + bebidaAlcoolica +
                '}';
    }
}
