package sistema.rotinas.primefaces.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import sistema.rotinas.primefaces.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    // 🔍 ✅ Novo método para buscar usuário por e-mail
    Optional<User> findByEmailIgnoreCase(String email);

    // ✅ Usado para remover relações de papéis ao excluir usuário
    @Modifying
    @Query("DELETE FROM User u JOIN u.roles r WHERE u.id = :userId")
    void removeRolesByUserId(@Param("userId") Long userId);
}
