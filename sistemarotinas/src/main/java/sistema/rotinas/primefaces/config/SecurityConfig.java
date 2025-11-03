package sistema.rotinas.primefaces.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import sistema.rotinas.primefaces.service.CustomUserDetailsService;

@Configuration
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService) {
        this.customUserDetailsService = customUserDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
            )
            .securityContext(securityContext -> securityContext
                .requireExplicitSave(false)
            )
            .authorizeHttpRequests(authorize -> authorize
                // Recursos estáticos (PrimeFaces, CSS, JS, imagens etc.)
                .requestMatchers("/resources/**", "/javax.faces.resource/**", "/jakarta.faces.resource/**").permitAll()

                // 🔥 Permitir acesso à pasta de uploads (anexos, imagens de e-mail, comprovantes, relatórios)
                .requestMatchers("/uploads/**").permitAll()

                // APIs públicas (para apps ou serviços externos)
                .requestMatchers(
                    "/api/lojas/**",
                    "/api/users/**",
                    "/api/notifications/**",
                    "/api/notifications/error/**",
                    "/api/telaprodutos/player/videos/**",
                    "/api/telaprodutos/player/images/**"
                ).permitAll()

                // Páginas públicas
                .requestMatchers("/pages/login.xhtml", "/register.xhtml").permitAll()

                // Acesso às páginas index e cadastro requer autenticação
                .requestMatchers("/pages/index.xhtml").authenticated()
                .requestMatchers("/pages/cadastro/**").hasAnyRole("ADMIN", "USER")

                // Acesso restrito às páginas de relatórios
                .requestMatchers("/pages/relatorios/**").hasRole("ADMIN")

                // Demais rotas protegidas
                .anyRequest().authenticated()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/pages/login.xhtml?faces-redirect=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .formLogin(form -> form.disable());

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
