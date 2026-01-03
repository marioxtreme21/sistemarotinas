package sistema.rotinas.primefaces.service.interfaces;

import java.util.List;

import sistema.rotinas.primefaces.enums.OrigemExecucaoEnum;

public interface IRotinaPriceRunnerService {

    /**
     * Executa manual ou scheduler.
     * - lojaIds vazio/null => executar todas.
     * Retorna execucaoId para consulta do histórico.
     */
    Long executar(List<Long> lojaIds, OrigemExecucaoEnum origem, String solicitante);
}
