package sistema.rotinas.primefaces.service.interfaces;

import java.time.LocalDateTime;
import java.util.List;

import sistema.rotinas.primefaces.dto.DivergenciaPrecoDTO;

public interface IDivergenciaPrecoService {

    List<DivergenciaPrecoDTO> comparar(int loja144, int nivel50, LocalDateTime referencia);

    default List<DivergenciaPrecoDTO> comparar(int loja144, int nivel50) {
        return comparar(loja144, nivel50, LocalDateTime.now());
    }

    byte[] gerarPlanilhaExcel(List<DivergenciaPrecoDTO> itens);
}
