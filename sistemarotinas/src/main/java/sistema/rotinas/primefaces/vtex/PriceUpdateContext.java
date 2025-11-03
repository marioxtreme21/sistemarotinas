package sistema.rotinas.primefaces.vtex;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PriceUpdateContext {

	private Long skuId;
	private Integer tradePolicyId;
	private BigDecimal listPrice;
	private BigDecimal value;
	private LocalDateTime from;

	// enriquecimento p/ logs e métricas
	private String nomeLoja;
	private String politicaComercial;
	private String warehouse;

	public PriceUpdateContext() {
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private final PriceUpdateContext ctx = new PriceUpdateContext();

		public Builder skuId(Long v) {
			ctx.skuId = v;
			return this;
		}

		public Builder tradePolicyId(Integer v) {
			ctx.tradePolicyId = v;
			return this;
		}

		public Builder listPrice(BigDecimal v) {
			ctx.listPrice = v;
			return this;
		}

		public Builder value(BigDecimal v) {
			ctx.value = v;
			return this;
		}

		public Builder from(LocalDateTime v) {
			ctx.from = v;
			return this;
		}

		public Builder nomeLoja(String v) {
			ctx.nomeLoja = v;
			return this;
		}

		public Builder politicaComercial(String v) {
			ctx.politicaComercial = v;
			return this;
		}

		public Builder warehouse(String v) {
			ctx.warehouse = v;
			return this;
		}

		public PriceUpdateContext build() {
			return ctx;
		}
	}

	// getters
	public Long getSkuId() {
		return skuId;
	}

	public Integer getTradePolicyId() {
		return tradePolicyId;
	}

	public BigDecimal getListPrice() {
		return listPrice;
	}

	public BigDecimal getValue() {
		return value;
	}

	public LocalDateTime getFrom() {
		return from;
	}

	public String getNomeLoja() {
		return nomeLoja;
	}

	public String getPoliticaComercial() {
		return politicaComercial;
	}

	public String getWarehouse() {
		return warehouse;
	}

	// setters (opcional, se quiser usar sem builder)
	public void setSkuId(Long skuId) {
		this.skuId = skuId;
	}

	public void setTradePolicyId(Integer tradePolicyId) {
		this.tradePolicyId = tradePolicyId;
	}

	public void setListPrice(BigDecimal listPrice) {
		this.listPrice = listPrice;
	}

	public void setValue(BigDecimal value) {
		this.value = value;
	}

	public void setFrom(LocalDateTime from) {
		this.from = from;
	}

	public void setNomeLoja(String nomeLoja) {
		this.nomeLoja = nomeLoja;
	}

	public void setPoliticaComercial(String politicaComercial) {
		this.politicaComercial = politicaComercial;
	}

	public void setWarehouse(String warehouse) {
		this.warehouse = warehouse;
	}
}
