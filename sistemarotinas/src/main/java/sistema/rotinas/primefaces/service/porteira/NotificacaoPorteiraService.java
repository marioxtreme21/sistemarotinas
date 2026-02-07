// FILE: src/main/java/sistema/rotinas/primefaces/service/porteira/NotificacaoPorteiraService.java
package sistema.rotinas.primefaces.service.porteira;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sistema.rotinas.primefaces.model.porteira.PorteiraBackup;
import sistema.rotinas.primefaces.model.porteira.PorteiraEletronica;
import sistema.rotinas.primefaces.service.EmailService;

@Service
public class NotificacaoPorteiraService {

    private static final Logger LOG = LoggerFactory.getLogger("ROTINA_PORTEIRA");

    @Autowired
    private EmailService emailService;

    /**
     * ✅ Lista para rotina de DESATIVAÇÃO/ATIVAÇÃO (já existente no projeto)
     */
    private static final List<String> DESTINATARIOS_PORTEIRA =
            List.of("lojasautonomas@hiperideal.com.br");

    /**
     * ✅ Lista específica para BACKUP/RESTORE
     */
    private static final List<String> DESTINATARIOS_BACKUP =
            List.of("backupautonomas@hiperideal.com.br");

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    // =========================================================
    // ROTINA DESATIVAR/ATIVAR (CANÔNICO EXISTENTE)
    // =========================================================
    public void notificarAcao(PorteiraEletronica porteira,
                              String acao,
                              String origem, // "MANUAL" | "AUTO"
                              boolean ok,
                              String mensagem,
                              String logCompleto) {

        String desc = (porteira != null && porteira.getDescricao() != null) ? porteira.getDescricao() : "-";
        String ip   = (porteira != null && porteira.getIp() != null) ? porteira.getIp() : "-";
        String id   = (porteira != null && porteira.getId() != null) ? String.valueOf(porteira.getId()) : "-";

        String statusTxt = ok ? "SUCESSO" : "FALHA";
        String emoji = ok ? "✅" : "❌";

        String origemNorm = normOrigem(origem);

        String agoraStr = LocalDateTime.now().format(FMT);

        String assunto = emoji
                + " Porteira - " + nz(acao)
                + " (" + origemNorm + ")"
                + " - " + desc
                + " - " + statusTxt
                + " - " + agoraStr;

        String titulo = "Rotina de Porteiras - Execução " + ("AUTO".equals(origemNorm) ? "Automática" : "Manual");

        StringBuilder corpo = new StringBuilder()
                .append("<div style='font-family:Arial,Helvetica,sans-serif;font-size:13px;'>")
                .append("<h2 style='margin:0 0 10px 0;'>").append(esc(titulo)).append("</h2>")

                .append("<p><b>Ação:</b> ").append(esc(nz(acao))).append(" (").append(esc(origemNorm)).append(")</p>")
                .append("<p><b>Porteira:</b> ").append(esc(desc)).append("</p>")
                .append("<p><b>Status:</b> ")
                .append(ok
                        ? "<span style='color:#137333;font-weight:bold;'>SUCESSO</span>"
                        : "<span style='color:#b91c1c;font-weight:bold;'>FALHA</span>")
                .append("</p>")

                .append("<p><b>Data/Hora:</b> ").append(esc(agoraStr)).append("</p>")
                .append("<p><b>ID:</b> ").append(esc(id)).append("</p>")
                .append("<p><b>IP:</b> ").append(esc(ip)).append("</p>");

        String msg = nz(mensagem);
        if (!"-".equals(msg) && !msg.isBlank()) {
            corpo.append("<p><b>Mensagem:</b> ").append(esc(msg)).append("</p>");
        }

        corpo.append("<h3 style='margin:14px 0 6px 0;'>Detalhes / Log</h3>")
             .append("<pre style='background:#f6f6f6;border:1px solid #ddd;padding:10px;white-space:pre-wrap;'>")
             .append(esc(nz(logCompleto)))
             .append("</pre>")
             .append("<div style='color:#888;margin-top:10px;'>E-mail automático - Sistema de Rotinas.</div>")
             .append("</div>");

        LOG.info("[{}][EMAIL][ACAO] to={} acao={} porteira={} ip={} status={}",
                origemNorm, DESTINATARIOS_PORTEIRA, nz(acao), nz(desc), nz(ip), statusTxt);

        try {
            emailService.enviarEmailSimples(DESTINATARIOS_PORTEIRA, assunto, corpo.toString());
            LOG.info("[{}][EMAIL][ACAO] ENVIADO assunto={}", origemNorm, assunto);
        } catch (Exception e) {
            LOG.error("[{}][EMAIL][ACAO] FALHA ao enviar assunto={} msg={}", origemNorm, assunto, e.getMessage(), e);
        }
    }

