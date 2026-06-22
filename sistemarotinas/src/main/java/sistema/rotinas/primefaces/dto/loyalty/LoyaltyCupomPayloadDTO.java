package sistema.rotinas.primefaces.dto.loyalty;

import java.math.BigDecimal;

public record LoyaltyCupomPayloadDTO(
        String dtMovimento,
        Integer idLoja,
        String nomeLoja,
        Integer idPDV,
        String idCliente,
        String categoria,
        Long numCupom,
        BigDecimal vlrVenda,
        Integer qtdProduto,
        String idOperador,
        String canalVenda
) {
}