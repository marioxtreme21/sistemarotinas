// FILE: src/main/java/sistema/rotinas/primefaces/scheduled/handlers/PorteiraBackupScheduledHandler.java
package sistema.rotinas.primefaces.scheduled.handlers;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import sistema.rotinas.primefaces.model.porteira.PorteiraBackup;
import sistema.rotinas.primefaces.model.porteira.PorteiraEletronica;
import sistema.rotinas.primefaces.scheduled.runtime.ScheduledTaskHandler;
import sistema.rotinas.primefaces.scheduled.runtime.TaskExecutionContext;
import sistema.rotinas.primefaces.scheduled.runtime.TaskRunResult;
import sistema.rotinas.primefaces.service.interfaces.porteira.IPorteiraEletronicaService;
import sistema.rotinas.primefaces.service.porteira.NotificacaoPorteiraService;
import sistema.rotinas.primefaces.service.porteira.NotificacaoPorteiraService.ResumoBackupItem;
import sistema.rotinas.primefaces.service.porteira.PorteiraBackupService;

@Component
public class PorteiraBackupScheduledHandler implements ScheduledTaskHandler {

    public static final String KEY = "ROTINA_PORTEIRA_BACKUP_AUTOMATICA";

    // Log do scheduler (persistência / execução do agendador)
    private static final Logger SCHED_LOG = LoggerFactory.getLogger("SCHEDULER_DB");

    // Log dedicado da rotina de porteira (vai para rotina-porteira.log)
    private static final Logger LOG = LoggerFactory.getLogger("ROTINA_PORTEIRA");

    /**
     * Anti-duplicidade (igual ao handler de ativar/desativar):
     * chave = KEY|YYYY-MM-DDTHH:mm
     */
    private static final Map<String, LocalDateTime> DEDUP = new ConcurrentHashMap<>();

    private final IPorteiraEletronicaService porteiraService;
    private final PorteiraBackupService backupService;
    private final NotificacaoPorteiraService notificacaoService;

    public PorteiraBackupScheduledHandler(IPorteiraEletronicaService porteiraService,
                                          PorteiraBackupService backupService,
                                          NotificacaoPorteiraService notificacaoService) {
        this.porteiraService = porteiraService;
        this.backupService = backupService;
        this.notificacaoService = notificacaoService;
    }

    @Override
    public String taskKey() {
        return KEY;
    }

