package sistema.rotinas.primefaces.repository.loyalty;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import sistema.rotinas.primefaces.model.loyalty.RotinaExecucaoLoyalty;

public interface RotinaExecucaoLoyaltyRepository extends JpaRepository<RotinaExecucaoLoyalty, Long> {

    List<RotinaExecucaoLoyalty> findTop50ByOrderByExecucaoLoyaltyIdDesc();
}