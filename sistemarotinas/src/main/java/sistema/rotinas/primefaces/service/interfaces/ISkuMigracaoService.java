package sistema.rotinas.primefaces.service.interfaces;

import java.util.List;

import sistema.rotinas.primefaces.dto.SkuMigracaoDTO;

public interface ISkuMigracaoService {
	/** Lista final para o relatório: Sku | Sku Novo | Descrição. */
	List<SkuMigracaoDTO> gerarDadosRelatorio();
}
