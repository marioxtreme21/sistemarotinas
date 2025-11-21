package sistema.rotinas.primefaces.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import sistema.rotinas.primefaces.model.ArquivosMgvPattern;

public interface ArquivosMgvPatternRepository extends JpaRepository<ArquivosMgvPattern, Long> {

	List<ArquivosMgvPattern> findByMgv_MgvIdOrderByPatternAsc(Long mgvId);

	int countByMgv_MgvIdAndRequiredTrue(Long mgvId);
}
