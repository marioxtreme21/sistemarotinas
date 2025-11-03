package sistema.rotinas.primefaces.dto;

import java.io.Serializable;

public class SkuMigracaoDTO implements Serializable {
	private static final long serialVersionUID = 1L;

	private Integer sku; // codigo_produto (MySQL)
	private String skuNovo; // git_cod_item + git_digito (Oracle RMS)
	private String descricao; // descricao (MySQL)

	public SkuMigracaoDTO() {
	}

	public SkuMigracaoDTO(Integer sku, String skuNovo, String descricao) {
		this.sku = sku;
		this.skuNovo = skuNovo;
		this.descricao = descricao;
	}

	public Integer getSku() {
		return sku;
	}

	public void setSku(Integer sku) {
		this.sku = sku;
	}

	public String getSkuNovo() {
		return skuNovo;
	}

	public void setSkuNovo(String skuNovo) {
		this.skuNovo = skuNovo;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}
}
