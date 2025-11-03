package sistema.rotinas.primefaces.vtex;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface VtexPricingClient {

    /**
     * NOVO: sobe preço usando contexto completo (recomendado).
     */
    boolean upsertFixedPrice(PriceUpdateContext ctx) throws VtexPricingException;

    /**
     * LEGADO: assinatura antiga — mantida p/ compat.
     * Implementação padrão: empacota em PriceUpdateContext (sem dados de loja).
     */
    default boolean upsertFixedPrice(Long skuId,
                                     Integer tradePolicyId,
                                     BigDecimal listPrice,
                                     BigDecimal value,
                                     LocalDateTime startAt) throws VtexPricingException {
        PriceUpdateContext ctx = PriceUpdateContext.builder()
                .skuId(skuId)
                .tradePolicyId(tradePolicyId)
                .listPrice(listPrice)
                .value(value)
                .from(startAt)
                .build();
        return upsertFixedPrice(ctx);
    }
}
