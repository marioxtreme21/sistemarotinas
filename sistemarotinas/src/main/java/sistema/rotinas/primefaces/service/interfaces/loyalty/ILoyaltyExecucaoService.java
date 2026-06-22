package sistema.rotinas.primefaces.service.interfaces.loyalty;

import java.time.LocalDate;
import java.util.List;

import sistema.rotinas.primefaces.enums.OrigemExecucaoEnum;
import sistema.rotinas.primefaces.model.loyalty.RotinaExecucaoLoyalty;
import sistema.rotinas.primefaces.model.loyalty.RotinaExecucaoLoyaltyCupom;

public interface ILoyaltyExecucaoService {

    RotinaExecucaoLoyalty executarCargaNormal(List<Long> lojaIds,
                                              LocalDate dataInicial,
                                              LocalDate dataFinal,
                                              boolean selecionarTodas,
                                              OrigemExecucaoEnum origemExecucao);

    RotinaExecucaoLoyalty executarCargaEmLotes(List<Long> lojaIds,
                                               LocalDate dataInicial,
                                               LocalDate dataFinal,
                                               boolean selecionarTodas,
                                               OrigemExecucaoEnum origemExecucao);

    int reprocessarPendencias(List<Long> lojaIds,
                              LocalDate dataInicial,
                              LocalDate dataFinal);

    List<RotinaExecucaoLoyalty> listarHistorico();

    List<RotinaExecucaoLoyaltyCupom> listarPendencias();

    RotinaExecucaoLoyalty executarDiaAnteriorAutomatico();

    RotinaExecucaoLoyalty executarDataReferencia(LocalDate dataReferencia,
                                                 List<Long> lojaIds,
                                                 boolean selecionarTodas,
                                                 OrigemExecucaoEnum origemExecucao);
}