// FILE: src/main/java/sistema/rotinas/primefaces/repository/porteira/PorteiraBackupUsuarioRepository.java
package sistema.rotinas.primefaces.repository.porteira;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import sistema.rotinas.primefaces.model.porteira.PorteiraBackupUsuario;

public interface PorteiraBackupUsuarioRepository extends JpaRepository<PorteiraBackupUsuario, Long> {

	List<PorteiraBackupUsuario> findByBackup_IdOrderByUserAsc(Long backupId);

	long deleteByBackup_Id(Long backupId);
}