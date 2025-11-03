package sistema.rotinas.primefaces.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import sistema.rotinas.primefaces.dto.ItemSubstituidoDTO;

@Service
public class PickAndPackApiService {

    private static final String TOKEN_URL = "https://auth.pickingnpacking.com/prod/token";
    private static final String API_URL_TEMPLATE = "https://api.pick-and-pack.com/prod/v1/orders/{orderId}";

    private static final String API_KEY = "a309f57805d630b1d67e5379b59c3be5:d08d40c84cb7243597c166a2ad14ff23c02542c95f6e2cb0f65c1b85bb0d7fddd353a21bffcdca0f6b2181317ec64917252b6ffcfbc48efc918a3197844a0c90";

    private String cachedToken = null;

    private final WebClient webClient = WebClient.builder().build();

    public List<ItemSubstituidoDTO> getItensSubstituidos(String orderId) {
        try {
            // 1️⃣ Garante token inicial
            if (cachedToken == null) {
                cachedToken = obterToken();
            }

            // 2️⃣ Monta URL
            String url = API_URL_TEMPLATE.replace("{orderId}", orderId);

            String response;
            try {
                // Primeira tentativa
                response = chamarApiPickAndPack(url, cachedToken);
            } catch (WebClientResponseException ex) {
                if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED || ex.getStatusCode() == HttpStatus.FORBIDDEN) {
                    System.out.println("⚠️ Token expirado, renovando...");
                    cachedToken = obterToken();
                    // Tenta novamente
                    response = chamarApiPickAndPack(url, cachedToken);
                } else {
                    throw ex;
                }
            }

            // 3️⃣ Processa JSON
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);

            Map<String, JsonNode> pickedItemsMap = new HashMap<>();
            JsonNode pickedItems = root.path("pickedItems");
            if (pickedItems.isArray()) {
                for (JsonNode item : pickedItems) {
                    String sku = item.path("sku").asText();
                    pickedItemsMap.put(sku, item);
                }
            }

            List<ItemSubstituidoDTO> resultado = new ArrayList<>();
            JsonNode replacedItems = root.path("replacedItems");
            if (replacedItems.isArray()) {
                for (JsonNode itemOriginal : replacedItems) {

                    String skuOriginal = itemOriginal.path("sku").asText();
                    String nomeOriginal = itemOriginal.path("itemName").asText();
                    String eanOriginal = itemOriginal.path("ean").asText();
                    Double precoOriginal = itemOriginal.path("price").asDouble() / 100.0;
                    Integer quantidadeOriginal = itemOriginal.path("quantity").asInt();

                    JsonNode replacedByArray = itemOriginal.path("replacedBy");
                    if (replacedByArray.isArray()) {
                        for (JsonNode skuSubNode : replacedByArray) {
                            String skuSubstituido = skuSubNode.asText();
                            JsonNode itemSubstituido = pickedItemsMap.get(skuSubstituido);

                            if (itemSubstituido != null) {
                                ItemSubstituidoDTO dto = new ItemSubstituidoDTO();
                                dto.setSkuOriginal(skuOriginal);
                                dto.setItemNameOriginal(nomeOriginal);
                                dto.setEanOriginal(eanOriginal);
                                dto.setPrecoOriginal(precoOriginal);
                                dto.setQuantidadeOriginal(quantidadeOriginal);

                                dto.setSkuSubstituido(itemSubstituido.path("sku").asText());
                                dto.setItemNameSubstituido(itemSubstituido.path("itemName").asText());
                                dto.setEanSubstituido(itemSubstituido.path("ean").asText());
                                dto.setPrecoSubstituido(itemSubstituido.path("price").asDouble() / 100.0);
                                dto.setQuantidadeSubstituida(itemSubstituido.path("quantity").asInt());

                                resultado.add(dto);
                            } else {
                                System.err.println("[WARN] Não encontrado pickedItem para sku substituído: " + skuSubstituido);
                            }
                        }
                    }
                }
            }

            return resultado;

        } catch (WebClientResponseException ex) {
            System.err.println("Erro HTTP: " + ex.getStatusCode() + " - " + ex.getResponseBodyAsString());
            return new ArrayList<>();
        } catch (Exception ex) {
            ex.printStackTrace();
            return new ArrayList<>();
        }
    }

    private String chamarApiPickAndPack(String url, String token) {
        return webClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    private String obterToken() {
        try {
            RestTemplate restTemplate = new RestTemplate();

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("apiKey", API_KEY);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(
                    TOKEN_URL,
                    requestEntity,
                    Map.class
            );

            Map<String, Object> response = responseEntity.getBody();

            if (response == null || !response.containsKey("data")) {
                throw new RuntimeException("Resposta inválida ao obter token (response vazio ou sem campo 'data')");
            }

            String token = (String) response.get("data");
            if (token == null || token.isEmpty()) {
                throw new RuntimeException("Token retornado vazio ao obter token do PickAndPack");
            }

            System.out.println("✅ Novo token obtido: " + token);
            return token;

        } catch (WebClientResponseException ex) {
            System.err.println("Erro HTTP na API PickAndPack (obterToken): " + ex.getStatusCode() + " - " + ex.getResponseBodyAsString());
            throw new RuntimeException("Falha ao obter token do PickAndPack API");
        } catch (Exception ex) {
            System.err.println("Erro geral ao obter token do PickAndPack: " + ex.getMessage());
            throw new RuntimeException("Falha ao obter token do PickAndPack API");
        }
    }
}
