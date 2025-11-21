package sistema.rotinas.primefaces.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import sistema.rotinas.primefaces.model.ArquivosPrice;

public interface ArquivosPriceRepository extends JpaRepository<ArquivosPrice, Long> {

    Optional<ArquivosPrice> findByLoja_LojaId(Long lojaId);

    boolean existsByLoja_LojaId(Long lojaId);
}
