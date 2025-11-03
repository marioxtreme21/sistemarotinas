package sistema.rotinas.primefaces.service.interfaces;

import java.time.LocalDate;
import java.util.List;

import sistema.rotinas.primefaces.dto.ResultadoSemPromocaoDTO;

public interface IComparativoSemPromocaoService {

	List<ResultadoSemPromocaoDTO> comparar(int loja144, int nivel50, LocalDate dataReferencia);

	byte[] gerarPlanilhaExcel(List<ResultadoSemPromocaoDTO> itens);
}
