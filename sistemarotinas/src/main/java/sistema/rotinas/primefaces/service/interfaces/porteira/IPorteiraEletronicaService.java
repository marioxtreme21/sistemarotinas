// FILE: src/main/java/sistema/rotinas/primefaces/service/interfaces/porteira/IPorteiraEletronicaService.java
package sistema.rotinas.primefaces.service.interfaces.porteira;

import java.util.List;
import sistema.rotinas.primefaces.model.porteira.PorteiraEletronica;

public interface IPorteiraEletronicaService {

	List<PorteiraEletronica> getAllPorteiras();

	PorteiraEletronica save(PorteiraEletronica porteira);

	PorteiraEletronica findById(Long id);

	void deleteById(Long id);

	PorteiraEletronica update(PorteiraEletronica porteira);

	List<PorteiraEletronica> findAllPorteiras(int first, int pageSize, String sortField, boolean ascendente);

	int countPorteiras();

	List<PorteiraEletronica> findPorteirasByCriteria(String campo, String condicao, String valor, int first,
			int pageSize, String sortField, boolean ascendente);

	int countPorteirasByCriteria(String campo, String condicao, String valor);

	List<PorteiraEletronica> buscarPorteirasComRotinaAtiva();

	String buscarSenhaPelaId(Long id);
}