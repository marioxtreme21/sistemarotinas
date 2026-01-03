package sistema.rotinas.primefaces.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sistema.rotinas.primefaces.enums.OrigemExecucaoEnum;
import sistema.rotinas.primefaces.enums.StatusExecucaoEnum;
import sistema.rotinas.primefaces.enums.TipoRotinaEnum;
import sistema.rotinas.primefaces.model.Loja;
import sistema.rotinas.primefaces.service.interfaces.ILojaService;
import sistema.rotinas.primefaces.service.interfaces.IRotinaExecucaoService;
import sistema.rotinas.primefaces.service.interfaces.IRotinaMgvRunnerService;

/**
 * Runner manual/scheduler do MGV.
 * (Por enquanto stub para manter padrão)
 */
@Service
public class RotinaMgvRunnerService implements IRotinaMgvRunnerService {

    @Autowired
    private IRotinaExecucaoService execucaoService;

    @Autowired
    private ILojaService lojaService;

    @Override
    public Long executar(List<Long> lojaIds, OrigemExecucaoEnum origem, String solicitante) {

        var execucao = execucaoService.iniciarExecucao(TipoRotinaEnum.MGV, origem, solicitante);
        Long execucaoId = execucao.getExecucaoId();

        try {
            List<Loja> lojas;
            if (lojaIds == null || lojaIds.isEmpty()) {
                lojas = lojaService.getAllLojas();
            } else {
                lojas = lojaIds.stream().map(id -> lojaService.findById(id)).toList();
            }

            for (Loja loja : lojas) {
                var el = execucaoService.iniciarLoja(execucaoId, loja);
                execucaoService.finalizarLoja(el.getExecucaoLojaId(), StatusExecucaoEnum.SUCESSO,
                        "Stub: execução MGV ainda não implementada", null);
            }

            execucaoService.finalizarExecucao(execucaoId, StatusExecucaoEnum.SUCESSO,
                    "Stub: execução MGV registrada no histórico (sem execução real)", null);

        } catch (Exception e) {
            execucaoService.finalizarExecucao(execucaoId, StatusExecucaoEnum.FALHA,
                    "Falha ao executar rotina MGV (stub)", e.getMessage());
        }

        return execucaoId;
    }
}
