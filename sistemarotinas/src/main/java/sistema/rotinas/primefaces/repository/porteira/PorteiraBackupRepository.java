package sistema.rotinas.primefaces.repository.porteira;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import sistema.rotinas.primefaces.model.porteira.PorteiraBackup;

public interface PorteiraBackupRepository extends JpaRepository<PorteiraBackup, Long> {

	List<PorteiraBackup> findByPorteira_IdOrderByCriadoEmDesc(Long porteiraId);

	Optional<PorteiraBackup> findTopByPorteira_IdOrderByCriadoEmDesc(Long porteiraId);

	boolean existsByPorteira_Id(Long porteiraId);

	List<PorteiraBackup> findByPorteira_IdAndCriadoEmBefore(Long porteiraId, LocalDateTime criadoEm);

	void deleteByPorteira_Id(Long porteiraId);
}