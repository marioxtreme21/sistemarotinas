package sistema.rotinas.primefaces.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sistema.rotinas.primefaces.model.rotina.RotinaExecucaoArquivoEtapa;

@Repository
public interface RotinaExecucaoArquivoEtapaRepository extends JpaRepository<RotinaExecucaoArquivoEtapa, Long> {

    List<RotinaExecucaoArquivoEtapa> findByExecucaoArquivoExecucaoArquivoIdOrderByEtapaIdAsc(Long execucaoArquivoId);
}
