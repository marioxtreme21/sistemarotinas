package sistema.rotinas.primefaces.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class ProdutoPrecoDTO implements Serializable {
	private static final long serialVersionUID = 1L;

	private Integer codigo;
	private String descricao; // <-- precisa existir
	private BigDecimal precoDe;
	private BigDecimal precoPor;
	private boolean ativo;

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
	} // <-- setter

	public BigDecimal getPrecoDe() {
		return precoDe;
	}

	public void setPrecoDe(BigDecimal precoDe) {
		this.precoDe = precoDe;
	}

	public BigDecimal getPrecoPor() {
		return precoPor;
	}

	public void setPrecoPor(BigDecimal precoPor) {
		this.precoPor = precoPor;
	}

	public boolean isAtivo() {
		return ativo;
	}

	public void setAtivo(boolean ativo) {
		this.ativo = ativo;
	}
}
