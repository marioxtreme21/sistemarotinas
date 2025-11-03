package sistema.rotinas.primefaces.service.interfaces;

import java.util.List;
import java.util.Optional;

import sistema.rotinas.primefaces.model.Role;

public interface IRoleService {

    List<Role> getAllRoles();

    Optional<Role> findByName(String name);

    Role saveRole(Role role);

    Role findRoleById(Long id);
}