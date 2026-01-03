package sistema.rotinas.primefaces.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sistema.rotinas.primefaces.model.rotina.RotinaExecucaoLoja;

@Repository
public interface RotinaExecucaoLojaRepository extends JpaRepository<RotinaExecucaoLoja, Long> {

    List<RotinaExecucaoLoja> findByExecucaoExecucaoIdOrderByExecucaoLojaIdAsc(Long execucaoId);
}
