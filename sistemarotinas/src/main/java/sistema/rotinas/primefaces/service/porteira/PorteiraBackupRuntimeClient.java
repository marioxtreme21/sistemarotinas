// FILE: src/main/java/sistema/rotinas/primefaces/service/porteira/PorteiraBackupRuntimeClient.java
package sistema.rotinas.primefaces.service.porteira;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import sistema.rotinas.primefaces.model.porteira.PorteiraEletronica;

@Component
public class PorteiraBackupRuntimeClient {

    private static final Logger LOG = LoggerFactory.getLogger("ROTINA_PORTEIRA");

    private final RestTemplate restTemplate = new RestTemplate();

    public RuntimeGetResult baixarUsuariosJson(PorteiraEletronica porteira) {
        String ip = safeIp(porteira);
        String desc = safeDesc(porteira);
        Long id = (porteira != null ? porteira.getId() : null);

        String url = String.format("http://%s:8087/?request=users", ip);
        HttpEntity<Void> entity = new HttpEntity<>(buildHeaders(porteira));

        long ini = System.currentTimeMillis();
        LOG.info("[BACKUP][RUNTIME][GET_USERS] start porteiraId={} desc={} ip={} url={}", id, desc, ip, url);

        try {
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            long ms = System.currentTimeMillis() - ini;

            String body = resp.getBody();
            LOG.info("[BACKUP][RUNTIME][GET_USERS] ok porteiraId={} http={} ms={} bodyLen={}",
                    id, resp.getStatusCodeValue(), ms, (body != null ? body.length() : 0));

            // DEBUG opcional (corta para não poluir)
            if (LOG.isDebugEnabled()) {
                LOG.debug("[BACKUP][RUNTIME][GET_USERS] bodySample={}", safeCut(body, 1200));
            }

            return new RuntimeGetResult(true, resp.getStatusCodeValue(), body);

        } catch (HttpStatusCodeException e) {
            long ms = System.currentTimeMillis() - ini;
            String body = safeBody(e.getResponseBodyAsString(), e);

            LOG.error("[BACKUP][RUNTIME][GET_USERS] http_error porteiraId={} http={} ms={} msg={} bodySample={}",
                    id, e.getStatusCode().value(), ms, e.getMessage(), safeCut(body, 1200));

            return new RuntimeGetResult(false, e.getStatusCode().value(), body);

        } catch (ResourceAccessException e) {
            long ms = System.currentTimeMillis() - ini;

            LOG.error("[BACKUP][RUNTIME][GET_USERS] connection_fail porteiraId={} ms={} msg={}",
                    id, ms, e.getMessage(), e);

            return new RuntimeGetResult(false, 0, "CONNECTION_FAIL: " + e.getMessage());

        } catch (Exception e) {
            long ms = System.currentTimeMillis() - ini;

            LOG.error("[BACKUP][RUNTIME][GET_USERS] fail porteiraId={} ms={} msg={}",
                    id, ms, e.getMessage(), e);

            return new RuntimeGetResult(false, 0, "FAIL: " + e.getMessage());
        }
    }

