// src/main/java/sistema/ecommerce/primefaces/vtex/VtexInventoryClientRateLimited.java
package sistema.rotinas.primefaces.vtex;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import com.google.common.util.concurrent.RateLimiter;
import org.slf4j.*;

@Component
@Primary
public class VtexInventoryClientRateLimited implements VtexInventoryClient {
	private static final Logger log = LoggerFactory.getLogger(VtexInventoryClientRateLimited.class);

	private final VtexInventoryClientImpl delegate;
	private final RateLimiter limiter;
	private final int maxTentativas;

	public VtexInventoryClientRateLimited(VtexInventoryClientImpl delegate,
			@Value("${vtex.inventory.rate-limit-per-second:30.0}") double permitsPerSecond,
			@Value("${vtex.inventory.max-retries:5}") int maxTentativas) {
		this.delegate = delegate;
		this.limiter = RateLimiter.create(Math.max(1.0, permitsPerSecond));
		this.maxTentativas = Math.max(1, maxTentativas);
		log.info("[VTEX-INV][rate-limit] {} req/s | maxTentativas={}", permitsPerSecond, this.maxTentativas);
	}

	@Override
	public boolean updateStock(InventoryUpdateContext ctx) throws VtexPricingException {
		int tentativas = 0;
		while (true) {
			limiter.acquire();
			try {
				return delegate.updateStock(ctx);
			} catch (Exception ex) {
				tentativas++;
				if (tentativas > maxTentativas) {
					log.error("[VTEX-INV] Excedeu tentativas sku={} wh={} msg={}", ctx.getSkuId(), ctx.getWarehouse(),
							ex.getMessage());
					if (ex instanceof VtexPricingException vpe)
						throw vpe;
					throw new VtexPricingException("Falha ao enviar estoque: " + ex.getMessage(), ex);
				}
				try {
					Thread.sleep(Math.min(20000, 300 * (1L << Math.min(tentativas - 1, 6))));
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}
}
