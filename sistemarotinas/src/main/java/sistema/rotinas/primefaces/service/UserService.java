package sistema.rotinas.primefaces.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import sistema.rotinas.primefaces.model.Role;
import sistema.rotinas.primefaces.model.User;
import sistema.rotinas.primefaces.repository.RoleRepository;
import sistema.rotinas.primefaces.repository.UserRepository;
import sistema.rotinas.primefaces.service.interfaces.IUserService;

@Service
public class UserService implements IUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    @Transactional
    public void save(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setDataCadastro(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void registerUserWithRole(User user, Role role) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRoles(Set.of(role));
        user.setDataCadastro(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public void updateUser(User user) {
        User existingUser = userRepository.findById(user.getId())
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        existingUser.setNome(user.getNome());
        existingUser.setSobrenome(user.getSobrenome());
        existingUser.setUsername(user.getUsername());
        existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
        existingUser.setRoles(new HashSet<>(user.getRoles()));
        existingUser.setLojas(new HashSet<>(user.getLojas()));

        userRepository.saveAndFlush(existingUser);
    }

    @Override
    @Transactional
    public void deleteUserById(Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            user.getRoles().clear();
            userRepository.saveAndFlush(user);
            userRepository.delete(user);
        }
    }

    @Override
    @Transactional
    public void registerUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        Role role = roleRepository.findByName("USER").orElseGet(() -> {
            Role newRole = new Role();
            newRole.setName("USER");
            return roleRepository.save(newRole);
        });
        user.setRoles(Collections.singleton(role));
        user.setDataCadastro(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void createUser(User user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            Role defaultRole = roleRepository.findByName("USER").orElseThrow(() ->
                new RuntimeException("Role padrão não encontrada"));
            user.setRoles(Set.of(defaultRole));
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setDataCadastro(LocalDateTime.now());
        userRepository.save(user);
    }

    // ✅ Lazy sem filtro
    @Override
    @Transactional
    public List<User> findAllUsuarios(int first, int pageSize, String sortField, boolean ascendente) {
        String sql = "SELECT * FROM user";
        if (sortField != null && !sortField.isEmpty()) {
            sql += " ORDER BY " + sortField + (ascendente ? " ASC" : " DESC");
        }

        Query query = entityManager.createNativeQuery(sql, User.class);
        query.setFirstResult(first);
        query.setMaxResults(pageSize);
        return query.getResultList();
    }

    @Override
    @Transactional
    public int countUsuarios() {
        Query query = entityManager.createNativeQuery("SELECT COUNT(*) FROM user");
        return ((Number) query.getSingleResult()).intValue();
    }

    // ✅ Lazy com filtros dinâmicos
    @Override
    @Transactional
    public List<User> findUsuariosByCriteria(String campo, String condicao, String valor, int first, int pageSize, String sortField, boolean ascendente) {
        StringBuilder sql = new StringBuilder("SELECT * FROM user WHERE 1=1");

        if (campo != null && !campo.isEmpty() && valor != null && !valor.isEmpty()) {
            sql.append(" AND ").append(campo);
            sql.append(" ").append(condicao.equals("equal") ? "= :valor" : "LIKE :valor");
        }

        if (sortField != null && !sortField.isEmpty()) {
            sql.append(" ORDER BY ").append(sortField).append(ascendente ? " ASC" : " DESC");
        }

        Query query = entityManager.createNativeQuery(sql.toString(), User.class);

        if (campo != null && !campo.isEmpty() && valor != null && !valor.isEmpty()) {
            query.setParameter("valor", condicao.equals("equal") ? valor : "%" + valor + "%");
        }

        query.setFirstResult(first);
        query.setMaxResults(pageSize);
        return query.getResultList();
    }

    @Override
    @Transactional
    public int countUsuariosByCriteria(String campo, String condicao, String valor) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM user WHERE 1=1");

        if (campo != null && !campo.isEmpty() && valor != null && !valor.isEmpty()) {
            sql.append(" AND ").append(campo);
            sql.append(" ").append(condicao.equals("equal") ? "= :valor" : "LIKE :valor");
        }

        Query query = entityManager.createNativeQuery(sql.toString());

        if (campo != null && !campo.isEmpty() && valor != null && !valor.isEmpty()) {
            query.setParameter("valor", condicao.equals("equal") ? valor : "%" + valor + "%");
        }

        return ((Number) query.getSingleResult()).intValue();
    }
    
    @Override
    @Transactional
    public User findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElse(null);
    }
    
    
}
