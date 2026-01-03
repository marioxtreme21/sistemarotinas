package sistema.rotinas.primefaces.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sistema.rotinas.primefaces.model.rotina.RotinaExecucaoArquivo;

@Repository
public interface RotinaExecucaoArquivoRepository extends JpaRepository<RotinaExecucaoArquivo, Long> {

    List<RotinaExecucaoArquivo> findByExecucaoExecucaoIdOrderByExecucaoArquivoIdAsc(Long execucaoId);

    List<RotinaExecucaoArquivo> findByExecucaoLojaExecucaoLojaIdOrderByExecucaoArquivoIdAsc(Long execucaoLojaId);
}
