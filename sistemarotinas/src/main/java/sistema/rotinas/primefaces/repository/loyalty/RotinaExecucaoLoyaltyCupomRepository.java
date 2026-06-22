package sistema.rotinas.primefaces.repository.loyalty;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import sistema.rotinas.primefaces.model.loyalty.RotinaExecucaoLoyaltyCupom;

public interface RotinaExecucaoLoyaltyCupomRepository extends JpaRepository<RotinaExecucaoLoyaltyCupom, Long> {

    @Query("""
        select c
          from RotinaExecucaoLoyaltyCupom c
          join fetch c.execucao
          join fetch c.loja
         where c.reprocessamentoPendente = true
         order by c.dataMovimento asc, c.execucaoLoyaltyCupomId asc
    """)
    List<RotinaExecucaoLoyaltyCupom> findByReprocessamentoPendenteTrueOrderByDataMovimentoAscExecucaoLoyaltyCupomIdAsc();

    @Query("""
        select c
          from RotinaExecucaoLoyaltyCupom c
          join fetch c.execucao
          join fetch c.loja
         where c.reprocessamentoPendente = true
           and c.loja.lojaId in :lojaIds
           and c.dataMovimento between :dataInicial and :dataFinal
         order by c.dataMovimento asc, c.execucaoLoyaltyCupomId asc
    """)
    List<RotinaExecucaoLoyaltyCupom> findByReprocessamentoPendenteTrueAndLoja_LojaIdInAndDataMovimentoBetweenOrderByDataMovimentoAscExecucaoLoyaltyCupomIdAsc(
            List<Long> lojaIds, LocalDate dataInicial, LocalDate dataFinal);
}