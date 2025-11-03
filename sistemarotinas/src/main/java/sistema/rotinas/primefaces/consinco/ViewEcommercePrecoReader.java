package sistema.rotinas.primefaces.consinco;

import java.util.List;

import sistema.rotinas.primefaces.dto.ProdutoPrecoDTO;

public interface ViewEcommercePrecoReader {
	List<ProdutoPrecoDTO> listarProdutosComPrecosAtivos(Long lojaId, String codLojaEconect, String codLojaRmsDg);

	ProdutoPrecoDTO buscarPrecoAtual(Long lojaId, String codLojaEconect, String codLojaRmsDg, Integer sku);
}
