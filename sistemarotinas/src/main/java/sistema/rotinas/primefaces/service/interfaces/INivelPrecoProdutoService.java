package sistema.rotinas.primefaces.service.interfaces;

import java.util.List;

import sistema.rotinas.primefaces.dto.NivelPrecoProdutoDTO;

public interface INivelPrecoProdutoService {

    List<NivelPrecoProdutoDTO> listarPorNivel(int codigoNivel);

    List<NivelPrecoProdutoDTO> listarPorNivel(
            int codigoNivel,
            int first,
            int pageSize,
            String sortField,
            boolean ascendente
    );

    int countPorNivel(int codigoNivel);

    List<String> listarCodigosEan13PorNivel(int codigoNivel);

    // NOVO: SELECT com IN (...) e ORDER BY p.descricao
    List<NivelPrecoProdutoDTO> listarPorNivelComProdutosOrdenadoPorDescricao(
            int codigoNivel, List<Long> codigosProduto
    );
}
