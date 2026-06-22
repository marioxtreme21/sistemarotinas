package sistema.rotinas.primefaces.scheduled.handlers.loyalty;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import sistema.rotinas.primefaces.enums.OrigemExecucaoEnum;
import sistema.rotinas.primefaces.model.loyalty.RotinaExecucaoLoyalty;
import sistema.rotinas.primefaces.scheduled.runtime.ScheduledTaskHandler;
import sistema.rotinas.primefaces.scheduled.runtime.TaskExecutionContext;
import sistema.rotinas.primefaces.scheduled.runtime.TaskRunResult;
import sistema.rotinas.primefaces.service.interfaces.loyalty.ILoyaltyExecucaoService;

@Component
public class LoyaltyScheduledHandler implements ScheduledTaskHandler {

    public static final String KEY = "ROTINA_LOYALTY_DIA_ANTERIOR";

    private final ILoyaltyExecucaoService loyaltyExecucaoService;

    public LoyaltyScheduledHandler(ILoyaltyExecucaoService loyaltyExecucaoService) {
        this.loyaltyExecucaoService = loyaltyExecucaoService;
    }

    @Override
    public String taskKey() {
        return KEY;
    }

    @Override
    public TaskRunResult execute(TaskExecutionContext ctx) {
        try {
            LocalDate dataReferencia = resolverDataReferencia(ctx);
            List<Long> lojaIds = resolverLojaIds(ctx);
            boolean selecionarTodas = (lojaIds == null || lojaIds.isEmpty());

            RotinaExecucaoLoyalty execucao = loyaltyExecucaoService.executarDataReferencia(
                    dataReferencia,
                    lojaIds,
                    selecionarTodas,
                    OrigemExecucaoEnum.AUTOMATICA
            );

            return TaskRunResult.ok(
                    "LOYALTY executado com sucesso. execucaoId=" + execucao.getExecucaoLoyaltyId()
                            + " data=" + dataReferencia
                            + " lojas=" + (selecionarTodas ? "ALL" : lojaIds.size())
            );

        } catch (IllegalArgumentException e) {
            return TaskRunResult.skipped(e.getMessage());
        } catch (Exception e) {
            return TaskRunResult.fail("Falha ao executar rotina Loyalty: " + e.getMessage(), e.getMessage());
        }
    }

    private LocalDate resolverDataReferencia(TaskExecutionContext ctx) {
        String dataStr = ctx != null ? ctx.paramString("dataReferencia", null) : null;
        if (dataStr == null || dataStr.isBlank()) {
            return LocalDate.now().minusDays(1);
        }
        return LocalDate.parse(dataStr.trim());
    }

    private List<Long> resolverLojaIds(TaskExecutionContext ctx) {
        String lojaIdsStr = ctx != null ? ctx.paramString("lojaIds", null) : null;
        if (lojaIdsStr == null || lojaIdsStr.isBlank()) {
            return List.of();
        }

        String[] partes = lojaIdsStr.split(",");
        List<Long> ids = new ArrayList<>();

        for (String parte : partes) {
            if (parte == null || parte.isBlank()) continue;
            try {
                ids.add(Long.valueOf(parte.trim()));
            } catch (NumberFormatException ignored) {
            }
        }

        return ids;
    }
}