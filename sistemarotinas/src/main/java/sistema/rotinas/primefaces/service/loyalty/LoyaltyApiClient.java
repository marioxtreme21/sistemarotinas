package sistema.rotinas.primefaces.service.loyalty;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import sistema.rotinas.primefaces.dto.loyalty.LoyaltyApiResponseDTO;
import sistema.rotinas.primefaces.dto.loyalty.LoyaltyCupomPayloadDTO;

@Service
public class LoyaltyApiClient {

    private static final Logger LOG = LoggerFactory.getLogger("LOYALTY_API");

    private final RestClient restClient;

    @Value("${loyalty.api.base-url:http://loyaltycom-reports.us-east-1.elasticbeanstalk.com}")
    private String baseUrl;

    @Value("${loyalty.api.transacional-cupom-path:/homologos/transacional-cupom}")
    private String transacionalCupomPath;

    @Value("${loyalty.api.bearer-token:}")
    private String bearerToken;

    public LoyaltyApiClient(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    public LoyaltyApiResponseDTO enviarCupom(LoyaltyCupomPayloadDTO payload) {
        if (payload == null) {
            return new LoyaltyApiResponseDTO(false, 0, null, "Payload não informado.");
        }
        return enviarCuponsEmLote(List.of(payload));
    }

    public LoyaltyApiResponseDTO enviarCuponsEmLote(List<LoyaltyCupomPayloadDTO> payloads) {
        String url = baseUrl + transacionalCupomPath;

        if (payloads == null || payloads.isEmpty()) {
            return new LoyaltyApiResponseDTO(false, 0, null, "Lista de payloads vazia.");
        }

        LoyaltyCupomPayloadDTO primeiro = payloads.get(0);
        LoyaltyCupomPayloadDTO ultimo = payloads.get(payloads.size() - 1);
        boolean envioUnitario = payloads.size() == 1;

        try {
            if (envioUnitario) {
                LOG.info("LOYALTY API envio iniciado | url={} dtMovimento={} idLoja={} nomeLoja={} idPdv={} numCupom={} categoria={}",
                        url,
                        primeiro.dtMovimento(),
                        primeiro.idLoja(),
                        primeiro.nomeLoja(),
                        primeiro.idPDV(),
                        primeiro.numCupom(),
                        primeiro.categoria());

                LOG.debug("LOYALTY API payload | payload={}", primeiro);
            } else {
                LOG.debug("LOYALTY API envio em lote iniciado | url={} qtdCupons={} dtMovimento={} idLoja={} nomeLoja={} primeiroCupom={} ultimoCupom={}",
                        url,
                        payloads.size(),
                        primeiro.dtMovimento(),
                        primeiro.idLoja(),
                        primeiro.nomeLoja(),
                        primeiro.numCupom(),
                        ultimo.numCupom());
            }

            ResponseEntity<String> response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, bearerToken)
                    .body(payloads)
                    .retrieve()
                    .toEntity(String.class);

            if (envioUnitario) {
                LOG.info("LOYALTY API envio concluído | status={} idLoja={} idPdv={} numCupom={}",
                        response.getStatusCode().value(),
                        primeiro.idLoja(),
                        primeiro.idPDV(),
                        primeiro.numCupom());

                LOG.debug("LOYALTY API resposta | status={} body={}",
                        response.getStatusCode().value(),
                        response.getBody());
            } else {
                LOG.debug("LOYALTY API envio em lote concluído | status={} qtdCupons={} idLoja={} primeiroCupom={} ultimoCupom={}",
                        response.getStatusCode().value(),
                        payloads.size(),
                        primeiro.idLoja(),
                        primeiro.numCupom(),
                        ultimo.numCupom());
            }

            return new LoyaltyApiResponseDTO(
                    response.getStatusCode().is2xxSuccessful(),
                    response.getStatusCode().value(),
                    response.getBody(),
                    null
            );

        } catch (RestClientResponseException e) {
            LOG.error("LOYALTY API erro HTTP | status={} qtdCupons={} idLoja={} primeiroCupom={} ultimoCupom={} responseBody={}",
                    e.getStatusCode().value(),
                    payloads.size(),
                    primeiro != null ? primeiro.idLoja() : null,
                    primeiro != null ? primeiro.numCupom() : null,
                    ultimo != null ? ultimo.numCupom() : null,
                    e.getResponseBodyAsString());

            return new LoyaltyApiResponseDTO(
                    false,
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString(),
                    e.getMessage()
            );

        } catch (Exception e) {
            LOG.error("LOYALTY API erro geral | qtdCupons={} idLoja={} primeiroCupom={} ultimoCupom={} msg={}",
                    payloads.size(),
                    primeiro != null ? primeiro.idLoja() : null,
                    primeiro != null ? primeiro.numCupom() : null,
                    ultimo != null ? ultimo.numCupom() : null,
                    e.getMessage(),
                    e);

            return new LoyaltyApiResponseDTO(
                    false,
                    0,
                    null,
                    e.getMessage()
            );
        }
    }
}