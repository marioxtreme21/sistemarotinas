package sistema.rotinas.primefaces.service.interfaces;

import sistema.rotinas.primefaces.model.rotina.RotinaExecucao;

public interface INotificacaoRotinaService {

    void notificarExecucaoFinalizada(Long execucaoId);

    void notificarExecucaoFinalizada(RotinaExecucao execucao);
}
