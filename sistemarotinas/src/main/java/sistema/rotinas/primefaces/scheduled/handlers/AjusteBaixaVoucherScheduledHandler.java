package sistema.rotinas.primefaces.scheduled.handlers;

import java.time.LocalDate;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import sistema.rotinas.primefaces.dto.ResultadoRotinaVoucherDTO;
import sistema.rotinas.primefaces.scheduled.runtime.ScheduledTaskHandler;
import sistema.rotinas.primefaces.scheduled.runtime.TaskExecutionContext;
import sistema.rotinas.primefaces.scheduled.runtime.TaskRunResult;
import sistema.rotinas.primefaces.service.interfaces.IAjusteBaixaVoucherService;

/**
 * Handler DB-Scheduler equivalente ao ScheduledAjusteBaixaVoucherTask.
 *
 * - Default: executa "hoje" (America/Sao_Paulo) como dataInicial e dataFinal.
 *
 * Params JSON opcionais:
 * {
 *   "tag": "AUTO",
 *   "zoneId": "America/Sao_Paulo",
 *   "data": "2026-01-12"          // opcional: força uma data específica (ISO)
 * }
 */
@Component
public class AjusteBaixaVoucherScheduledHandler implements ScheduledTaskHandler {

    public static final String KEY = "ROTINA_VOUCHER_FUN_BAIXA";

    private static final Logger LOG = LoggerFactory.getLogger("SCHEDULER_DB");
    private static final String DEFAULT_ZONE = "America/Sao_Paulo";

    private final IAjusteBaixaVoucherService service;

    public AjusteBaixaVoucherScheduledHandler(IAjusteBaixaVoucherService service) {
        this.service = service;
    }

    @Override
    public String taskKey() {
        return KEY;
    }

    @Override
    public TaskRunResult execute(TaskExecutionContext ctx) {

        String tag = safeTrim(ctx != null ? ctx.paramString("tag", "AUTO") : "AUTO");
        if (tag == null) tag = "AUTO";

        String zoneIdStr = safeTrim(ctx != null ? ctx.paramString("zoneId", DEFAULT_ZONE) : DEFAULT_ZONE);
        if (zoneIdStr == null) zoneIdStr = DEFAULT_ZONE;

        ZoneId zone;
        try {
            zone = ZoneId.of(zoneIdStr);
        } catch (Exception e) {
            zone = ZoneId.of(DEFAULT_ZONE);
            LOG.warn("[HANDLER][VOUCHER] zoneId inválido='{}'. Usando default={}.", zoneIdStr, DEFAULT_ZONE);
        }

        LocalDate hoje = LocalDate.now(zone);

        // permite forçar data via JSON (ótimo p/ testes)
        LocalDate dataRef = hoje;
        try {
            String dataIso = safeTrim(ctx != null ? ctx.paramString("data", null) : null);
            if (dataIso != null) {
                dataRef = LocalDate.parse(dataIso); // ISO-8601: yyyy-MM-dd
            }
        } catch (Exception e) {
            LOG.warn("[HANDLER][VOUCHER] data inválida no JSON. Usando hoje={} (zone={}).", hoje, zone.getId());
            dataRef = hoje;
        }

        try {
            LOG.info("[HANDLER][VOUCHER] Início. tag={} data={} zone={}", tag, dataRef, zone.getId());

            ResultadoRotinaVoucherDTO r = service.executar(dataRef, dataRef);

            // monta um resumo seguro (sem NPE caso algum getter retorne null)
            String resumo =
                    "Baixa Voucher executada. " +
                    "data=" + dataRef +
                    " lidos=" + nvl(r != null ? r.getTotalLidosEconect() : null) +
                    " inseridos=" + nvl(r != null ? r.getTotalMovInseridos() : null) +
                    " atualizados=" + nvl(r != null ? r.getTotalMovAtualizados() : null) +
                    " jaExistentes=" + nvl(r != null ? r.getTotalMovJaExistentes() : null) +
                    " jaExistentesSemAlt=" + nvl(r != null ? r.getTotalMovJaExistentesSemAlteracao() : null) +
                    " cpfNaoEncontrado=" + nvl(r != null ? r.getTotalClientesNaoEncontrados() : null) +
                    " marcados99=" + nvl(r != null ? r.getTotalClientesMarcadosSituacao99() : null) +
                    " tag=" + tag;

            LOG.info("[HANDLER][VOUCHER] OK. {}", resumo);
            return TaskRunResult.ok(resumo);

        } catch (Exception e) {
            LOG.error("[HANDLER][VOUCHER] FAIL. tag={} data={} msg={}", tag, dataRef, e.getMessage(), e);
            return TaskRunResult.fail("Falha Baixa Voucher: " + e.getMessage());
        }
    }

    private static String safeTrim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static long nvl(Long v) {
        return v == null ? 0L : v.longValue();
    }

    private static long nvl(Integer v) {
        return v == null ? 0L : v.longValue();
    }
}