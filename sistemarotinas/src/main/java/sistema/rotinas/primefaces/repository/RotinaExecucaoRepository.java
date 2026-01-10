package sistema.rotinas.primefaces.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import sistema.rotinas.primefaces.enums.TipoRotinaEnum;
import sistema.rotinas.primefaces.model.rotina.RotinaExecucao;

public interface RotinaExecucaoRepository extends JpaRepository<RotinaExecucao, Long> {

    Optional<RotinaExecucao> findTopByTipoRotinaAndInicioEmBetweenOrderByInicioEmDesc(
            TipoRotinaEnum tipoRotina,
            LocalDateTime ini,
            LocalDateTime fim
    );
}