    // Compatibilidade (assinaturas antigas)
    public void notificarAcao(PorteiraEletronica porteira, String acao, boolean ok, String mensagem, String logCompleto) {
        notificarAcao(porteira, acao, "MANUAL", ok, mensagem, logCompleto);
    }

    public void notificarAcao(PorteiraEletronica porteira, String acao, boolean ok, String mensagem) {
        notificarAcao(porteira, acao, "MANUAL", ok, mensagem, mensagem);
    }

    // =========================================================
    // BACKUP (MANUAL/AUTO individual) - e-mail por execução
    // =========================================================
    public void notificarBackup(PorteiraEletronica porteira,
                                PorteiraBackup backup,
                                String origem, // MANUAL|AUTO
                                boolean ok,
                                String mensagem,
                                String logCompleto) {

        String origemNorm = normOrigem(origem);
        String agoraStr = LocalDateTime.now().format(FMT);

        String desc = (porteira != null ? nz(porteira.getDescricao()) : "-");
        String ip   = (porteira != null ? nz(porteira.getIp()) : "-");
        Long backupId = (backup != null ? backup.getId() : null);
        Integer totalUsuarios = (backup != null ? backup.getTotalUsuarios() : null);

        String emoji = ok ? "✅" : "❌";
        String statusTxt = ok ? "SUCESSO" : "FALHA";

        String assunto = emoji
                + " Porteira - BACKUP (" + origemNorm + ")"
                + " - " + desc
                + " - " + statusTxt
                + " - " + agoraStr;

        StringBuilder corpo = new StringBuilder()
                .append("<div style='font-family:Arial,Helvetica,sans-serif;font-size:13px;'>")
                .append("<h2 style='margin:0 0 10px 0;'>Backup de Usuários - Execução ").append(esc(origemNorm)).append("</h2>")
                .append("<p><b>Porteira:</b> ").append(esc(desc)).append("</p>")
                .append("<p><b>IP:</b> ").append(esc(ip)).append("</p>")
                .append("<p><b>Backup ID:</b> ").append(backupId != null ? backupId : "-").append("</p>")
                .append("<p><b>Usuários:</b> ").append(totalUsuarios != null ? totalUsuarios : "-").append("</p>")
                .append("<p><b>Status:</b> ").append(ok
                        ? "<span style='color:#137333;font-weight:bold;'>SUCESSO</span>"
                        : "<span style='color:#b91c1c;font-weight:bold;'>FALHA</span>").append("</p>")
                .append("<p><b>Data/Hora:</b> ").append(esc(agoraStr)).append("</p>");

        String msg = nz(mensagem);
        if (!"-".equals(msg) && !msg.isBlank()) {
            corpo.append("<p><b>Mensagem:</b> ").append(esc(msg)).append("</p>");
        }

        corpo.append("<h3 style='margin:14px 0 6px 0;'>Log</h3>")
             .append("<pre style='background:#f6f6f6;border:1px solid #ddd;padding:10px;white-space:pre-wrap;'>")
             .append(esc(nz(logCompleto)))
             .append("</pre>")
             .append("<div style='color:#888;margin-top:10px;'>E-mail automático - Sistema de Rotinas.</div>")
             .append("</div>");

        LOG.info("[{}][EMAIL][BACKUP] to={} porteira={} ip={} backupId={} usuarios={} status={}",
                origemNorm, DESTINATARIOS_BACKUP, nz(desc), nz(ip), (backupId != null ? backupId : "-"),
                (totalUsuarios != null ? totalUsuarios : "-"), statusTxt);

        try {
            emailService.enviarEmailSimples(DESTINATARIOS_BACKUP, assunto, corpo.toString());
            LOG.info("[{}][EMAIL][BACKUP] ENVIADO assunto={}", origemNorm, assunto);
        } catch (Exception e) {
            LOG.error("[{}][EMAIL][BACKUP] FALHA ao enviar assunto={} msg={}", origemNorm, assunto, e.getMessage(), e);
        }
    }

