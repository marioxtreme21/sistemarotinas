// src/main/java/sistema/ecommerce/primefaces/vtex/VtexInventoryClient.java
package sistema.rotinas.primefaces.vtex;

public interface VtexInventoryClient {
	boolean updateStock(InventoryUpdateContext ctx) throws VtexPricingException;

	/** Legado simples */
	default boolean updateStock(Long skuId, String warehouse, Integer quantity) throws VtexPricingException {
		InventoryUpdateContext ctx = InventoryUpdateContext.builder().skuId(skuId).warehouse(warehouse)
				.quantity(quantity).build();
		return updateStock(ctx);
	}
}
