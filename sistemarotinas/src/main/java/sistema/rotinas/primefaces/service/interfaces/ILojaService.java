package sistema.rotinas.primefaces.service.interfaces;

import java.util.List;

import sistema.rotinas.primefaces.model.Loja;

public interface ILojaService {

	List<Loja> getAllLojas();

	Loja save(Loja loja);

	Loja findById(Long id);

	void deleteById(Long id);

	Loja update(Loja loja);

	List<Loja> findAllLojas(int first, int pageSize, String sortField, boolean ascendente);

	int countLojas();

	List<Loja> findLojasByCriteria(String campo, String condicao, String valor, int first, int pageSize,
			String sortField, boolean ascendente);

	int countLojasByCriteria(String campo, String condicao, String valor);

	// ✅ Novo método
	List<Loja> getLojasPermitidasDoUsuarioLogado();
}