    public RuntimePostResult enviarUsuario(PorteiraEletronica destino, String userJson) {
        String ip = safeIp(destino);
        String desc = safeDesc(destino);
        Long id = (destino != null ? destino.getId() : null);

        String url = String.format("http://%s:8087/?request=adduser", ip);

        HttpHeaders headers = buildHeaders(destino);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(userJson, headers);

        long ini = System.currentTimeMillis();

        // não logar payload inteiro em INFO (pode ter senha). Só tamanho / amostra curta em DEBUG.
        int len = (userJson != null ? userJson.length() : 0);
        LOG.debug("[RESTORE][RUNTIME][ADDUSER] start porteiraId={} desc={} ip={} url={} payloadLen={}",
                id, desc, ip, url, len);

        try {
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            long ms = System.currentTimeMillis() - ini;

            String body = resp.getBody();
            LOG.debug("[RESTORE][RUNTIME][ADDUSER] ok porteiraId={} http={} ms={} respLen={}",
                    id, resp.getStatusCodeValue(), ms, (body != null ? body.length() : 0));

            if (LOG.isTraceEnabled()) {
                LOG.trace("[RESTORE][RUNTIME][ADDUSER] payloadSample={}", safeCut(userJson, 800));
                LOG.trace("[RESTORE][RUNTIME][ADDUSER] respSample={}", safeCut(body, 800));
            }

            return new RuntimePostResult(true, resp.getStatusCodeValue(), body);

        } catch (HttpStatusCodeException e) {
            long ms = System.currentTimeMillis() - ini;
            String body = safeBody(e.getResponseBodyAsString(), e);

            LOG.error("[RESTORE][RUNTIME][ADDUSER] http_error porteiraId={} http={} ms={} msg={} respSample={}",
                    id, e.getStatusCode().value(), ms, e.getMessage(), safeCut(body, 1200));

            if (LOG.isTraceEnabled()) {
                LOG.trace("[RESTORE][RUNTIME][ADDUSER] payloadSample={}", safeCut(userJson, 800));
            }

            return new RuntimePostResult(false, e.getStatusCode().value(), body);

        } catch (ResourceAccessException e) {
            long ms = System.currentTimeMillis() - ini;

            LOG.error("[RESTORE][RUNTIME][ADDUSER] connection_fail porteiraId={} ms={} msg={}",
                    id, ms, e.getMessage(), e);

            return new RuntimePostResult(false, 0, "CONNECTION_FAIL: " + e.getMessage());

        } catch (Exception e) {
            long ms = System.currentTimeMillis() - ini;

            LOG.error("[RESTORE][RUNTIME][ADDUSER] fail porteiraId={} ms={} msg={}",
                    id, ms, e.getMessage(), e);

            return new RuntimePostResult(false, 0, "FAIL: " + e.getMessage());
        }
    }

    private HttpHeaders buildHeaders(PorteiraEletronica p) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(MediaType.parseMediaTypes("application/json"));

        String usuario = (p != null ? p.getUsuarioIntegracao() : null);
        String senha   = (p != null ? p.getSenhaIntegracao() : null);

        // evita NPE e ajuda diagnóstico (sem expor senha)
        if (usuario == null || usuario.isBlank() || senha == null || senha.isBlank()) {
            LOG.warn("[PORTEIRA][RUNTIME] credenciais_integracao_ausentes porteiraId={} desc={} ip={} usuarioPresent={} senhaPresent={}",
                    (p != null ? p.getId() : null),
                    safeDesc(p),
                    safeIp(p),
                    (usuario != null && !usuario.isBlank()),
                    (senha != null && !senha.isBlank()));
        }

        String auth = (usuario != null ? usuario : "") + ":" + (senha != null ? senha : "");
        String encoded = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encoded);

        return headers;
    }

    private String safeBody(String body, Exception e) {
        if (body != null && !body.isBlank()) return body;
        return "HTTP_ERROR: " + (e != null ? e.getMessage() : "unknown");
    }

    private static String safeCut(String s, int max) {
        if (s == null) return null;
        if (max <= 0) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private static String safeIp(PorteiraEletronica p) {
        String ip = (p != null ? p.getIp() : null);
        return (ip == null || ip.isBlank()) ? "-" : ip.trim();
    }

    private static String safeDesc(PorteiraEletronica p) {
        String d = (p != null ? p.getDescricao() : null);
        return (d == null || d.isBlank()) ? "-" : d.trim();
    }

    // =========================
    // DTOs de retorno
    // =========================

    public static class RuntimeGetResult {
        private final boolean ok;
        private final int httpCode;
        private final String body;

        public RuntimeGetResult(boolean ok, int httpCode, String body) {
            this.ok = ok;
            this.httpCode = httpCode;
            this.body = body;
        }

        public boolean isOk() { return ok; }
        public int getHttpCode() { return httpCode; }
        public String getBody() { return body; }
    }

    public static class RuntimePostResult {
        private final boolean ok;
        private final int httpCode;
        private final String body;

        public RuntimePostResult(boolean ok, int httpCode, String body) {
            this.ok = ok;
            this.httpCode = httpCode;
            this.body = body;
        }

        public boolean isOk() { return ok; }
        public int getHttpCode() { return httpCode; }
        public String getBody() { return body; }
    }
}