    // =========================================================
    // RESTORE - e-mail por execução
    // =========================================================
    public void notificarRestore(PorteiraEletronica origem,
                                 PorteiraEletronica destino,
                                 Long backupId,
                                 int total,
                                 int ok,
                                 int falha,
                                 boolean dryRun,
                                 String origemExecucao, // MANUAL|AUTO
                                 String logCompleto) {

        String origemNorm = normOrigem(origemExecucao);

        String agoraStr = LocalDateTime.now().format(FMT);

        String oDesc = (origem != null ? nz(origem.getDescricao()) : "-");
        String oIp   = (origem != null ? nz(origem.getIp()) : "-");
        String dDesc = (destino != null ? nz(destino.getDescricao()) : "-");
        String dIp   = (destino != null ? nz(destino.getIp()) : "-");

        boolean sucessoGeral = (falha == 0);
        String emoji = sucessoGeral ? "✅" : (ok > 0 ? "⚠️" : "❌");
        String statusTxt = sucessoGeral ? "SUCESSO" : (ok > 0 ? "PARCIAL" : "FALHA");

        String assunto = emoji
                + " Porteira - RESTORE (" + origemNorm + ")"
                + " - backupId=" + (backupId != null ? backupId : "-")
                + " - " + statusTxt
                + " - " + agoraStr;

        StringBuilder corpo = new StringBuilder()
                .append("<div style='font-family:Arial,Helvetica,sans-serif;font-size:13px;'>")
                .append("<h2 style='margin:0 0 10px 0;'>Restore de Usuários - Execução ").append(esc(origemNorm)).append("</h2>")

                .append("<p><b>Backup ID:</b> ").append(backupId != null ? backupId : "-").append("</p>")

                .append("<p><b>Origem:</b> ").append(esc(oDesc)).append(" (").append(esc(oIp)).append(")</p>")
                .append("<p><b>Destino:</b> ").append(esc(dDesc)).append(" (").append(esc(dIp)).append(")</p>")

                .append("<p><b>Dry-run:</b> ").append(dryRun ? "SIM (sem alterações)" : "NÃO").append("</p>")

                .append("<p><b>Resultado:</b> total=").append(total)
                .append(" ok=").append(ok)
                .append(" falha=").append(falha)
                .append("</p>")

                .append("<p><b>Status:</b> ").append(sucessoGeral
                        ? "<span style='color:#137333;font-weight:bold;'>SUCESSO</span>"
                        : "<span style='color:#b91c1c;font-weight:bold;'>FALHA/PARCIAL</span>").append("</p>")

                .append("<p><b>Data/Hora:</b> ").append(esc(agoraStr)).append("</p>")

                .append("<h3 style='margin:14px 0 6px 0;'>Log</h3>")
                .append("<pre style='background:#f6f6f6;border:1px solid #ddd;padding:10px;white-space:pre-wrap;'>")
                .append(esc(nz(logCompleto)))
                .append("</pre>")
                .append("<div style='color:#888;margin-top:10px;'>E-mail automático - Sistema de Rotinas.</div>")
                .append("</div>");

        LOG.info("[{}][EMAIL][RESTORE] to={} backupId={} origem={}({}) destino={}({}) total={} ok={} falha={} dryRun={} status={}",
                origemNorm, DESTINATARIOS_BACKUP, (backupId != null ? backupId : "-"),
                nz(oDesc), nz(oIp), nz(dDesc), nz(dIp),
                total, ok, falha, dryRun, statusTxt);

        try {
            emailService.enviarEmailSimples(DESTINATARIOS_BACKUP, assunto, corpo.toString());
            LOG.info("[{}][EMAIL][RESTORE] ENVIADO assunto={}", origemNorm, assunto);
        } catch (Exception e) {
            LOG.error("[{}][EMAIL][RESTORE] FALHA ao enviar assunto={} msg={}", origemNorm, assunto, e.getMessage(), e);
        }
    }

