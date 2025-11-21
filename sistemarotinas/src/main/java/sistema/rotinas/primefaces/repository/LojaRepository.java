package sistema.rotinas.primefaces.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import sistema.rotinas.primefaces.model.Loja;

public interface LojaRepository extends JpaRepository<Loja, Long> {

    Optional<Loja> findByCodigoEmpresaSitef(String codigoEmpresaSitef);

    Optional<Loja> findByNome(String nome);

    Optional<Loja> findByCnpj(String cnpj);

    // ✅ já existia para o Price Update
    List<Loja> findByEcommerceAtivoTrueAndHorarioPriceUpdate(String horarioPriceUpdate);

    // ✅ NOVO: usa o campo cod_loja_econect da sua entidade Loja
    Optional<Loja> findByCodLojaEconect(String codLojaEconect);
}
