package sistema.rotinas.primefaces.repository;

import java.util.List;
import java.util.LinkedHashMap;

/**
 * Executa SELECT * FROM ctr_int_vda WHERE tip_int = 16 no MySQL (10.1.1.50).
 * Retorna cada linha como LinkedHashMap<Coluna, Valor> preservando a ordem de colunas.
 */
public interface CtrIntVdaRepository {
    List<LinkedHashMap<String, Object>> listarTipInt16();
}
