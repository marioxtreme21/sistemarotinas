package sistema.rotinas.primefaces.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import sistema.rotinas.primefaces.dto.SkuMigracaoDTO;
import sistema.rotinas.primefaces.repository.ItemRmsRepository;
import sistema.rotinas.primefaces.repository.ProdutoMySqlRepository;
import sistema.rotinas.primefaces.service.interfaces.ISkuMigracaoService;

@Service
public class SkuMigracaoService implements ISkuMigracaoService {

	private final ProdutoMySqlRepository produtoRepo;
	private final ItemRmsRepository itemRmsRepo;

	public SkuMigracaoService(ProdutoMySqlRepository produtoRepo, ItemRmsRepository itemRmsRepo) {
		this.produtoRepo = produtoRepo;
		this.itemRmsRepo = itemRmsRepo;
	}

	@Override
	public List<SkuMigracaoDTO> gerarDadosRelatorio() {
		// 1) MySQL: Sku + Descrição
		List<SkuMigracaoDTO> base = produtoRepo.listarSkuDescricao();

		// 2) Oracle RMS: mapa sku -> skuNovo (git_cod_item + git_digito)
		Map<Integer, String> mapaRms = itemRmsRepo.buscarSkuNovoPorSkus(
				base.stream().map(SkuMigracaoDTO::getSku).filter(Objects::nonNull).collect(Collectors.toList()));

		// 3) Enriquecer
		for (SkuMigracaoDTO dto : base) {
			String skuNovo = (dto.getSku() != null) ? mapaRms.get(dto.getSku()) : null;
			dto.setSkuNovo(skuNovo != null ? skuNovo : "");
		}
		return base;
	}
}