    // =========================================================
    // ✅ BACKUP AUTOMÁTICO - RESUMO CONSOLIDADO
    //  - Assunto e corpo bem diferentes do manual
    // =========================================================
    public void notificarResumoBackupAuto(List<ResumoBackupItem> itens,
                                          boolean dryRun,
                                          String logCompleto) {

        String agoraStr = LocalDateTime.now().format(FMT);

        int totalPorteiras = (itens != null ? itens.size() : 0);
        int okCount = 0;
        int falhaCount = 0;
        int somaUsuarios = 0;

        if (itens != null) {
            for (ResumoBackupItem i : itens) {
                if (i != null && i.ok) okCount++; else falhaCount++;
                if (i != null) somaUsuarios += Math.max(0, i.totalUsuarios);
            }
        }

        boolean sucessoGeral = (falhaCount == 0);
        String statusTxt = sucessoGeral ? "OK" : (okCount > 0 ? "PARCIAL" : "FALHA");
        String emoji = sucessoGeral ? "✅" : (okCount > 0 ? "⚠️" : "❌");

        // ✅ ASSUNTO (AUTO/RESUMO DIÁRIO)
        String assunto = emoji
                + " Porteiras - BACKUP AUTOMÁTICO - RESUMO DIÁRIO"
                + " - " + statusTxt
                + " - " + agoraStr;

        // ✅ CORPO (AUTO/RESUMO DIÁRIO)
        StringBuilder corpo = new StringBuilder()
            .append("<div style='font-family:Arial,Helvetica,sans-serif;font-size:13px;'>")
            .append("<h2 style='margin:0 0 10px 0;'>Backup Automático de Usuários - Resumo Diário</h2>")
            .append("<p style='margin:0 0 10px 0;color:#555;'>Consolidado do agendamento (1 e-mail por execução do lote).</p>")

            .append("<p><b>Execução:</b> <span style='color:#1f2937;font-weight:bold;'>AUTOMÁTICA</span></p>")
            .append("<p><b>Data/Hora:</b> ").append(esc(agoraStr)).append("</p>")
            .append("<p><b>Dry-run:</b> ").append(dryRun ? "SIM (sem alterações)" : "NÃO").append("</p>")

            .append("<p><b>Resumo:</b> ")
            .append("Porteiras=").append(totalPorteiras)
            .append(" | OK=").append(okCount)
            .append(" | Falha=").append(falhaCount)
            .append(" | Usuários (somatório)=").append(somaUsuarios)
            .append("</p>");

        corpo.append("<h3 style='margin:14px 0 6px 0;'>Resultado por porteira</h3>")
             .append("<table cellpadding='6' cellspacing='0' style='border-collapse:collapse;border:1px solid #ddd;width:100%;'>")
             .append("<thead><tr style='background:#f6f6f6;'>")
             .append("<th style='border:1px solid #ddd;text-align:left;'>Porteira</th>")
             .append("<th style='border:1px solid #ddd;text-align:left;'>IP</th>")
             .append("<th style='border:1px solid #ddd;text-align:left;'>Status</th>")
             .append("<th style='border:1px solid #ddd;text-align:right;'>Usuários</th>")
             .append("<th style='border:1px solid #ddd;text-align:right;'>Backup ID</th>")
             .append("<th style='border:1px solid #ddd;text-align:left;'>Observação</th>")
             .append("</tr></thead><tbody>");

        if (itens != null) {
            for (ResumoBackupItem i : itens) {
                if (i == null) continue;

                String st = i.ok
                        ? "<span style='color:#137333;font-weight:bold;'>OK</span>"
                        : "<span style='color:#b91c1c;font-weight:bold;'>FALHA</span>";

                corpo.append("<tr>")
                     .append("<td style='border:1px solid #ddd;'>").append(esc(nz(i.porteiraDescricao))).append("</td>")
                     .append("<td style='border:1px solid #ddd;'>").append(esc(nz(i.porteiraIp))).append("</td>")
                     .append("<td style='border:1px solid #ddd;'>").append(st).append("</td>")
                     .append("<td style='border:1px solid #ddd;text-align:right;'>").append(i.totalUsuarios).append("</td>")
                     .append("<td style='border:1px solid #ddd;text-align:right;'>").append(i.backupId != null ? i.backupId : "-").append("</td>")
                     .append("<td style='border:1px solid #ddd;'>").append(esc(nz(i.mensagem))).append("</td>")
                     .append("</tr>");
            }
        }

        corpo.append("</tbody></table>");

        corpo.append("<h3 style='margin:14px 0 6px 0;'>Log consolidado (técnico)</h3>")
             .append("<pre style='background:#f6f6f6;border:1px solid #ddd;padding:10px;white-space:pre-wrap;'>")
             .append(esc(nz(logCompleto)))
             .append("</pre>")
             .append("<div style='color:#888;margin-top:10px;'>E-mail automático - Sistema de Rotinas.</div>")
             .append("</div>");

        LOG.info("[AUTO][EMAIL][BACKUP_RESUMO] to={} porteiras={} ok={} falha={} somaUsuarios={} dryRun={} status={}",
                DESTINATARIOS_BACKUP, totalPorteiras, okCount, falhaCount, somaUsuarios, dryRun, statusTxt);

        try {
            emailService.enviarEmailSimples(DESTINATARIOS_BACKUP, assunto, corpo.toString());
            LOG.info("[AUTO][EMAIL][BACKUP_RESUMO] ENVIADO assunto={}", assunto);
        } catch (Exception e) {
            LOG.error("[AUTO][EMAIL][BACKUP_RESUMO] FALHA ao enviar assunto={} msg={}", assunto, e.getMessage(), e);
        }
    }

    // =========================================================
    // DTO para o resumo
    // =========================================================
    public static class ResumoBackupItem {
        public Long backupId;
        public Long porteiraId;
        public String porteiraDescricao;
        public String porteiraIp;
        public boolean ok;
        public int totalUsuarios;
        public String mensagem;

        public ResumoBackupItem() {}
    }

    // =========================================================
    // HELPERS
    // =========================================================
    private static String nz(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String normOrigem(String origem) {
        String o = nz(origem).trim().toUpperCase();
        if (!"AUTO".equals(o) && !"MANUAL".equals(o)) return "MANUAL";
        return o;
    }
}