package sistema.rotinas.primefaces.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class TvApiKeyFilter extends OncePerRequestFilter {

    @Value("${tv.api.enabled:true}")
    private boolean enabled;

    @Value("${tv.api.key:}")
    private String apiKey;

    @Value("${tv.api.header:X-API-KEY}")
    private String headerName;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // só protege a API da TV
        return path == null || !path.contains("/api/tv/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        // se não configurou chave, melhor bloquear para não ficar aberto sem querer
        if (apiKey == null || apiKey.isBlank()) {
            unauthorized(response, "TV API key não configurada no servidor.");
            return;
        }

        String provided = request.getHeader(headerName);

        // (Opcional) fallback por query param: /api/tv/... ?key=...
        if (provided == null || provided.isBlank()) {
            provided = request.getParameter("key");
        }

        if (!apiKey.equals(provided)) {
            unauthorized(response, "Chave inválida.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void unauthorized(HttpServletResponse response, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"unauthorized\",\"message\":\"" + msg.replace("\"", "") + "\"}");
    }
}