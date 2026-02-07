// FILE: src/main/java/sistema/rotinas/primefaces/repository/porteira/PorteiraEletronicaRepository.java
package sistema.rotinas.primefaces.repository.porteira;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import sistema.rotinas.primefaces.model.porteira.PorteiraEletronica;

public interface PorteiraEletronicaRepository extends JpaRepository<PorteiraEletronica, Long> {

	@Query("select p from PorteiraEletronica p where p.executarRotinaDesativacaoAtiva = true")
	List<PorteiraEletronica> buscarPorteirasComRotinaAtiva();

	@Query("select p.senhaIntegracao from PorteiraEletronica p where p.id = :id")
	String buscarSenhaPelaId(Long id);
}