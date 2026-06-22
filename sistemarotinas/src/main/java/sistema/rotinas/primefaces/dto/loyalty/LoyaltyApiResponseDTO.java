package sistema.rotinas.primefaces.dto.loyalty;

public record LoyaltyApiResponseDTO(
        boolean sucesso,
        int httpStatus,
        String responseBody,
        String erro
) {
}