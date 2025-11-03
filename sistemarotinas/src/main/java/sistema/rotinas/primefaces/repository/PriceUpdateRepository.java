package sistema.rotinas.primefaces.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import sistema.rotinas.primefaces.model.PriceUpdate;

public interface PriceUpdateRepository extends JpaRepository<PriceUpdate, Long> {

    // SKU específico dentro de uma loja
    Optional<PriceUpdate> findByLoja_LojaIdAndCodigo(Long lojaId, Integer codigo);

    // Pendentes de envio (geral)
    List<PriceUpdate> findByStatusEnvioVtexFalseOrStatusEnvioVtexIsNull();

    // Pendentes de envio por loja
    List<PriceUpdate> findByLoja_LojaIdAndStatusEnvioVtexFalse(Long lojaId);

    // Reprocessamento pendente (geral)
    List<PriceUpdate> findByReprocessamentoFalseOrReprocessamentoIsNull();

    // Filtra por data de último envio
    List<PriceUpdate> findByDataUltimoEnvioAfter(LocalDateTime data);

    // Contagem por loja
    int countByLoja_LojaId(Long lojaId);
}
