package sistema.rotinas.primefaces.vtex;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.google.common.util.concurrent.RateLimiter;

/**
 * Wrapper com RateLimiter e retry/backoff para chamadas VTEX.
 * <p>
 * - @Primary => esta implementação é injetada sempre que pedirem VtexPricingClient. <br>
 * - O delegate é o VtexPricingClientImpl real, injetado explicitamente para evitar ambiguidade. <br>
 * - Respeita Retry-After (429) e aplica backoff exponencial com jitter para 5xx/transientes.
 */
@Component
@Primary
public class VtexPricingClientRateLimited implements VtexPricingClient {

    private static final Logger log = LoggerFactory.getLogger(VtexPricingClientRateLimited.class);

    /** Implementação real (HTTP) — injetada explicitamente para evitar loop. */
    private final VtexPricingClientImpl delegate;

    /** Rate limiter (permits/segundo) — configurável. */
    private final RateLimiter limiter;

    /** Máximo de tentativas para 429/5xx — configurável. */
    private final int maxTentativas;

    public VtexPricingClientRateLimited(
            VtexPricingClientImpl delegate,
            @Value("${vtex.pricing.rate-limit-per-second:30.0}") double permitsPerSecond,
            @Value("${vtex.pricing.max-retries:5}") int maxTentativas) {

        this.delegate = delegate;
        this.limiter = RateLimiter.create(Math.max(1.0, permitsPerSecond));
        this.maxTentativas = Math.max(1, maxTentativas);

        log.info("[VTEX][rate-limit] Ativado: {} req/s | maxTentativas={}", permitsPerSecond, this.maxTentativas);
    }

    /* ========================= NOVA ASSINATURA (Contexto) ========================= */

    @Override
    public boolean upsertFixedPrice(PriceUpdateContext ctx) throws VtexPricingException {
        int tentativas = 0;

        while (true) {
            // Controle de taxa global por JVM/instância
            limiter.acquire(); // bloqueia até ter "permits" suficientes

            try {
                // Chamada delegada para a implementação real
                return delegate.upsertFixedPrice(ctx);

            } catch (HttpClientErrorException.TooManyRequests tmr) {
                // 429 - aplicar Retry-After e tentar novamente (até maxTentativas)
                tentativas++;
                if (tentativas > maxTentativas) {
                    log.warn("[VTEX][429][excedido] sku={} policy={} loja='{}' wh='{}' tentativas={}",
                            safeSku(ctx), ctx.getTradePolicyId(), ctx.getNomeLoja(), ctx.getWarehouse(), tentativas);
                    throw tmr;
                }

                long retryAfterSecs = extrairRetryAfter(tmr.getResponseHeaders());
                long espera = Math.max(1, retryAfterSecs);
                log.debug("[VTEX][429][retry] sku={} policy={} loja='{}' wh='{}' retryAfter={}s tentativa={}/{}",
                        safeSku(ctx), ctx.getTradePolicyId(), ctx.getNomeLoja(), ctx.getWarehouse(),
                        espera, tentativas, maxTentativas);

                sleepSeconds(espera);
                // volta ao loop e tenta de novo

            } catch (HttpServerErrorException ex5xx) {
                // 5xx transitório — aplicar backoff exponencial leve + jitter
                tentativas++;
                if (tentativas > maxTentativas) {
                    log.warn("[VTEX][5xx][excedido] sku={} policy={} loja='{}' wh='{}' tentativas={}",
                            safeSku(ctx), ctx.getTradePolicyId(), ctx.getNomeLoja(), ctx.getWarehouse(), tentativas);
                    throw ex5xx;
                }

                long espera = backoffComJitter(tentativas);
                log.debug("[VTEX][5xx][retry] sku={} policy={} loja='{}' wh='{}' wait={}ms tentativa={}/{} msg={}",
                        safeSku(ctx), ctx.getTradePolicyId(), ctx.getNomeLoja(), ctx.getWarehouse(),
                        espera, tentativas, maxTentativas, ex5xx.getMessage());

                sleepMillis(espera);

            } catch (VtexPricingException vpe) {
                // Erros de negócio/parse/etc: retry não ajuda
                log.error("[VTEX][erro] sku={} policy={} loja='{}' wh='{}' msg={}",
                        safeSku(ctx), ctx.getTradePolicyId(), ctx.getNomeLoja(), ctx.getWarehouse(), vpe.getMessage());
                throw vpe;

            } catch (Exception ex) {
                // Qualquer outro erro inesperado — tentar backoff curto
                tentativas++;
                if (tentativas > maxTentativas) {
                    log.error("[VTEX][erro-inesperado][excedido] sku={} policy={} loja='{}' wh='{}' tentativas={} msg={}",
                            safeSku(ctx), ctx.getTradePolicyId(), ctx.getNomeLoja(), ctx.getWarehouse(),
                            tentativas, ex.getMessage(), ex);
                    throw new VtexPricingException("Falha inesperada ao enviar preço: " + ex.getMessage(), ex);
                }
                long espera = backoffComJitter(tentativas);
                log.debug("[VTEX][erro-inesperado][retry] sku={} policy={} loja='{}' wh='{}' wait={}ms tentativa={}/{} msg={}",
                        safeSku(ctx), ctx.getTradePolicyId(), ctx.getNomeLoja(), ctx.getWarehouse(),
                        espera, tentativas, maxTentativas, ex.getMessage());
                sleepMillis(espera);
            }
        }
    }

    /* ========================= ASSINATURA ANTIGA (Retrocompat.) ========================= */

    @Override
    public boolean upsertFixedPrice(Long skuId,
                                    Integer tradePolicyId,
                                    BigDecimal listPrice,
                                    BigDecimal value,
                                    LocalDateTime startAt) throws VtexPricingException {
        // Monta um contexto mínimo para manter compatibilidade
        PriceUpdateContext ctx = PriceUpdateContext.builder()
                .skuId(skuId)
                .tradePolicyId(tradePolicyId)
                .listPrice(listPrice)
                .value(value)
                .from(startAt)
                // nomeLoja / warehouse podem ser nulos aqui
                .build();

        return upsertFixedPrice(ctx);
    }

    /* =================================== Helpers =================================== */

    private long extrairRetryAfter(HttpHeaders headers) {
        if (headers == null) return 1L;
        String v = headers.getFirst("Retry-After");
        if (v == null) return 1L;
        try {
            return Long.parseLong(v.trim());
        } catch (Exception e) {
            return 1L;
        }
    }

    private void sleepSeconds(long seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private void sleepMillis(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    /** Backoff exponencial com jitter (entre 0.5x e 1.5x), base ~300ms. */
    private long backoffComJitter(int tentativa) {
        long base = 300L * (1L << Math.min(tentativa - 1, 6)); // cresce até ~19.2s
        long jitter = ThreadLocalRandom.current().nextLong(base / 2, (long) (base * 1.5));
        return Math.max(200L, jitter);
    }

    private Long safeSku(PriceUpdateContext ctx) {
        return ctx != null ? ctx.getSkuId() : null;
    }
}
