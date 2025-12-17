package sistema.rotinas.primefaces.service.interfaces;

import java.util.List;

import sistema.rotinas.primefaces.model.Tara;

public interface ITaraService {

    List<Tara> getAllTaras();

    Tara save(Tara tara);

    Tara findById(Long prd);

    void deleteById(Long prd);

    Tara update(Tara tara);

    List<Tara> findAllTaras(int first, int pageSize, String sortField, boolean ascendente);

    int countTaras();

    List< Tara > findTarasByCriteria(String campo, String condicao, String valor,
                                     int first, int pageSize, String sortField, boolean ascendente);

    int countTarasByCriteria(String campo, String condicao, String valor);

    /**
     * Sincroniza a tabela local cad_pso_emb com a tabela cad_pso_emb do servidor 144.
     * - Lê todos os registros locais
     * - Limpa a tabela remota
     * - Reinsere todos os registros locais no 144
     */
    void sincronizarComServidor144();
}
