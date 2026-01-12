package sistema.rotinas.primefaces.repository.scheduled;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import sistema.rotinas.primefaces.model.scheduled.ScheduledTaskConfig;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduledTaskConfigRepository extends JpaRepository<ScheduledTaskConfig, Long> {

    // =========================================================
    // ✅ Base: engine DB scheduler
    // =========================================================

    @Query("""
        select t from ScheduledTaskConfig t
        where t.enabled = true
          and t.running = false
          and t.nextRunAt is not null
          and t.nextRunAt <= :now
          and (t.owner = 'ALL' or t.owner in :owners)
        order by t.nextRunAt asc
    """)
    List<ScheduledTaskConfig> findDue(@Param("now") LocalDateTime now,
                                     @Param("owners") List<String> owners,
                                     Pageable pageable);

    /**
     * Claim atômico: só 1 instância (DEV/PROD) consegue marcar como running=true.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
        update ScheduledTaskConfig t
           set t.running = true,
               t.lastStartedAt = :now,
               t.lastStatus = null,
               t.lastMessage = null,
               t.lastError = null
         where t.id = :id
           and t.enabled = true
           and t.running = false
    """)
    int claim(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
        update ScheduledTaskConfig t
           set t.running = false,
               t.lastFinishedAt = :now,
               t.lastStatus = :status,
               t.lastMessage = :message,
               t.lastError = :error,
               t.nextRunAt = :nextRun,
               t.okCount = t.okCount + :okInc,
               t.failCount = t.failCount + :failInc
         where t.id = :id
    """)
    int finish(@Param("id") Long id,
               @Param("now") LocalDateTime now,
               @Param("status") String status,
               @Param("message") String message,
               @Param("error") String error,
               @Param("nextRun") LocalDateTime nextRun,
               @Param("okInc") long okInc,
               @Param("failInc") long failInc);

    // =========================================================
    // ✅ Suporte à tela / service
    // =========================================================

    /**
     * Útil para filtro por rotina via prefixo:
     * PRICE_  / MGV_ / TARA_
     */
    List<ScheduledTaskConfig> findByTaskKeyStartingWithIgnoreCase(String prefix);

    /**
     * Útil para validação/edição por chave (caso você trate "taskKey único").
     * OBS: no seu modelo novo task_key NÃO é unique (várias janelas),
     * então este método vira "pega qualquer uma".
     * Pode manter, mas use com cuidado.
     */
    Optional<ScheduledTaskConfig> findByTaskKeyIgnoreCase(String taskKey);

    /**
     * ✅ Para a pesquisa por task do banco (todas as janelas daquele taskKey).
     */
    List<ScheduledTaskConfig> findByTaskKeyIgnoreCaseOrderByIdAsc(String taskKey);

    /**
     * ✅ Para selectOneMenu da pesquisa:
     * lista os task_key distintos cadastrados (já configurados).
     */
    @Query("""
        select distinct t.taskKey
          from ScheduledTaskConfig t
         where t.taskKey is not null and trim(t.taskKey) <> ''
         order by t.taskKey
    """)
    List<String> listDistinctTaskKeys();

    /**
     * (Opcional) Se quiser popular owner do banco também.
     */
    @Query("""
        select distinct t.owner
          from ScheduledTaskConfig t
         where t.owner is not null and trim(t.owner) <> ''
         order by t.owner
    """)
    List<String> listDistinctOwners();

    // =========================================================
    // ✅ Diagnóstico de owner NULL/blank (para log)
    // =========================================================

    @Query("""
        select count(t) from ScheduledTaskConfig t
        where t.owner is null or trim(t.owner) = ''
    """)
    long countOwnerNullOrBlank();

    @Query("""
        select t from ScheduledTaskConfig t
        where t.owner is null or trim(t.owner) = ''
        order by t.id asc
    """)
    List<ScheduledTaskConfig> listOwnerNullOrBlank(Pageable pageable);

    /**
     * (Opcional) corrige owner NULL/blank para um valor padrão.
     * Use com cuidado.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
        update ScheduledTaskConfig t
           set t.owner = :newOwner
         where t.owner is null or trim(t.owner) = ''
    """)
    int fixOwnerNullOrBlank(@Param("newOwner") String newOwner);
}