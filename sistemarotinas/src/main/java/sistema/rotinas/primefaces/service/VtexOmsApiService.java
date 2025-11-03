package sistema.rotinas.primefaces.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;

@Service
public class VtexOmsApiService {

    private static final Logger log = LoggerFactory.getLogger(VtexOmsApiService.class);

    private static final String API_URL = "https://hiperideal.vtexcommercestable.com.br/api/oms/pvt/orders";

    private static final String APP_KEY = "vtexappkey-hiperideal-LXAUFA";
    private static final String APP_TOKEN = "TUEYAGTQSUWYYFMYJOZTOWXWCFDUOPVVUEYFKWLHOFBIMPIMBLSQYBBWWHWVCYBDYAEUHGNMCSFIVOJMXRSCWEXTCBJCAQEPDYJLLOFVYGOGVTAHYJXLFZHTDKRFWKMM";

    private final RestTemplate restTemplate = new RestTemplate();

    public String obterOrderIdPorSequence(String sequence) {
        try {
            // Montar a URL com parâmetro sequence
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(API_URL)
                    .queryParam("sequence", sequence);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-VTEX-API-AppKey", APP_KEY);
            headers.set("X-VTEX-API-AppToken", APP_TOKEN);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    uriBuilder.toUriString(),
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // Parsear o JSON manualmente para pegar o orderId (usando Jackson simples)
                com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readTree(response.getBody());

                com.fasterxml.jackson.databind.JsonNode listNode = root.path("list");

                if (listNode.isArray() && listNode.size() > 0) {
                    String orderId = listNode.get(0).path("orderId").asText();
                    log.info("✅ OrderId encontrado para sequence {}: {}", sequence, orderId);
                    return orderId;
                } else {
                    log.warn("⚠️ Nenhum resultado na lista para sequence {}", sequence);
                }
            } else {
                log.error("❌ Erro ao consultar API VTEX para sequence {} - status {}", sequence, response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("❌ Exceção ao consultar API VTEX para sequence {}: {}", sequence, e.getMessage(), e);
        }

        return null;
    }
}
