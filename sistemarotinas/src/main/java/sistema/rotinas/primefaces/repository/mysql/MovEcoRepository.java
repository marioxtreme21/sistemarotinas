package sistema.rotinas.primefaces.repository.mysql;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import sistema.rotinas.primefaces.dto.PedidoFreteDTO;

public interface MovEcoRepository {
	List<PedidoFreteDTO> listarPedidosComFrete(LocalDate dataInicial, BigDecimal freteMinimo);
}
