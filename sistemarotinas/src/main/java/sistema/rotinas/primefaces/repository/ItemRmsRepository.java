package sistema.rotinas.primefaces.repository;

import java.util.List;
import java.util.Map;

/** Busca git_cod_item + git_digito no Oracle RMS para SKUs informados. */
public interface ItemRmsRepository {
    Map<Integer, String> buscarSkuNovoPorSkus(List<Integer> skus);
}
