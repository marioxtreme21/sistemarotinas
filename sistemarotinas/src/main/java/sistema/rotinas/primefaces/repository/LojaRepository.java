package sistema.rotinas.primefaces.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import sistema.rotinas.primefaces.model.Loja;

public interface LojaRepository extends JpaRepository<Loja, Long> {

    Optional<Loja> findByCodigoEmpresaSitef(String codigoEmpresaSitef);

    Optional<Loja> findByNome(String nome);

    Optional<Loja> findByCnpj(String cnpj);

    List<Loja> findByEcommerceAtivoTrueAndHorarioPriceUpdate(String horarioPriceUpdate);

    Optional<Loja> findByCodLojaEconect(String codLojaEconect);

    List<Loja> findByLoyaltyAtivoTrueAndCodLojaEconectIsNotNullOrderByNomeAsc();

    List<Loja> findByLojaIdInAndLoyaltyAtivoTrueAndCodLojaEconectIsNotNullOrderByNomeAsc(List<Long> lojaIds);
}