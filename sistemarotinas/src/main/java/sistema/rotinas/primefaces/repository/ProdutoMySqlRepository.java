package sistema.rotinas.primefaces.repository;

import java.util.List;

import sistema.rotinas.primefaces.dto.SkuMigracaoDTO;

/** Lê SKU e descrição do MySQL (10.1.1.144). */
public interface ProdutoMySqlRepository {
    List<SkuMigracaoDTO> listarSkuDescricao();
}
