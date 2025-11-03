package sistema.rotinas.primefaces.service;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.control.PagedResultsCookie;
import org.springframework.ldap.control.PagedResultsDirContextProcessor;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Service;

import sistema.rotinas.primefaces.model.Role;
import sistema.rotinas.primefaces.model.User;
import sistema.rotinas.primefaces.repository.RoleRepository;
import sistema.rotinas.primefaces.repository.UserRepository;

import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LdapImportService {

    private static final Logger log = LoggerFactory.getLogger(LdapImportService.class);

    @Autowired
    private LdapTemplate ldapTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserService userService;

    @PostConstruct
    public void configurarLdapTemplate() {
        ldapTemplate.setIgnorePartialResultException(true);
    }

    @Transactional
    public int importarUsuariosADComMensagem(StringBuilder feedback) {
        log.info("🔍 Iniciando importacao de usuarios do Active Directory...");

        List<User> usuariosLdap;

        try {
            usuariosLdap = buscarUsuariosComPaginacao();
        } catch (Exception e) {
            log.error("❌ Erro ao buscar usuarios do AD: {}", e.getMessage(), e);
            feedback.append("Erro ao consultar o Active Directory: ").append(e.getMessage());
            return 0;
        }

        log.info("📋 Total de registros encontrados no AD: {}", usuariosLdap.size());

        int novos = 0;
        int atualizados = 0;
        int existentes = 0;
        int semEmail = 0;

        Set<String> usernamesProcessados = new HashSet<>();
        List<String> usuariosSemEmailRelevantes = new ArrayList<>();

        Role rolePadrao = roleRepository.findByName("USER").orElseGet(() -> {
            Role r = new Role();
            r.setName("USER");
            return roleRepository.save(r);
        });

        for (User ldapUser : usuariosLdap) {
            String username = ldapUser.getUsername();
            String nome = ldapUser.getNome();
            String email = ldapUser.getEmail();

            if (usernamesProcessados.contains(username)) {
                log.warn("⚠️ Usuario '{}' ja processado, ignorando duplicado", username);
                continue;
            }
            usernamesProcessados.add(username);

            if (isBlank(username) || isBlank(nome)) {
                log.warn("❌ Ignorado: username ou nome nulo - username='{}', nome='{}'", username, nome);
                continue;
            }

            if (isBlank(email)) {
                log.warn("⚠️ Ignorado: usuario '{}' sem e-mail", username);
                if (username.contains(".") && !username.endsWith("$")) {
                    usuariosSemEmailRelevantes.add(username);
                }
                semEmail++;
                continue;
            }

            try {
                Optional<User> existenteOpt = userRepository.findByUsername(username);
                if (existenteOpt.isEmpty()) {
                    ldapUser.setRoles(Set.of(rolePadrao));
                    userService.createUser(ldapUser);
                    log.info("✅ Importado: {} - {}", username, email);
                    novos++;
                } else {
                    User existente = existenteOpt.get();
                    boolean precisaAtualizar = false;

                    if (!Objects.equals(existente.getNome(), nome)) {
                        existente.setNome(nome);
                        precisaAtualizar = true;
                    }
                    if (!Objects.equals(existente.getSobrenome(), ldapUser.getSobrenome())) {
                        existente.setSobrenome(ldapUser.getSobrenome());
                        precisaAtualizar = true;
                    }
                    if (!Objects.equals(existente.getEmail(), email)) {
                        existente.setEmail(email);
                        precisaAtualizar = true;
                    }

                    if (precisaAtualizar) {
                        userService.updateUser(existente);
                        log.info("🔁 Atualizado: {}", username);
                        atualizados++;
                    } else {
                        log.info("ℹ️ Ja existente (sem alteracoes): {}", username);
                        existentes++;
                    }
                }

                if ((novos + atualizados + existentes) % 100 == 0) {
                    log.info("⏳ Progresso parcial: {} processados...", novos + atualizados + existentes);
                }

            } catch (Exception e) {
                log.error("❌ Erro ao processar usuario '{}': {}", username, e.getMessage(), e);
            }
        }

        log.info("✅ Novos: {}", novos);
        log.info("🔁 Atualizados: {}", atualizados);
        log.info("ℹ️ Existentes inalterados: {}", existentes);
        log.info("⚠️ Ignorados (sem e-mail): {}", semEmail);

        feedback.append("Importacao concluida:\n")
                .append("✔️ Novos: ").append(novos).append("\n")
                .append("🔁 Atualizados: ").append(atualizados).append("\n")
                .append("ℹ️ Ja existentes: ").append(existentes).append("\n")
                .append("⚠️ Ignorados (sem e-mail): ").append(semEmail);

        // ✅ Log final com usuarios sem email (com ponto no username), ordenados
        usuariosSemEmailRelevantes.stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(u -> log.info("🔧 Usuario '{}' precisa de e-mail no AD", u));

        return novos;
    }

    private List<User> buscarUsuariosComPaginacao() {
        List<User> usuarios = new ArrayList<>();
        int pageSize = 1000;
        String filtro = "(&(objectClass=user)(!(sAMAccountName=*$)))";

        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        PagedResultsCookie cookie = null;
        int pagina = 1;

        do {
            PagedResultsDirContextProcessor processor = new PagedResultsDirContextProcessor(pageSize, cookie);

            log.info("🔍 Buscando pagina {} com filtro: {}", pagina, filtro);

            List<User> page = ldapTemplate.search(
                    "",
                    filtro,
                    searchControls,
                    (AttributesMapper<User>) attrs -> {
                        User user = new User();
                        user.setUsername(getValue(attrs, "sAMAccountName"));
                        user.setNome(getValue(attrs, "givenName"));
                        user.setSobrenome(getValue(attrs, "sn"));
                        user.setEmail(getValue(attrs, "mail"));
                        user.setOrigem("AD");
                        user.setTipo("USUARIO");
                        user.setPassword("123456");
                        return user;
                    },
                    processor
            );

            log.info("📄 Pagina {} carregada com {} usuarios", pagina, page.size());
            usuarios.addAll(page);

            cookie = processor.getCookie();
            pagina++;

        } while (cookie != null && cookie.getCookie() != null && cookie.getCookie().length > 0);

        log.info("✅ Fim da paginacao LDAP");
        return usuarios;
    }

    public void testarConexao() {
        ldapTemplate.search("", "(objectClass=*)", (AttributesMapper<String>) attrs -> "ok");
    }

    private String getValue(Attributes attrs, String key) {
        try {
            return attrs.get(key) != null ? attrs.get(key).get().toString() : null;
        } catch (Exception e) {
            log.warn("⚠️ Erro ao obter atributo '{}': {}", key, e.getMessage());
            return null;
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
