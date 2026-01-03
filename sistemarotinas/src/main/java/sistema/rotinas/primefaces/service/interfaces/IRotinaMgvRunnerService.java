package sistema.rotinas.primefaces.service.interfaces;

import java.util.List;

import sistema.rotinas.primefaces.enums.OrigemExecucaoEnum;

public interface IRotinaMgvRunnerService {

    Long executar(List<Long> lojaIds, OrigemExecucaoEnum origem, String solicitante);
}
