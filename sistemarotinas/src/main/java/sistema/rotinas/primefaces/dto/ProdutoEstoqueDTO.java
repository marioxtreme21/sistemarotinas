// src/main/java/sistema/ecommerce/primefaces/dto/ProdutoEstoqueDTO.java
package sistema.rotinas.primefaces.dto;

import java.math.BigDecimal;

public class ProdutoEstoqueDTO {
	private Integer codigo;
	private String descricao;
	private BigDecimal quantidade; // para vir da view como decimal

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

	public BigDecimal getQuantidade() {
		return quantidade;
	}

	public void setQuantidade(BigDecimal quantidade) {
		this.quantidade = quantidade;
	}
}
