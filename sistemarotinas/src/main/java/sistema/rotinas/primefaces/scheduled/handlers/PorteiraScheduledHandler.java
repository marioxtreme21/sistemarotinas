// FILE: src/main/java/sistema/rotinas/primefaces/scheduled/handlers/PorteiraScheduledHandler.java
package sistema.rotinas.primefaces.scheduled.handlers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import sistema.rotinas.primefaces.model.porteira.PorteiraEletronica;
import sistema.rotinas.primefaces.scheduled.runtime.ScheduledTaskHandler;
import sistema.rotinas.primefaces.scheduled.runtime.TaskExecutionContext;
import sistema.rotinas.primefaces.scheduled.runtime.TaskRunResult;
import sistema.rotinas.primefaces.service.interfaces.porteira.IPorteiraEletronicaService;
import sistema.rotinas.primefaces.service.porteira.NotificacaoPorteiraService;
import sistema.rotinas.primefaces.service.porteira.PorteiraEletronicaRuntimeClient;

@Component
public class PorteiraScheduledHandler implements ScheduledTaskHandler {

    public static final String KEY = "ROTINA_PORTEIRA_AUTOMATICA";

    private static final Logger SCHED_LOG = LoggerFactory.getLogger("SCHEDULER_DB");
    private static final Logger LOG = LoggerFactory.getLogger("ROTINA_PORTEIRA");

    private final IPorteiraEletronicaService porteiraService;
    private final PorteiraEletronicaRuntimeClient runtimeClient;
    private final NotificacaoPorteiraService notificacaoPorteiraService;

    /**
     * Anti-duplicidade:
     * chave = porteiraId|acao|YYYY-MM-DDTHH:mm
     * valor = instante do registro
     */
    private static final Map<String, note> DEDUP = new ConcurrentHashMap<>();

    public PorteiraScheduledHandler(IPorteiraEletronicaService porteiraService,
                                    PorteiraEletronicaRuntimeClient runtimeClient,
                                    NotificacaoPorteiraService notificacaoPorteiraService) {
        this.porteiraService = porteiraService;
        this.runtimeClient = runtimeClient;
        this.notificacaoPorteiraService = notificacaoPorteiraService;
    }

    @Override
    public String taskKey() {
        return KEY;
    }

    @Override
    public TaskRunResult execute(TaskExecutionContext ctx) {

        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        LocalDate hoje = now.toLocalDate();
        LocalTime horaAgora = now.toLocalTime().truncatedTo(ChronoUnit.MINUTES);

        boolean dryRun = (ctx != null && ctx.paramBool("dryRun", false));

        int total = 0;
        int elegiveis = 0;
        int executadas = 0;
        int ignoradasFlag = 0;
        int ignoradasJanela = 0;
        int ignoradasSemHorario = 0;
        int dedupBloqueadas = 0;

        long ini = System.currentTimeMillis();

        try {
            SCHED_LOG.debug("[HANDLER][PORTEIRA] Tick. now={} dryRun={}", now, dryRun);
            LOG.info("[AUTO][PORTEIRA] Tick start now={} dryRun={}", now, dryRun);

            List<PorteiraEletronica> todas = porteiraService.getAllPorteiras();
            if (todas == null || todas.isEmpty()) {
                LOG.warn("[AUTO][PORTEIRA] Nenhuma porteira cadastrada.");
                return TaskRunResult.skipped("Nenhuma porteira cadastrada.");
            }

            total = todas.size();

            for (PorteiraEletronica p : todas) {
                if (p == null) continue;

                Long id = p.getId();
                String desc = safe(p.getDescricao());
                String ip = safe(p.getIp());

                // Flag habilita rotina
                if (!Boolean.TRUE.equals(p.getExecutarRotinaDesativacaoAtiva())) {
                    ignoradasFlag++;
                    LOG.debug("[AUTO][PORTEIRA] Skip flagDesativacao=false porteiraId={} desc={} ip={}", id, desc, ip);
                    continue;
                }

                // Janela de data
                LocalDate di = p.getDataInicio();
                LocalDate df = p.getDataFim();

                if (di != null && hoje.isBefore(di)) {
                    ignoradasJanela++;
                    LOG.debug("[AUTO][PORTEIRA] Skip janelaData (hoje<inicio) porteiraId={} desc={} hoje={} inicio={}",
                            id, desc, hoje, di);
                    continue;
                }

                if (df != null && hoje.isAfter(df)) {
                    ignoradasJanela++;
                    LOG.debug("[AUTO][PORTEIRA] Skip janelaData (hoje>fim) porteiraId={} desc={} hoje={} fim={}",
                            id, desc, hoje, df);
                    continue;
                }

                LocalTime hi = safeTrunc(p.getHoraInicio());
                LocalTime hf = safeTrunc(p.getHoraFim());

                if (hi == null && hf == null) {
                    ignoradasSemHorario++;
                    LOG.debug("[AUTO][PORTEIRA] Skip semHorario porteiraId={} desc={} ip={}", id, desc, ip);
                    continue;
                }

                elegiveis++;

                boolean rodouAlgoNestaPorteira = false;

                if (hi != null && horaAgora.equals(hi)) {
                    if (runOncePerMinute(p, "DESATIVAR", now)) {
                        executadas += executarAcao(p, "DESATIVAR", false, dryRun);
                        rodouAlgoNestaPorteira = true;
                    } else {
                        dedupBloqueadas++;
                        LOG.debug("[AUTO][PORTEIRA] Dedup bloqueou DESATIVAR porteiraId={} now={}", id, now);
                    }
                }

                if (hf != null && horaAgora.equals(hf)) {
                    if (runOncePerMinute(p, "ATIVAR", now)) {
                        executadas += executarAcao(p, "ATIVAR", true, dryRun);
                        rodouAlgoNestaPorteira = true;
                    } else {
                        dedupBloqueadas++;
                        LOG.debug("[AUTO][PORTEIRA] Dedup bloqueou ATIVAR porteiraId={} now={}", id, now);
                    }
                }

                if (!rodouAlgoNestaPorteira) {
                    // útil pra entender que está "elegível" mas não é o minuto de execução
                    LOG.trace("[AUTO][PORTEIRA] Elegivel mas nao executou neste minuto porteiraId={} hi={} hf={} now={}",
                            id, hi, hf, horaAgora);
                }
            }

            long ms = System.currentTimeMillis() - ini;

            LOG.info("[AUTO][PORTEIRA] Tick end now={} ms={} total={} elegiveis={} executadas={} dryRun={} " +
                            "skipFlag={} skipJanela={} skipSemHorario={} dedup={}",
                    now, ms, total, elegiveis, executadas, dryRun,
                    ignoradasFlag, ignoradasJanela, ignoradasSemHorario, dedupBloqueadas);

            if (executadas == 0) {
                return TaskRunResult.skipped("Tick ok. total=" + total +
                        " elegiveis=" + elegiveis +
                        " executadas=0 now=" + now);
            }

            return TaskRunResult.ok("Tick ok. total=" + total +
                    " elegiveis=" + elegiveis +
                    " executadas=" + executadas +
                    " now=" + now);

        } catch (Exception e) {
            long ms = System.currentTimeMillis() - ini;
            SCHED_LOG.error("[HANDLER][PORTEIRA] FAIL. now={} ms={} msg={}", now, ms, e.getMessage(), e);
            LOG.error("[AUTO][PORTEIRA] Tick FAIL now={} ms={} msg={}", now, ms, e.getMessage(), e);
            return TaskRunResult.fail("Falha na ROTINA_PORTEIRA_AUTOMATICA: " + e.getMessage());
        }
    }

