package sistema.rotinas.primefaces.bean;

import java.io.IOException;
import java.io.Serializable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpSession;

@Component
@Named
@SessionScoped
public class LoginBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private String username;
    private String password;

    @Autowired
    private AuthenticationManager authenticationManager;




    // Método de login
    public void login() {
        try {
            Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
            );

            // Atualiza o SecurityContext para a sessão atual
            SecurityContextHolder.getContext().setAuthentication(auth);
            FacesContext.getCurrentInstance().getExternalContext().getSession(true); // Garante a criação da sessão

            // Redirecionar para a página inicial após o login
            FacesContext.getCurrentInstance().getExternalContext().redirect("/sistemarotinas/pages/index.xhtml");
        } catch (AuthenticationException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Login falhou!", null));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String logout() {
        try {
            System.out.println("Método de logout chamado!");

            FacesContext facesContext = FacesContext.getCurrentInstance();
            HttpSession session = (HttpSession) facesContext.getExternalContext().getSession(false);
            if (session != null) {
                session.invalidate(); // Invalida a sessão
            }
            SecurityContextHolder.clearContext(); // Limpa o contexto de segurança

            // Redirecionar para a página de login
            facesContext.getExternalContext().redirect("/sistemarotinas/pages/login.xhtml");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null; // Retorna nulo para não quebrar a navegação do JSF
    }

    public boolean hasRole(String role) {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
            .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_" + role));
    }
    

    // Getters e setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}