package sistema.rotinas.primefaces.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sistema.rotinas.primefaces.model.LojaRemoteConfig;

@Repository
public interface LojaRemoteConfigRepository extends JpaRepository<LojaRemoteConfig, Long> {

    // Já existentes
    Optional<LojaRemoteConfig> findByLoja_LojaId(Long lojaId);

    boolean existsByLoja_LojaId(Long lojaId);

    // Suporte à configuração global (padrão do sistema)
    Optional<LojaRemoteConfig> findByGlobalTrue();

    boolean existsByGlobalTrue();
}
