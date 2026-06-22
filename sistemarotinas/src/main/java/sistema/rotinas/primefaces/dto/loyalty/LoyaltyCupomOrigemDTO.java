package sistema.rotinas.primefaces.dto.loyalty;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LoyaltyCupomOrigemDTO(
        LocalDate dtMovimento,
        Integer idLoja,
        String nomeLoja,
        Integer idPdv,
        Long numCupom,
        String categoria,
        String idCliente,
        String canalVenda,
        BigDecimal vlrVenda,
        String idOperador,
        Integer qtdProduto
) {
}