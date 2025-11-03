package sistema.rotinas.primefaces.bean;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import org.primefaces.model.DualListModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import jakarta.inject.Named;
import sistema.rotinas.primefaces.lazy.CarregamentoLazyListForObject;
import sistema.rotinas.primefaces.model.Loja;
import sistema.rotinas.primefaces.model.Role;
import sistema.rotinas.primefaces.model.User;
import sistema.rotinas.primefaces.service.LdapImportService;
import sistema.rotinas.primefaces.service.RoleService;
import sistema.rotinas.primefaces.service.UserService;
import sistema.rotinas.primefaces.service.interfaces.ILojaService;

@Component
@Named
@SessionScoped
public class UserEditBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(UserEditBean.class);

    private User user;
    private Role selectedRole;
    private List<Role> availableRoles;
    private List<User> users;
    private String searchKeyword;
    private boolean isEditing;

    // Campos para pesquisa
    private String campoSelecionado;
    private String condicaoSelecionada;
    private String valorPesquisa;
    private CarregamentoLazyListForObject<User> usuariosLazy;
    private List<SelectItem> camposPesquisa;

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private ILojaService lojaService;

    @Autowired
    private LdapImportService ldapImportService;

    private DualListModel<Loja> lojasDualList;

    @PostConstruct
    public void init() {
        log.info("Inicializando UserEditBean");
        this.availableRoles = roleService.getAllRoles();
        this.user = new User();
        this.isEditing = false;
        this.lojasDualList = new DualListModel<>(lojaService.getAllLojas(), new ArrayList<>());
        carregarUsuariosSobDemanda();

        camposPesquisa = new ArrayList<>();
        camposPesquisa.add(new SelectItem("nome", "Nome"));
        camposPesquisa.add(new SelectItem("username", "Username"));
        camposPesquisa.add(new SelectItem("email", "E-mail")); // ✅ Novo campo
        camposPesquisa.add(new SelectItem("origem", "Origem"));
        camposPesquisa.add(new SelectItem("tipo", "Tipo"));
    }

    public void carregarUsuariosSobDemanda() {
        log.debug("Iniciando carregamento sob demanda de usuários...");
        usuariosLazy = new CarregamentoLazyListForObject<>(
            (first, pageSize) -> {
                List<User> usuarios = userService.findAllUsuarios(first, pageSize, null, true);
                log.debug("Usuários carregados: {}", usuarios.size());
                return usuarios;
            },
            () -> {
                int total = userService.countUsuarios();
                log.debug("Total de usuários: {}", total);
                return total;
            }
        );
    }

    public void pesquisar() {
        log.info("Realizando pesquisa - campo: {}, condição: {}, valor: {}", campoSelecionado, condicaoSelecionada, valorPesquisa);
        usuariosLazy = new CarregamentoLazyListForObject<>(
            (first, pageSize) -> {
                List<User> usuarios = userService.findUsuariosByCriteria(campoSelecionado, condicaoSelecionada, valorPesquisa, first, pageSize, null, true);
                log.debug("Usuários encontrados: {}", usuarios.size());
                return usuarios;
            },
            () -> {
                int total = userService.countUsuariosByCriteria(campoSelecionado, condicaoSelecionada, valorPesquisa);
                log.debug("Total de usuários encontrados: {}", total);
                return total;
            }
        );
    }

    public void saveUser() {
    	System.out.println("Acessou metodo saveUser");
        if (isEditing) {
            updateUser();
        } else {
            createUser();
        }
    }

    public void loadAllUsers() {
        this.users = userService.getAllUsers();
        
    }

    public void loadUsers() {
        this.users = userService.getAllUsers();
    }

    public void searchUsers() {
        if (searchKeyword != null && !searchKeyword.isEmpty()) {
            users = userService.getAllUsers().stream()
                    .filter(u -> u.getNome().toLowerCase().contains(searchKeyword.toLowerCase()))
                    .collect(Collectors.toList());
        } else {
            loadAllUsers();
        }
    }

    public void prepareCreateUser() {
        this.user = new User();
        this.selectedRole = null;
        this.isEditing = false;
        this.lojasDualList = new DualListModel<>(lojaService.getAllLojas(), new ArrayList<>());
    }
    
    

    public void loadUser(Long userId) {
        this.user = userService.findById(userId);
        if (this.user != null) {
            // Protege contra NoSuchElementException caso o set esteja vazio
            this.selectedRole = this.user.getRoles().stream().findFirst().orElse(null);
            this.isEditing = true;

            List<Loja> todas = new ArrayList<>(lojaService.getAllLojas());
            List<Loja> selecionadas = new ArrayList<>(this.user.getLojas());

            todas.removeAll(selecionadas);
            todas.sort(Comparator.comparing(Loja::getCodLojaEconect, Comparator.nullsLast(String::compareTo)));
            selecionadas.sort(Comparator.comparing(Loja::getCodLojaEconect, Comparator.nullsLast(String::compareTo)));
            this.lojasDualList = new DualListModel<>(todas, selecionadas);
        }
    }

    public void createUser() {
        if (isUserDataValid()) {
            user.setRoles(Collections.singleton(selectedRole));
            user.setLojas(new HashSet<>(lojasDualList.getTarget()));
            userService.save(user);
            carregarUsuariosSobDemanda();
            resetFields();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Usuário criado com sucesso!", null));
        }
    }

    public void updateUser() {
        if (isUserDataValid()) {
            user.setRoles(Collections.singleton(selectedRole));
            user.setLojas(new HashSet<>(lojasDualList.getTarget()));
            userService.updateUser(user);
            carregarUsuariosSobDemanda();
            resetFields();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Usuário atualizado com sucesso!", null));
        }
    }

    public void deleteUser(Long userId) {
        if (userId != null) {
            userService.deleteUserById(userId);
            carregarUsuariosSobDemanda();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Usuário excluído com sucesso!", null));
        }
    }

    private boolean isUserDataValid() {
        if (user.getNome() == null || user.getNome().isEmpty() ||
            user.getUsername() == null || user.getUsername().isEmpty() ||
            user.getPassword() == null || user.getPassword().isEmpty() ||
            selectedRole == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Dados incompletos", "Preencha todos os campos obrigatórios."));
            return false;
        }
        return true;
    }

    private void resetFields() {
        this.user = new User();
        this.selectedRole = null;
        this.isEditing = false;
        this.lojasDualList = new DualListModel<>(lojaService.getAllLojas(), new ArrayList<>());
    }

    public void testarConexaoAD() {
    	System.out.println("Acesou o metodo testarConexãoAD");
        try {
           // ldapImportService.testarConexao();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Conexão bem-sucedida com o AD.", null));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro ao conectar no AD: " + e.getMessage(), null));
        }
    }

    public void importarUsuariosAD() {
        try {
            StringBuilder feedback = new StringBuilder();
            ldapImportService.importarUsuariosADComMensagem(feedback);

            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Importação do AD", feedback.toString()));

            carregarUsuariosSobDemanda();
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro na importação", e.getMessage()));
        }
    }

    // Getters e Setters

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Role getSelectedRole() { return selectedRole; }
    public void setSelectedRole(Role selectedRole) { this.selectedRole = selectedRole; }

    public List<Role> getAvailableRoles() { return availableRoles; }
    public void setAvailableRoles(List<Role> availableRoles) { this.availableRoles = availableRoles; }

    public List<User> getUsers() { return users; }
    public void setUsers(List<User> users) { this.users = users; }

    public String getSearchKeyword() { return searchKeyword; }
    public void setSearchKeyword(String searchKeyword) { this.searchKeyword = searchKeyword; }

    public boolean isEditing() { return isEditing; }
    public void setEditing(boolean isEditing) { this.isEditing = isEditing; }

    public DualListModel<Loja> getLojasDualList() { return lojasDualList; }
    public void setLojasDualList(DualListModel<Loja> lojasDualList) { this.lojasDualList = lojasDualList; }

    public CarregamentoLazyListForObject<User> getUsuariosLazy() {
        return usuariosLazy;
    }

    public void setUsuariosLazy(CarregamentoLazyListForObject<User> usuariosLazy) {
        this.usuariosLazy = usuariosLazy;
    }

    public List<SelectItem> getCamposPesquisa() {
        return camposPesquisa;
    }

    public void setCamposPesquisa(List<SelectItem> camposPesquisa) {
        this.camposPesquisa = camposPesquisa;
    }

    public String getCampoSelecionado() {
        return campoSelecionado;
    }

    public void setCampoSelecionado(String campoSelecionado) {
        this.campoSelecionado = campoSelecionado;
    }

    public String getCondicaoSelecionada() {
        return condicaoSelecionada;
    }

    public void setCondicaoSelecionada(String condicaoSelecionada) {
        this.condicaoSelecionada = condicaoSelecionada;
    }

    public String getValorPesquisa() {
        return valorPesquisa;
    }

    public void setValorPesquisa(String valorPesquisa) {
        this.valorPesquisa = valorPesquisa;
    }
}
