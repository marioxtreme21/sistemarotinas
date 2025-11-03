package sistema.rotinas.primefaces.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import sistema.rotinas.primefaces.model.ConfiguracaoEmail;

public interface ConfiguracaoEmailRepository extends JpaRepository<ConfiguracaoEmail, Long> {

    Optional<ConfiguracaoEmail> findByAtivoTrue();
}
