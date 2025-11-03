package sistema.rotinas.primefaces.vtex;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC; // ✅
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Implementação do cliente de Pricing VTEX.
 * Esta versão aceita PriceUpdateContext para logar nome da loja / warehouse.
 */
@Service
public class VtexPricingClientImpl implements VtexPricingClient {

    private static final Logger log = LoggerFactory.getLogger(VtexPricingClientImpl.class);

    // ====== CONFIG (mover para application.properties em produção) ======
    private String baseUrl  = "https://api.vtex.com/hiperideal";
    private String appKey   = "vtexappkey-hiperideal-LXAUFA";
    private String appToken = "TUEYAGTQSUWYYFMYJOZTOWXWCFDUOPVVUEYFKWLHOFBIMPIMBLSQYBBWWHWVCYBDYAEUHGNMCSFIVOJMXRSCWEXTCBJCAQEPDYJLLOFVYGOGVTAHYJXLFZHTDKRFWKMM";
    // ===================================================================

    private final RestTemplate rest = new RestTemplate();
    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    /* ========================= NOVA ASSINATURA (com contexto) ========================= */

    @Override
    public boolean upsertFixedPrice(PriceUpdateContext ctx) throws VtexPricingException {
        final String cid = UUID.randomUUID().toString(); // correlation id por SKU
        final Instant t0 = Instant.now();

        Long skuId = ctx.getSkuId();
        Integer tradePolicyId = ctx.getTradePolicyId();
        BigDecimal listPrice = ctx.getListPrice();
        BigDecimal value = ctx.getValue();
        LocalDateTime from = ctx.getFrom();

        // 1) Tenta POST /fixed/{policy}
        String fixedUrl = baseUrl + "/pricing/prices/" + skuId + "/fixed/" + tradePolicyId;

        if (log.isInfoEnabled()) {
            log.info("[VTEX][{}] Início upsertFixedPrice sku={} policy={} listPrice={} value={} from={} loja='{}' wh='{}' item={}/{} ok={} falha={}",
                    cid, skuId, tradePolicyId, listPrice, value, iso(from), ctx.getNomeLoja(), ctx.getWarehouse(),
                    mdc("batchPos"), mdc("batchTotal"), mdc("okCount"), mdc("failCount"));
        }
        if (log.isDebugEnabled()) {
            log.debug("[VTEX][{}] Endpoint(POST fixed): {}", cid, fixedUrl);
        }

        try {
            FixedPriceBody fixed = FixedPriceBody.of(listPrice, value, iso(from), "2050-12-31T22:00:00-04:00");
            ResponseEntity<String> resp = rest.exchange(
                    fixedUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(List.of(fixed), headers()),
                    String.class
            );

            HttpStatusCode sc = resp.getStatusCode();
            if (is2xx(sc)) {
                long ms = Duration.between(t0, Instant.now()).toMillis();
                log.info("[VTEX][{}] POST /fixed OK (status={}) em {} ms. loja='{}' wh='{}' item={}/{} ok={} falha={} Resp: {}",
                        cid, sc.value(), ms, ctx.getNomeLoja(), ctx.getWarehouse(),
                        mdc("batchPos"), mdc("batchTotal"), mdc("okCount"), mdc("failCount"),
                        truncate(resp.getBody(), 300));
                return true;
            } else {
                log.warn("[VTEX][{}] POST /fixed retornou status {}. Vai tentar fallback PUT. loja='{}' wh='{}' item={}/{} ok={} falha={}",
                        cid, sc.value(), ctx.getNomeLoja(), ctx.getWarehouse(),
                        mdc("batchPos"), mdc("batchTotal"), mdc("okCount"), mdc("failCount"));
            }

        } catch (HttpClientErrorException.NotFound nf) {
            log.warn("[VTEX][{}] POST /fixed 404 (sku={}, policy={}). Tentando criar base price com PUT. loja='{}' wh='{}' item={}/{} ok={} falha={}",
                    cid, skuId, tradePolicyId, ctx.getNomeLoja(), ctx.getWarehouse(),
                    mdc("batchPos"), mdc("batchTotal"), mdc("okCount"), mdc("failCount"));
            // cai para o fallback
        } catch (Exception ex) {
            log.error("[VTEX][{}] Erro no POST /fixed: {}. Vai tentar fallback PUT. loja='{}' wh='{}' item={}/{} ok={} falha={}",
                    cid, ex.getMessage(), ctx.getNomeLoja(), ctx.getWarehouse(),
                    mdc("batchPos"), mdc("batchTotal"), mdc("okCount"), mdc("failCount"), ex);
            // continua para o fallback
        }

        // 2) Fallback: cria/atualiza o preço base + fixed em um único PUT
        boolean ok = createBasePriceWithFixed(cid, ctx,
                "2050-12-31T22:00:00-04:00");

        long ms = Duration.between(t0, Instant.now()).toMillis();
        if (ok) {
            log.info("[VTEX][{}] upsertFixedPrice concluído com SUCESSO (via PUT) em {} ms. loja='{}' wh='{}' item={}/{} ok={} falha={}",
                    cid, ms, ctx.getNomeLoja(), ctx.getWarehouse(),
                    mdc("batchPos"), mdc("batchTotal"), mdc("okCount"), mdc("failCount"));
        } else {
            log.error("[VTEX][{}] upsertFixedPrice FALHOU após {} ms (POST e PUT sem sucesso). loja='{}' wh='{}' item={}/{} ok={} falha={}",
                    cid, ms, ctx.getNomeLoja(), ctx.getWarehouse(),
                    mdc("batchPos"), mdc("batchTotal"), mdc("okCount"), mdc("failCount"));
        }
        return ok;
    }

