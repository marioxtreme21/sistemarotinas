// src/main/java/sistema/ecommerce/primefaces/vtex/VtexInventoryClientImpl.java
package sistema.rotinas.primefaces.vtex;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class VtexInventoryClientImpl implements VtexInventoryClient {
	private static final Logger log = LoggerFactory.getLogger(VtexInventoryClientImpl.class);

	// mover para properties em produção
	private String baseUrl = "https://hiperideal.vtexcommercestable.com.br";
	private String appKey = "vtexappkey-hiperideal-LXAUFA";
	private String appToken = "TUEYAGTQSUWYYFMYJOZTOWXWCFDUOPVVUEYFKWLHOFBIMPIMBLSQYBBWWHWVCYBDYAEUHGNMCSFIVOJMXRSCWEXTCBJCAQEPDYJLLOFVYGOGVTAHYJXLFZHTDKRFWKMM";

	private final RestTemplate rest = new RestTemplate();
	private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

	@Override
	public boolean updateStock(InventoryUpdateContext ctx) throws VtexPricingException {
		final String cid = UUID.randomUUID().toString();

		String url = baseUrl + "/api/logistics/pvt/inventory/skus/" + ctx.getSkuId() + "/warehouses/"
				+ ctx.getWarehouse();

		if (log.isInfoEnabled()) {
			log.info("[VTEX-INV][{}] PUT estoque sku={} wh={} qty={} loja='{}' item={}/{} ok={} falha={}", cid,
					ctx.getSkuId(), ctx.getWarehouse(), ctx.getQuantity(), ctx.getNomeLoja(), mdc("batchPos"),
					mdc("batchTotal"), mdc("okCount"), mdc("failCount"));
		}
		try {
			InventoryBody body = InventoryBody.of(ctx.isUnlimitedQuantity(),
					ctx.getDateUtcOnBalanceSystem() != null
							? ctx.getDateUtcOnBalanceSystem().atOffset(ZoneOffset.UTC).format(ISO_OFFSET)
							: null,
					ctx.getQuantity());

			ResponseEntity<String> resp = rest.exchange(url, HttpMethod.PUT, new HttpEntity<>(body, headers()),
					String.class);
			if (resp.getStatusCode().is2xxSuccessful()) {
				log.info("[VTEX-INV][{}] OK status={}", cid, resp.getStatusCode().value());
				return true;
			}
			log.warn("[VTEX-INV][{}] status={} body={}", cid, resp.getStatusCode().value(), resp.getBody());
			return false;

		} catch (Exception ex) {
			log.error("[VTEX-INV][{}] ERRO: {}", cid, ex.getMessage(), ex);
			throw new VtexPricingException("Falha no envio de estoque: " + ex.getMessage(), ex);
		}
	}

	private HttpHeaders headers() {
		HttpHeaders h = new HttpHeaders();
		h.setContentType(MediaType.APPLICATION_JSON);
		h.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
		h.set("X-VTEX-API-AppKey", appKey);
		h.set("X-VTEX-API-AppToken", appToken);
		return h;
	}

	private String mdc(String k) {
		String v = MDC.get(k);
		return (v == null || v.isBlank()) ? "-" : v;
	}

	static class InventoryBody {
		public boolean unlimitedQuantity;
		public String dateUtcOnBalanceSystem; // ISO_OFFSET or null
		public Integer quantity;

		static InventoryBody of(boolean unlimited, String dateUtc, Integer qty) {
			InventoryBody b = new InventoryBody();
			b.unlimitedQuantity = unlimited;
			b.dateUtcOnBalanceSystem = dateUtc;
			b.quantity = qty;
			return b;
		}
	}
}
