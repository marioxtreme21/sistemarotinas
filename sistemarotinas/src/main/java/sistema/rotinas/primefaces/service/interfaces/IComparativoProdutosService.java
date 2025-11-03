package sistema.rotinas.primefaces.service.interfaces;

import java.util.List;

import sistema.rotinas.primefaces.dto.ProdutoNivelPrecoDTO;

public interface IComparativoProdutosService {
    /**
     * Retorna itens que estão no host 144 (nivel144) e não estão no host 50 (nivel50),
     * comparando pelo EAN (codigo_ean).
     */
    List<ProdutoNivelPrecoDTO> eansPresentes144Ausentes50(int nivel144, int nivel50);

    /**
     * Gera o XLSX (bytes) do comparativo.
     */
    byte[] gerarPlanilhaExcel(List<ProdutoNivelPrecoDTO> itens);
}
