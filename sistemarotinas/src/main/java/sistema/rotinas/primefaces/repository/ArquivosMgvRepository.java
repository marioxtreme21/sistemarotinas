package sistema.rotinas.primefaces.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import sistema.rotinas.primefaces.model.ArquivosMgv;

public interface ArquivosMgvRepository extends JpaRepository<ArquivosMgv, Long> {

    Optional<ArquivosMgv> findByLoja_LojaId(Long lojaId);

    boolean existsByLoja_LojaId(Long lojaId);
}
