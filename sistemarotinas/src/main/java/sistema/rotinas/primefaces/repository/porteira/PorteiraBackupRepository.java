// FILE: src/main/java/sistema/rotinas/primefaces/repository/porteira/PorteiraBackupRepository.java
package sistema.rotinas.primefaces.repository.porteira;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import sistema.rotinas.primefaces.model.porteira.PorteiraBackup;

public interface PorteiraBackupRepository extends JpaRepository<PorteiraBackup, Long> {

	Optional<PorteiraBackup> findByPorteira_Id(Long porteiraId);

	boolean existsByPorteira_Id(Long porteiraId);

	void deleteByPorteira_Id(Long porteiraId);
}