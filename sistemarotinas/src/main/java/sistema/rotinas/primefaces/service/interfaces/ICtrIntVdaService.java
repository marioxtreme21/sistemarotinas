package sistema.rotinas.primefaces.service.interfaces;

import java.util.List;

import sistema.rotinas.primefaces.dto.CtrIntVdaDTO;

public interface ICtrIntVdaService {
	/**
	 * Retorna as linhas (tip_int=16) com a coluna derivada TRANSACTION_ID extraída
	 * de JSO_ENV.
	 */
	List<CtrIntVdaDTO> listarComTransactionId();
}
