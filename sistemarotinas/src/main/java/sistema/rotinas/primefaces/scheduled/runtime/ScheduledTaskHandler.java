package sistema.rotinas.primefaces.scheduled.runtime;

public interface ScheduledTaskHandler {

    /**
     * Chave do handler (ex: PRICE_AUTO) — deve bater com scheduled_task_config.task_key
     * (no sentido: a config aponta qual handler executar).
     */
    String taskKey();

    /**
     * Execução da tarefa (padrão).
     */
    TaskRunResult execute(TaskExecutionContext ctx);

    /**
     * Alias para compatibilidade com código antigo (engine chamando run()).
     */
    default TaskRunResult run(TaskExecutionContext ctx) {
        return execute(ctx);
    }

    /**
     * Alias para compatibilidade com código antigo (engine chamando handle()).
     */
    default TaskRunResult handle(TaskExecutionContext ctx) {
        return execute(ctx);
    }
}