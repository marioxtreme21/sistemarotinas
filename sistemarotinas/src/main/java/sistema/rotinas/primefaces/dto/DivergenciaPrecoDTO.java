package sistema.rotinas.primefaces.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DivergenciaPrecoDTO implements Serializable {
	private static final long serialVersionUID = 1L;

	private Integer loja144;
	private Integer nivel50;
	private String codigoEan;
	private Integer codigoProduto;
	private String descricao;
	private String embalagem;
	private BigDecimal preco144;
	private BigDecimal preco50;
	private BigDecimal diferenca;

	// NOVOS: datas da promoção (144)
	private LocalDateTime dataInicial;
	private LocalDateTime dataFinal;

	// getters/setters
	public Integer getLoja144() {
		return loja144;
	}

	public void setLoja144(Integer loja144) {
		this.loja144 = loja144;
	}

	public Integer getNivel50() {
		return nivel50;
	}

	public void setNivel50(Integer nivel50) {
		this.nivel50 = nivel50;
	}

	public String getCodigoEan() {
		return codigoEan;
	}

	public void setCodigoEan(String codigoEan) {
		this.codigoEan = codigoEan;
	}

	public Integer getCodigoProduto() {
		return codigoProduto;
	}

	public void setCodigoProduto(Integer codigoProduto) {
		this.codigoProduto = codigoProduto;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}

	public String getEmbalagem() {
		return embalagem;
	}

	public void setEmbalagem(String embalagem) {
		this.embalagem = embalagem;
	}

	public BigDecimal getPreco144() {
		return preco144;
	}

	public void setPreco144(BigDecimal preco144) {
		this.preco144 = preco144;
	}

	public BigDecimal getPreco50() {
		return preco50;
	}

	public void setPreco50(BigDecimal preco50) {
		this.preco50 = preco50;
	}

	public BigDecimal getDiferenca() {
		return diferenca;
	}

	public void setDiferenca(BigDecimal diferenca) {
		this.diferenca = diferenca;
	}

	public LocalDateTime getDataInicial() {
		return dataInicial;
	}

	public void setDataInicial(LocalDateTime dataInicial) {
		this.dataInicial = dataInicial;
	}

	public LocalDateTime getDataFinal() {
		return dataFinal;
	}

	public void setDataFinal(LocalDateTime dataFinal) {
		this.dataFinal = dataFinal;
	}
}
