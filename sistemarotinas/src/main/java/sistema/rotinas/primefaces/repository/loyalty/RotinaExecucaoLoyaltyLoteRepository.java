package sistema.rotinas.primefaces.repository.loyalty;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import sistema.rotinas.primefaces.model.loyalty.RotinaExecucaoLoyalty;
import sistema.rotinas.primefaces.model.loyalty.RotinaExecucaoLoyaltyLote;

public interface RotinaExecucaoLoyaltyLoteRepository extends JpaRepository<RotinaExecucaoLoyaltyLote, Long> {

    List<RotinaExecucaoLoyaltyLote> findByExecucaoOrderByLoteIdAsc(RotinaExecucaoLoyalty execucao);

    List<RotinaExecucaoLoyaltyLote> findByLoja_LojaIdAndDataMovimentoBetweenOrderByDataMovimentoAsc(
            Long lojaId, LocalDate dataInicial, LocalDate dataFinal);
}