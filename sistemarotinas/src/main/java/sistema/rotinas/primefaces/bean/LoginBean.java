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
		FacesContext facesContext = FacesContext.getCurrentInstance();

		try {
			String u = (username != null ? username.trim() : "");
			String p = (password != null ? password : "");

			if (u.isBlank() || p.isBlank()) {
				facesContext.addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_WARN, "Atenção", "Informe usuário e senha."));
				return;
			}

			Authentication auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(u, p));

			// Atualiza o SecurityContext para a sessão atual
			SecurityContextHolder.getContext().setAuthentication(auth);

			// Garante a criação da sessão + persiste o SecurityContext
			HttpSession session = (HttpSession) facesContext.getExternalContext().getSession(true);
			session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

			// Redirecionar para a página inicial após o login
			facesContext.getExternalContext().redirect("/sistemarotinas/pages/index.xhtml");

		} catch (AuthenticationException e) {
			facesContext.addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Login falhou!", "Usuário ou senha inválidos."));
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
		return null;
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