    /**
     * Executa a ação e notifica por e-mail igual ao manual.
     * @return 1 se executou, 0 se dryRun
     */
    private int executarAcao(PorteiraEletronica p, String acao, boolean ativar, boolean dryRun) {

        Long id = p.getId();
        String desc = safe(p.getDescricao());
        String ip = safe(p.getIp());

        long ini = System.currentTimeMillis();
        LOG.info("[AUTO][PORTEIRA] Solicitação {} - porteiraId={}, descricao={}, ip={} dryRun={}", acao, id, desc, ip, dryRun);

        if (dryRun) {
            String logFake =
                    "[DRY-RUN] Ação=" + acao + " porteiraId=" + id + " descricao=" + desc + " ip=" + ip + "\n" +
                    "Nenhuma chamada HTTP foi feita.";

            notificacaoPorteiraService.notificarAcao(
                    p,
                    acao,
                    "AUTO",
                    true,
                    "DRY-RUN (nenhuma chamada HTTP foi feita)",
                    logFake
            );

            long ms = System.currentTimeMillis() - ini;
            LOG.info("[AUTO][PORTEIRA] DRY-RUN concluído - porteiraId={}, acao={} ms={}", id, acao, ms);
            return 0;
        }

        PorteiraEletronicaRuntimeClient.RuntimeExecResult r =
                ativar ? runtimeClient.ativar(p) : runtimeClient.desativar(p);

        boolean ok = (r != null && r.isOk());
        String logDetalhado = (r != null ? r.getLog() : "Retorno nulo do runtimeClient");

        long ms = System.currentTimeMillis() - ini;

        if (ok) {
            LOG.info("[AUTO][PORTEIRA] {} OK - porteiraId={}, descricao={} ms={}", acao, id, desc, ms);
        } else {
            LOG.error("[AUTO][PORTEIRA] {} FALHA - porteiraId={}, descricao={} ms={}", acao, id, desc, ms);
        }

        notificacaoPorteiraService.notificarAcao(
                p,
                acao,
                "AUTO",
                ok,
                ok ? "Executado com sucesso" : "Falha ao executar",
                logDetalhado
        );

        return 1;
    }

    private boolean runOncePerMinute(PorteiraEletronica p, String acao, LocalDateTime minuteKey) {
        Long id = p.getId();
        String base = (id != null ? String.valueOf(id) : (safe(p.getIp()) + "|" + safe(p.getDescricao())));
        String key = base + "|" + acao + "|" + minuteKey;

        note prev = DEDUP.putIfAbsent(key, new note(LocalDateTime.now(), "first"));
        return prev == null;
    }

    private static LocalTime safeTrunc(LocalTime t) {
        return t == null ? null : t.truncatedTo(ChronoUnit.MINUTES);
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    // mini-estrutura pra permitir evoluir o dedup depois (e.g. debug do motivo/tempo)
    private static class note {
        final LocalDateTime at;
        final String why;
        note(LocalDateTime at, String why) {
            this.at = at;
            this.why = why;
        }
    }
}