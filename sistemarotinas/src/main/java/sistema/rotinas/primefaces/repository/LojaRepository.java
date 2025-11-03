package sistema.rotinas.primefaces.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import sistema.rotinas.primefaces.model.Loja;

public interface LojaRepository extends JpaRepository<Loja, Long> {

	Optional<Loja> findByCodigoEmpresaSitef(String codigoEmpresaSitef);

	Optional<Loja> findByNome(String nome);

	Optional<Loja> findByCnpj(String cnpj);

	// ✅ para buscar lojas ativas por horário, sem SQL nativo
	List<Loja> findByEcommerceAtivoTrueAndHorarioPriceUpdate(String horarioPriceUpdate);
}
