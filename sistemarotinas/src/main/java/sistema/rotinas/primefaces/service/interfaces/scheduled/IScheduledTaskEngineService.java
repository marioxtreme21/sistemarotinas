package sistema.rotinas.primefaces.service.interfaces.scheduled;

public interface IScheduledTaskEngineService {

    /**
     * Executa a task imediatamente (sincrono), gravando:
     * - running/lastStartedAt/lastFinishedAt
     * - lastStatus/lastMessage/lastError
     * - nextRunAt (recalculado a partir do cron)
     * - okCount/failCount
     *
     * Lança IllegalArgumentException se id não existir.
     * Pode lançar IllegalStateException se estiver running ou desabilitada (dependendo do claim).
     */
    void executarUmaVez(Long scheduledTaskId);
}