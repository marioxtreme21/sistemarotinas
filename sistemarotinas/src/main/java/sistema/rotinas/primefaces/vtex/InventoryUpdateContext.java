// src/main/java/sistema/ecommerce/primefaces/vtex/InventoryUpdateContext.java
package sistema.rotinas.primefaces.vtex;

import java.time.LocalDateTime;

public class InventoryUpdateContext {
	private Long skuId;
	private String warehouse;
	private Integer quantity;
	private boolean unlimitedQuantity = false;
	private LocalDateTime dateUtcOnBalanceSystem = null;

	// enriquecimento p/ logs
	private String nomeLoja;

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private final InventoryUpdateContext ctx = new InventoryUpdateContext();

		public Builder skuId(Long v) {
			ctx.skuId = v;
			return this;
		}

		public Builder warehouse(String v) {
			ctx.warehouse = v;
			return this;
		}

		public Builder quantity(Integer v) {
			ctx.quantity = v;
			return this;
		}

		public Builder unlimitedQuantity(boolean v) {
			ctx.unlimitedQuantity = v;
			return this;
		}

		public Builder dateUtcOnBalanceSystem(LocalDateTime v) {
			ctx.dateUtcOnBalanceSystem = v;
			return this;
		}

		public Builder nomeLoja(String v) {
			ctx.nomeLoja = v;
			return this;
		}

		public InventoryUpdateContext build() {
			return ctx;
		}
	}

	public Long getSkuId() {
		return skuId;
	}

	public String getWarehouse() {
		return warehouse;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public boolean isUnlimitedQuantity() {
		return unlimitedQuantity;
	}

	public LocalDateTime getDateUtcOnBalanceSystem() {
		return dateUtcOnBalanceSystem;
	}

	public String getNomeLoja() {
		return nomeLoja;
	}

	public void setSkuId(Long v) {
		this.skuId = v;
	}

	public void setWarehouse(String v) {
		this.warehouse = v;
	}

	public void setQuantity(Integer v) {
		this.quantity = v;
	}

	public void setUnlimitedQuantity(boolean v) {
		this.unlimitedQuantity = v;
	}

	public void setDateUtcOnBalanceSystem(LocalDateTime v) {
		this.dateUtcOnBalanceSystem = v;
	}

	public void setNomeLoja(String v) {
		this.nomeLoja = v;
	}
}
