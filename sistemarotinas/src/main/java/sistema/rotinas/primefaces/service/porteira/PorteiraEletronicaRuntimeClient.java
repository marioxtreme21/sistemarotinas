// FILE: src/main/java/sistema/rotinas/primefaces/service/porteira/PorteiraEletronicaRuntimeClient.java
package sistema.rotinas.primefaces.service.porteira;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import sistema.rotinas.primefaces.model.porteira.PorteiraEletronica;

@Service
public class PorteiraEletronicaRuntimeClient {

    private static final Logger LOG = LoggerFactory.getLogger("ROTINA_PORTEIRA");

    private static final String CONFIG_ENDPOINT = "http://%s:8087/?request=getconfig";
    private static final String SET_CONFIG_ENDPOINT = "http://%s:8087/?request=setconfig";
    private static final String REBOOT_ENDPOINT = "http://%s:8087/?request=reboot";

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final long WAIT_BEFORE_REBOOT_MS = 20_000; // legado
    private static final long WAIT_AFTER_REBOOT_MS  = 100_000; // legado

    private final RestTemplate restTemplate = new RestTemplate();

    public RuntimeExecResult ativar(PorteiraEletronica p) {
        return executar(p, true);
    }

    public RuntimeExecResult desativar(PorteiraEletronica p) {
        return executar(p, false);
    }