    @Override
    public TaskRunResult execute(TaskExecutionContext ctx) {

        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        boolean dryRun = (ctx != null && ctx.paramBool("dryRun", false));

        // ✅ evita rodar 2x no mesmo minuto (caso o scheduler dispare duplicado)
        if (!runOncePerMinute(now)) {
            SCHED_LOG.warn("[DBSCHED][{}] DEDUP: já executado neste minuto. now={} dryRun={}", KEY, now, dryRun);
            LOG.warn("[AUTO][BACKUP][DEDUP] já executado neste minuto. now={} dryRun={}", now, dryRun);
            return TaskRunResult.skipped("DEDUP: já executado neste minuto. now=" + now);
        }

        long t0 = System.currentTimeMillis();

        try {
            SCHED_LOG.info("[DBSCHED][{}] Tick now={} dryRun={}", KEY, now, dryRun);
            LOG.info("[AUTO][BACKUP][INICIO] now={} dryRun={}", now, dryRun);

            List<PorteiraEletronica> todas = porteiraService.getAllPorteiras();
            if (todas == null || todas.isEmpty()) {
                SCHED_LOG.info("[DBSCHED][{}] Nenhuma porteira cadastrada.", KEY);
                LOG.info("[AUTO][BACKUP] Nenhuma porteira cadastrada.");
                return TaskRunResult.skipped("Nenhuma porteira cadastrada.");
            }

            List<ResumoBackupItem> resumo = new ArrayList<>();
            StringBuilder logConsolidado = new StringBuilder();
            logConsolidado.append("[AUTO] Backup de porteiras (lote) - inicio=").append(now)
                          .append(" dryRun=").append(dryRun).append("\n");

            int executadas = 0;
            int ok = 0;
            int falha = 0;
            int somaUsuarios = 0;

            for (PorteiraEletronica p : todas) {
                if (p == null || p.getId() == null) {
                    continue;
                }

                long tP0 = System.currentTimeMillis();

                Long porteiraId = p.getId();
                String desc = safe(p.getDescricao());
                String ip = safe(p.getIp());

                ResumoBackupItem item = new ResumoBackupItem();
                item.porteiraId = porteiraId;
                item.porteiraDescricao = desc;
                item.porteiraIp = ip;
                item.totalUsuarios = 0;

                LOG.info("[AUTO][BACKUP] Iniciando porteiraId={} desc='{}' ip={}", porteiraId, desc, ip);

                try {
                    if (dryRun) {
                        item.ok = true;
                        item.mensagem = "DRY-RUN (sem execução)";
                        resumo.add(item);

                        ok++;
                        logConsolidado.append("[DRY] porteiraId=").append(porteiraId)
                                      .append(" desc=").append(desc)
                                      .append(" ip=").append(ip)
                                      .append("\n");

                        LOG.info("[AUTO][BACKUP][DRY] porteiraId={} concluído (sem execução)", porteiraId);
                        continue;
                    }

                    // ✅ este método NÃO envia e-mail individual por porteira
                    PorteiraBackup b = backupService.executarBackupAutoSemNotificar(porteiraId);

                    item.backupId = (b != null ? b.getId() : null);
                    item.totalUsuarios = (b != null && b.getTotalUsuarios() != null) ? b.getTotalUsuarios() : 0;
                    item.ok = (b != null && "OK".equalsIgnoreCase(b.getStatus()));
                    item.mensagem = item.ok ? "Backup OK" : ("Status=" + (b != null ? b.getStatus() : "NULL"));

                    resumo.add(item);
                    executadas++;

                    if (item.ok) ok++; else falha++;
                    somaUsuarios += Math.max(0, item.totalUsuarios);

                    logConsolidado.append(item.ok ? "[OK] " : "[FALHA] ")
                                  .append("porteiraId=").append(porteiraId)
                                  .append(" desc=").append(desc)
                                  .append(" ip=").append(ip)
                                  .append(" usuarios=").append(item.totalUsuarios)
                                  .append(" backupId=").append(item.backupId)
                                  .append(" msg=").append(item.mensagem)
                                  .append("\n");

                    long dt = System.currentTimeMillis() - tP0;
                    if (item.ok) {
                        LOG.info("[AUTO][BACKUP][OK] porteiraId={} backupId={} usuarios={} dtMs={}",
                                porteiraId, item.backupId, item.totalUsuarios, dt);
                    } else {
                        LOG.error("[AUTO][BACKUP][FALHA] porteiraId={} backupId={} usuarios={} msg={} dtMs={}",
                                porteiraId, item.backupId, item.totalUsuarios, item.mensagem, dt);
                    }

                } catch (Exception e) {
                    item.ok = false;
                    item.mensagem = "EXCEPTION: " + e.getMessage();
                    resumo.add(item);

                    falha++;

                    logConsolidado.append("[EXCEPTION] porteiraId=").append(porteiraId)
                                  .append(" desc=").append(desc)
                                  .append(" ip=").append(ip)
                                  .append(" msg=").append(e.getMessage())
                                  .append("\n");

                    LOG.error("[AUTO][BACKUP][EXCEPTION] porteiraId={} desc='{}' ip={} msg={}",
                            porteiraId, desc, ip, e.getMessage(), e);
                }
            }

            logConsolidado.append("\n[AUTO] Fim lote. porteiras=")
                          .append(todas.size())
                          .append(" executadas=").append(executadas)
                          .append(" ok=").append(ok)
                          .append(" falha=").append(falha)
                          .append(" somaUsuarios=").append(somaUsuarios)
                          .append(" fim=").append(LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")))
                          .append("\n");

            // ✅ 1 e-mail ao final (RESUMO DIÁRIO)
            try {
                notificacaoService.notificarResumoBackupAuto(resumo, dryRun, logConsolidado.toString());
                LOG.info("[AUTO][BACKUP][EMAIL_RESUMO] enviado. porteiras={} executadas={} ok={} falha={} somaUsuarios={} dryRun={}",
                        todas.size(), executadas, ok, falha, somaUsuarios, dryRun);
            } catch (Exception e) {
                SCHED_LOG.error("[DBSCHED][{}] Falha ao enviar e-mail resumo: {}", KEY, e.getMessage(), e);
                LOG.error("[AUTO][BACKUP][EMAIL_RESUMO] FALHA ao enviar msg={}", e.getMessage(), e);
            }

            long dtTotal = System.currentTimeMillis() - t0;

            if (!dryRun && executadas == 0) {
                SCHED_LOG.info("[DBSCHED][{}] Tick ok (SKIP). porteiras={} executadas=0 now={} dtMs={}",
                        KEY, (todas != null ? todas.size() : 0), now, dtTotal);
                LOG.info("[AUTO][BACKUP][FIM] SKIP. porteiras={} executadas=0 dtMs={}",
                        (todas != null ? todas.size() : 0), dtTotal);
                return TaskRunResult.skipped("Tick ok. porteiras=" + todas.size() + " executadas=0 now=" + now);
            }

            SCHED_LOG.info("[DBSCHED][{}] Tick ok. porteiras={} executadas={} ok={} falha={} dtMs={}",
                    KEY, todas.size(), executadas, ok, falha, dtTotal);
            LOG.info("[AUTO][BACKUP][FIM] porteiras={} executadas={} ok={} falha={} somaUsuarios={} dryRun={} dtMs={}",
                    todas.size(), executadas, ok, falha, somaUsuarios, dryRun, dtTotal);

            return TaskRunResult.ok("Tick ok. porteiras=" + todas.size()
                    + " executadas=" + executadas + " ok=" + ok + " falha=" + falha + " now=" + now);

        } catch (Exception e) {
            SCHED_LOG.error("[DBSCHED][{}] FAIL. {}", KEY, e.getMessage(), e);
            LOG.error("[AUTO][BACKUP][FAIL] msg={}", e.getMessage(), e);
            return TaskRunResult.fail("Falha na ROTINA_PORTEIRA_BACKUP_AUTOMATICA: " + e.getMessage());
        }
    }

    private static boolean runOncePerMinute(LocalDateTime minuteKey) {
        String key = KEY + "|" + minuteKey;
        LocalDateTime prev = DEDUP.putIfAbsent(key, LocalDateTime.now());
        return prev == null;
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}