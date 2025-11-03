package sistema.rotinas.primefaces.service.interfaces;

import java.util.List;

import sistema.rotinas.primefaces.model.Role;
import sistema.rotinas.primefaces.model.User;

public interface IUserService {

    List<User> getAllUsers();

    void save(User user);

    void registerUserWithRole(User user, Role role);

    void registerUser(User user);

    User findById(Long id);

    void updateUser(User user);

    void deleteUserById(Long id);

    void createUser(User user);

    // 🔍 ✅ Novo método para busca por e-mail (case insensitive)
    User findByEmail(String email);

    // ✅ Lazy sem filtro
    List<User> findAllUsuarios(int first, int pageSize, String sortField, boolean ascendente);

    int countUsuarios();

    // ✅ Lazy com filtro flexível
    List<User> findUsuariosByCriteria(String campo, String condicao, String valor,
                                       int first, int pageSize, String sortField, boolean ascendente);

    int countUsuariosByCriteria(String campo, String condicao, String valor);
}