    /* ========================= ASSINATURA ANTIGA (compat.) ========================= */

    @Override
    public boolean upsertFixedPrice(Long skuId,
                                    Integer tradePolicyId,
                                    BigDecimal listPrice,
                                    BigDecimal value,
                                    LocalDateTime from) throws VtexPricingException {
        PriceUpdateContext ctx = PriceUpdateContext.builder()
                .skuId(skuId)
                .tradePolicyId(tradePolicyId)
                .listPrice(listPrice)
                .value(value)
                .from(from)
                .build();
        return upsertFixedPrice(ctx);
    }

    /* =================================== HELPERS =================================== */

    private boolean createBasePriceWithFixed(String cid,
                                             PriceUpdateContext ctx,
                                             String to) throws VtexPricingException {
        Long skuId = ctx.getSkuId();
        Integer tradePolicyId = ctx.getTradePolicyId();
        BigDecimal listPrice = ctx.getListPrice();
        BigDecimal value = ctx.getValue();
        LocalDateTime from = ctx.getFrom();

        String url = baseUrl + "/pricing/prices/" + skuId;

        if (log.isInfoEnabled()) {
            log.info("[VTEX][{}] Fallback PUT /pricing/prices/{sku} sku={} policy={} listPrice={} value={} from={} to={} loja='{}' wh='{}' item={}/{} ok={} falha={}",
                    cid, skuId, tradePolicyId, listPrice, value, iso(from), to, ctx.getNomeLoja(), ctx.getWarehouse(),
                    mdc("batchPos"), mdc("batchTotal"), mdc("okCount"), mdc("failCount"));
        }
        if (log.isDebugEnabled()) {
            log.debug("[VTEX][{}] Endpoint(PUT base+fixed): {}", cid, url);
        }

        try {
            BasePriceBody body = BasePriceBody.of(listPrice, value, tradePolicyId, iso(from), to);
            ResponseEntity<String> resp = rest.exchange(
                    url,
                    HttpMethod.PUT,
                    new HttpEntity<>(body, headers()),
                    String.class
            );

            HttpStatusCode sc = resp.getStatusCode();
            if (is2xx(sc)) {
                log.info("[VTEX][{}] PUT base+fixed OK (status={}). loja='{}' wh='{}' item={}/{} ok={} falha={} Resp: {}",
                        cid, sc.value(), ctx.getNomeLoja(), ctx.getWarehouse(),
                        mdc("batchPos"), mdc("batchTotal"), mdc("okCount"), mdc("failCount"),
                        truncate(resp.getBody(), 300));
                return true;
            } else {
                log.warn("[VTEX][{}] PUT base+fixed retornou status {}. loja='{}' wh='{}' item={}/{} ok={} falha={} Resp: {}",
                        cid, sc.value(), ctx.getNomeLoja(), ctx.getWarehouse(),
                        mdc("batchPos"), mdc("batchTotal"), mdc("okCount"), mdc("failCount"),
                        truncate(resp.getBody(), 300));
                return false;
            }
        } catch (Exception ex) {
            throw new VtexPricingException("[cid=" + cid + "] Falha no PUT base+fixed: " + ex.getMessage(), ex);
        }
    }

    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setAccept(List.of(MediaType.APPLICATION_JSON));
        // ⚠ Evitar logar estes headers em produção
        h.set("X-VTEX-API-AppKey", appKey);
        h.set("X-VTEX-API-AppToken", appToken);
        return h;
    }

    private String iso(LocalDateTime dt) {
        return dt.atOffset(ZoneOffset.UTC).format(ISO_OFFSET);
    }

    private boolean is2xx(HttpStatusCode status) {
        return status != null && status.is2xxSuccessful();
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max) + "...(truncated)";
    }

    // ✅ Helper para ler valores do MDC (evita NPE)
    private String mdc(String k) {
        String v = MDC.get(k);
        return (v == null || v.isBlank()) ? "-" : v;
    }

    /* ============================== DTOs INTERNOS ============================== */

    /** Corpo para POST /fixed */
    static class FixedPriceBody {
        public BigDecimal value;
        public BigDecimal listPrice;
        public Integer minQuantity;
        public DateRange dateRange;

        static FixedPriceBody of(BigDecimal listPrice, BigDecimal value, String from, String to) {
            FixedPriceBody b = new FixedPriceBody();
            b.value = value;
            b.listPrice = listPrice;
            b.minQuantity = 1;
            b.dateRange = new DateRange(from, to);
            return b;
        }
    }

    /** Corpo para PUT /pricing/prices/{sku} */
    static class BasePriceBody {
        public BigDecimal markup = null;
        public BigDecimal listPrice;
        public BigDecimal basePrice;
        public BigDecimal costPrice = null;
        public List<FixedPriceWithPolicy> fixedPrices;

        static BasePriceBody of(BigDecimal listPrice, BigDecimal value, Integer tradePolicyId, String from, String to) {
            BasePriceBody b = new BasePriceBody();
            b.listPrice = listPrice;
            b.basePrice = listPrice; // base = listPrice
            FixedPriceBody fixed = FixedPriceBody.of(listPrice, value, from, to);
            b.fixedPrices = List.of(new FixedPriceWithPolicy(fixed, String.valueOf(tradePolicyId)));
            return b;
        }
    }

    /** Wrapper para incluir o tradePolicyId no PUT */
    static class FixedPriceWithPolicy extends FixedPriceBody {
        public String tradePolicyId;

        FixedPriceWithPolicy(FixedPriceBody base, String tradePolicyId) {
            this.value = base.value;
            this.listPrice = base.listPrice;
            this.minQuantity = base.minQuantity;
            this.dateRange = base.dateRange;
            this.tradePolicyId = tradePolicyId;
        }
    }

    static class DateRange {
        public String from;
        public String to;

        DateRange() {}
        DateRange(String from, String to) {
            this.from = from;
            this.to = to;
        }
    }
}
