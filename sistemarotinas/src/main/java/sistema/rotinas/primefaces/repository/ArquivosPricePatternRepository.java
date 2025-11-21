package sistema.rotinas.primefaces.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import sistema.rotinas.primefaces.model.ArquivosPricePattern;

public interface ArquivosPricePatternRepository extends JpaRepository<ArquivosPricePattern, Long> {

    List<ArquivosPricePattern> findByPrice_PriceIdOrderByPatternAsc(Long priceId);

    int countByPrice_PriceIdAndRequiredTrue(Long priceId);
}
