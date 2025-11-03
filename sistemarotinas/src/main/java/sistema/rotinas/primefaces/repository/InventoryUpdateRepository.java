// src/main/java/sistema/ecommerce/primefaces/repository/InventoryUpdateRepository.java
package sistema.rotinas.primefaces.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import sistema.rotinas.primefaces.model.InventoryUpdate;

public interface InventoryUpdateRepository extends JpaRepository<InventoryUpdate, Long> {

	Optional<InventoryUpdate> findByLoja_LojaIdAndCodigo(Long lojaId, Integer codigo);

	List<InventoryUpdate> findByStatusEnvioVtexFalseOrStatusEnvioVtexIsNull();

	List<InventoryUpdate> findByLoja_LojaIdAndStatusEnvioVtexFalse(Long lojaId);

	List<InventoryUpdate> findByReprocessamentoFalseOrReprocessamentoIsNull();

	List<InventoryUpdate> findByDataUltimoEnvioAfter(LocalDateTime data);

	int countByLoja_LojaId(Long lojaId);
}
