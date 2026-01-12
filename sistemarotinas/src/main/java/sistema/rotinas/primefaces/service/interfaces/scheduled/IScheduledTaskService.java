package sistema.rotinas.primefaces.service.interfaces.scheduled;

import java.time.LocalDateTime;
import java.util.List;

import sistema.rotinas.primefaces.model.scheduled.ScheduledTaskConfig;

public interface IScheduledTaskService {

    // =========================================================
    // ===== Listagem base
    // =========================================================

    List<ScheduledTaskConfig> listarTodas();

    /**
     * Ex.: "PRICE" | "MGV" | "TARA" | "ALL"
     *
     * Estratégia:
     * - "ALL" ignora filtro
     * - Caso contrário, aceita os dois padrões:
     *   - PRICE_*
     *   - ROTINA_PRICE_*
     */
    List<ScheduledTaskConfig> listarPorRotina(String rotina);

    /**
     * Lista todas as linhas (janelas) para um taskKey.
     * Importante: task_key NÃO é unique, então podem existir múltiplas linhas.
     */
    List<ScheduledTaskConfig> listarPorTaskKey(String taskKey);

    ScheduledTaskConfig findById(Long id);

    // =========================================================
    // ===== Pesquisa (tela)
    // =========================================================

    /**
     * Pesquisa para a tela (rotina/owner/ativas/texto).
     *
     * Regras:
     * - rotina: "ALL" ignora; senão aplica prefixo (aceita PRICE_* e ROTINA_PRICE_*)
     * - owner: null/blank ignora
     * - somenteAtivas=true aplica enabled=true
     * - texto: filtra (taskKey ou descricao) via contains
     */
    List<ScheduledTaskConfig> pesquisar(String rotina,
                                       String owner,
                                       boolean somenteAtivas,
                                       String texto);

    /**
     * Para combos da tela (carregar do banco).
     * Retorna os task_key distintos cadastrados.
     */
    List<String> listarTaskKeysCadastradas();

    // =========================================================
    // ===== Persistência / validação
    // =========================================================

    void validar(ScheduledTaskConfig t);

    ScheduledTaskConfig salvar(ScheduledTaskConfig t);

    void atualizarEnabled(Long id, boolean enabled);

    void atualizarEnabledEmLote(List<Long> ids, boolean enabled);

    // =========================================================
    // ===== JSON / Cron
    // =========================================================

    void validarJson(String paramsJson);

    List<LocalDateTime> previewNextRuns(String cronExpr, String zoneId, int total);
}