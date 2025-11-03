package sistema.rotinas.primefaces.consinco;

import java.util.List;

import sistema.rotinas.primefaces.dto.ProdutoEstoqueDTO;

public interface ViewEcommerceEstoqueReader {
	List<ProdutoEstoqueDTO> listarProdutosComEstoque(Long lojaId, String codLojaEconect, String codLojaRmsDg);

	ProdutoEstoqueDTO buscarEstoqueAtual(Long lojaId, String codLojaEconect, String codLojaRmsDg, Integer codigo);
}
