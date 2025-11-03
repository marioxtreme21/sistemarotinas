package sistema.rotinas.primefaces.service.interfaces;

import java.util.List;
import java.util.Optional;

import sistema.rotinas.primefaces.dto.PriceUpdateRunResult;
import sistema.rotinas.primefaces.model.PriceUpdate;

public interface IPriceUpdateService {

	// ... (já existentes)

	/** Executa o envio por loja e devolve o resumo da execução. */
	PriceUpdateRunResult atualizarPrecosPorLojaComRelatorio(Long lojaId);

	/** Reprocessa somente pendências da loja e devolve [ok, falha]. */
	int[] reprocessarPendentesPorLoja(Long lojaId);

	// já existentes:
	List<PriceUpdate> getAll();

	PriceUpdate save(PriceUpdate priceUpdate);

	PriceUpdate findById(Long id);

	void deleteById(Long id);

	PriceUpdate update(PriceUpdate priceUpdate);

	List<PriceUpdate> findAll(int first, int pageSize, String sortField, boolean ascendente);

	int count();

	List<PriceUpdate> findByCriteria(String campo, String condicao, String valor, int first, int pageSize,
			String sortField, boolean ascendente);

	int countByCriteria(String campo, String condicao, String valor);

	Optional<PriceUpdate> findByLojaAndCodigo(Long lojaId, Integer codigo);

	List<PriceUpdate> listarPendentesEnvio();

	List<PriceUpdate> listarPendentesEnvioPorLoja(Long lojaId);

	int countPendentesEnvio();

	int countPendentesEnvioPorLoja(Long lojaId);

	void atualizarPrecosSelectHorario(String horarioUpdate);

	void atualizarPrecosPorLoja(Long lojaId); // mantém a assinatura antiga (compatibilidade)

	void reprocessarPendentes(); // mantém a geral (compatibilidade)
}
