package sistema.rotinas.primefaces.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sistema.rotinas.primefaces.model.Role;
import sistema.rotinas.primefaces.repository.RoleRepository;
import sistema.rotinas.primefaces.service.interfaces.IRoleService;

@Service
public class RoleService implements IRoleService {

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @Override
    public Optional<Role> findByName(String name) {
        return roleRepository.findByName(name);
    }

    @Override
    public Role saveRole(Role role) {
        return roleRepository.save(role);
    }

    @Override
    public Role findRoleById(Long id) {
        return roleRepository.findById(id).orElse(null);
    }
}