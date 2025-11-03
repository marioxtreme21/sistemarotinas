// src/main/java/sistema/ecommerce/primefaces/service/interfaces/IInventoryUpdateService.java
package sistema.rotinas.primefaces.service.interfaces;

import java.util.List;
import java.util.Optional;

import sistema.rotinas.primefaces.dto.InventoryUpdateRunResult;
import sistema.rotinas.primefaces.model.InventoryUpdate;

public interface IInventoryUpdateService {

	// execução
	void atualizarEstoquePorLoja(Long lojaId); // null = todas ativas

	InventoryUpdateRunResult atualizarEstoquePorLojaComRelatorio(Long lojaId);

	void reprocessarPendentes();

	int[] reprocessarPendentesPorLoja(Long lojaId);

	// consultas auxiliares
	List<InventoryUpdate> listarPendentesEnvio();

	List<InventoryUpdate> listarPendentesEnvioPorLoja(Long lojaId);

	int countPendentesEnvio();

	int countPendentesEnvioPorLoja(Long lojaId);

	Optional<InventoryUpdate> findByLojaAndCodigo(Long lojaId, Integer codigo);

	// CRUD + paginação/pesquisa (espelho)
	List<InventoryUpdate> getAll();

	InventoryUpdate save(InventoryUpdate e);

	InventoryUpdate findById(Long id);

	void deleteById(Long id);

	InventoryUpdate update(InventoryUpdate e);

	List<InventoryUpdate> findAll(int first, int pageSize, String sortField, boolean ascendente);

	int count();

	List<InventoryUpdate> findByCriteria(String campo, String condicao, String valor, int first, int pageSize,
			String sortField, boolean ascendente);

	int countByCriteria(String campo, String condicao, String valor);
}
