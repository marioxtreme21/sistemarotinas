package sistema.rotinas.primefaces.service.interfaces;

import java.util.List;

import sistema.rotinas.primefaces.dto.ProdutoEcommerceExternoDTO;

public interface IProdutoEcommerceExternoService {

    /** Leitura simples para inspeção rápida (usaremos esta no primeiro teste de console). */
    List<ProdutoEcommerceExternoDTO> getPrimeiros(int limit);

    /** Paginação padrão do projeto (offset/limit). */
    List<ProdutoEcommerceExternoDTO> findAllProdutosEcommerce(int first, int pageSize, String sortField, boolean ascendente);

    /** Total de registros da view. */
    int countProdutosEcommerce();

    /** Pesquisa por critérios (campo/condição/valor) com paginação. */
    List<ProdutoEcommerceExternoDTO> findProdutosEcommerceByCriteria(String campo, String condicao, String valor,
                                                                     int first, int pageSize, String sortField, boolean ascendente);

    /** Total filtrado para a pesquisa por critérios. */
    int countProdutosEcommerceByCriteria(String campo, String condicao, String valor);

    /** Busca direta pela chave composta típica (LOJA, COD, DG). */
    ProdutoEcommerceExternoDTO findByChave(Integer loja, Integer cod, Integer dg);

    /** Busca por EAN (tratado como string para preservar zeros). */
    List<ProdutoEcommerceExternoDTO> findByEan(String ean);
}
