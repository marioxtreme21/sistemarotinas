package sistema.rotinas.primefaces.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import sistema.rotinas.primefaces.model.Tara;

public interface TaraRepository extends JpaRepository<Tara, Long> {

    // Se precisar de consultas específicas no futuro, adicionamos aqui
}