    private RuntimeExecResult executar(PorteiraEletronica p, boolean ativar) {
        StringBuilder log = new StringBuilder();
        LocalDateTime ini = LocalDateTime.now();

        if (p == null) return RuntimeExecResult.fail("Porteira nula.");

        String acao = ativar ? "ATIVAR" : "DESATIVAR";
        String desc = nz(p.getDescricao());
        String ip = nz(p.getIp());

        log.append(ativar ? "🟢" : "🔴")
           .append(" [").append(FMT.format(ini)).append("] ")
           .append("Iniciando ").append(acao).append(" - ")
           .append(desc).append(" IP=").append(ip).append("\n");

        try {
            String usuario = nz(p.getUsuarioIntegracao());
            String senha = nz(p.getSenhaIntegracao());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            if (!usuario.isBlank() || !senha.isBlank()) {
                String auth = usuario + ":" + senha;
                String encoded = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
                headers.set("Authorization", "Basic " + encoded);
            }

            log.append("🔎 Usuário: ").append(usuario.isBlank() ? "-" : usuario).append("\n")
               .append("🔎 Tipo de autenticação: Basic Auth\n");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // 1) Config atual
            ResponseEntity<Map> cfg = restTemplate.exchange(
                    String.format(CONFIG_ENDPOINT, ip),
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            log.append("📥 [").append(FMT.format(LocalDateTime.now())).append("] Configuração atual: ")
               .append(Objects.toString(cfg.getBody())).append("\n");

            LOG.debug("[PORTEIRA] {} getconfig HTTP={} ip={} body={}", acao, cfg.getStatusCodeValue(), ip, cfg.getBody());

            // 2) Payload desejado
            Map<String, String> payload = ativar
                    ? Map.of("keyboard", "true", "rfid", "on", "qrcode", "on", "qrcode_dynamic", "on", "fingerprint", "on")
                    : Map.of("keyboard", "false", "rfid", "off", "qrcode", "off", "qrcode_dynamic", "off", "fingerprint", "off");

            log.append("🧩 [").append(FMT.format(LocalDateTime.now())).append("] Enviando setconfig para ")
               .append(acao).append(" payload=").append(payload).append("\n");

            HttpEntity<Map<String, String>> setEntity = new HttpEntity<>(payload, headers);

            ResponseEntity<String> setCfg = restTemplate.exchange(
                    String.format(SET_CONFIG_ENDPOINT, ip),
                    HttpMethod.POST,
                    setEntity,
                    String.class
            );

            log.append("🔧 [").append(FMT.format(LocalDateTime.now())).append("] setconfig HTTP=")
               .append(setCfg.getStatusCodeValue()).append(" body=")
               .append(nz(setCfg.getBody())).append("\n");

            LOG.debug("[PORTEIRA] {} setconfig HTTP={} ip={} payload={} body={}",
                    acao, setCfg.getStatusCodeValue(), ip, payload, setCfg.getBody());

            // 3) Wait antes reboot
            log.append("⏳ Aguardando ").append(WAIT_BEFORE_REBOOT_MS / 1000).append("s antes do reboot...\n");
            sleepSilencioso(WAIT_BEFORE_REBOOT_MS);

            // 4) Reboot
            ResponseEntity<String> reboot = restTemplate.exchange(
                    String.format(REBOOT_ENDPOINT, ip),
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            log.append("🔄 [").append(FMT.format(LocalDateTime.now())).append("] Reboot solicitado HTTP=")
               .append(reboot.getStatusCodeValue()).append(" body=")
               .append(nz(reboot.getBody())).append("\n");

            LOG.debug("[PORTEIRA] {} reboot HTTP={} ip={} body={}", acao, reboot.getStatusCodeValue(), ip, reboot.getBody());

            // 5) Wait depois reboot
            log.append("⏳ Aguardando ").append(WAIT_AFTER_REBOOT_MS / 1000).append("s para a porteira subir...\n");
            sleepSilencioso(WAIT_AFTER_REBOOT_MS);

            // 6) Config final
            ResponseEntity<Map> cfgFinal = restTemplate.exchange(
                    String.format(CONFIG_ENDPOINT, ip),
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            log.append("📥 [").append(FMT.format(LocalDateTime.now())).append("] Configuração final após reboot: ")
               .append(Objects.toString(cfgFinal.getBody())).append("\n");

            LOG.debug("[PORTEIRA] {} getconfig FINAL HTTP={} ip={} body={}",
                    acao, cfgFinal.getStatusCodeValue(), ip, cfgFinal.getBody());

            // 7) Validar se aplicou (mínimo)
            Map<String, Object> diffs = comparar(cfgFinal.getBody(), payload);

            if (!diffs.isEmpty()) {
                log.append("⚠️ Divergências detectadas (config final x esperado): ").append(diffs).append("\n");
                log.append("❌ Processo finalizou com divergência.\n");

                LOG.warn("[PORTEIRA] {} divergência ip={} porteiraId={} diffs={}", acao, ip, p.getId(), diffs);
                return RuntimeExecResult.fail(log.toString());
            }

            log.append("✅ Processo concluído com sucesso.\n");
            LOG.info("[PORTEIRA] {} OK - {} (ip={})", acao, desc, ip);

            return RuntimeExecResult.ok(log.toString());

        } catch (Exception e) {
            log.append("❌ Erro durante o processo: ").append(e.getMessage()).append("\n");
            LOG.error("[PORTEIRA] {} FAIL - {} (ip={}) msg={}", acao, desc, ip, e.getMessage(), e);
            return RuntimeExecResult.fail(log.toString());
        }
    }

    private static Map<String, Object> comparar(Map body, Map<String, String> esperado) {
        Map<String, Object> diffs = new LinkedHashMap<>();
        if (esperado == null || esperado.isEmpty()) return diffs;

        if (body == null) {
            diffs.put("body", "null");
            return diffs;
        }

        for (Map.Entry<String, String> e : esperado.entrySet()) {
            String k = e.getKey();
            String exp = String.valueOf(e.getValue());

            Object atualObj = body.get(k);
            String atual = atualObj == null ? null : String.valueOf(atualObj);

            if (!equivalente(k, exp, atual)) {
                diffs.put(k, "esperado=" + exp + ", atual=" + atual);
            }
        }
        return diffs;
    }

    private static boolean equivalente(String key, String exp, String atual) {
        if (atual == null) return false;

        String e = exp.trim().toLowerCase();
        String a = atual.trim().toLowerCase();

        // normalizações comuns
        if (key.equals("keyboard")) {
            // alguns devices retornam "true/false", outros "1/0"
            if (e.equals("true"))  return a.equals("true") || a.equals("1");
            if (e.equals("false")) return a.equals("false") || a.equals("0");
        }

        // rfid/qrcode/fingerprint normalmente "on/off"
        return a.equals(e);
    }

    private static void sleepSilencioso(long ms) {
        try { Thread.sleep(ms); } catch (Exception ignore) {}
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }

    // =========================
    // Result
    // =========================
    public static class RuntimeExecResult {
        private final boolean ok;
        private final String log;

        private RuntimeExecResult(boolean ok, String log) {
            this.ok = ok;
            this.log = log;
        }

        public boolean isOk() { return ok; }

        public String getLog() { return log; }

        public static RuntimeExecResult ok(String log) { return new RuntimeExecResult(true, log); }

        public static RuntimeExecResult fail(String log) { return new RuntimeExecResult(false, log); }
    }
}