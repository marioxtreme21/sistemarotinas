package sistema.rotinas.primefaces.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sistema.rotinas.primefaces.model.rotina.RotinaExecucao;

@Repository
public interface RotinaExecucaoRepository extends JpaRepository<RotinaExecucao, Long> {
}
