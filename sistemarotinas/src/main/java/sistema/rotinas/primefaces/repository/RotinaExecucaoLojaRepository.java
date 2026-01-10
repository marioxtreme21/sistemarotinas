package sistema.rotinas.primefaces.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sistema.rotinas.primefaces.enums.TipoRotinaEnum;
import sistema.rotinas.primefaces.model.rotina.RotinaExecucaoLoja;

@Repository
public interface RotinaExecucaoLojaRepository extends JpaRepository<RotinaExecucaoLoja, Long> {

    // ✅ já existente (mantido)
    List<RotinaExecucaoLoja> findByExecucaoExecucaoIdOrderByExecucaoLojaIdAsc(Long execucaoId);

    // ✅ NOVO: último status da loja no dia para um tipo de rotina (PRICE/MGV)
    Optional<RotinaExecucaoLoja>
    findTopByLoja_LojaIdAndExecucao_TipoRotinaAndExecucao_InicioEmBetweenOrderByExecucao_InicioEmDesc(
            Long lojaId,
            TipoRotinaEnum tipoRotina,
            LocalDateTime ini,
            LocalDateTime fim
    );
}