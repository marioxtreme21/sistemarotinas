package sistema.rotinas.primefaces.service.interfaces;

import java.time.LocalDateTime;

import sistema.rotinas.primefaces.enums.EtapaArquivoEnum;
import sistema.rotinas.primefaces.enums.OrigemExecucaoEnum;
import sistema.rotinas.primefaces.enums.StatusExecucaoEnum;
import sistema.rotinas.primefaces.enums.TipoRotinaEnum;
import sistema.rotinas.primefaces.model.Loja;
import sistema.rotinas.primefaces.model.rotina.RotinaExecucao;
import sistema.rotinas.primefaces.model.rotina.RotinaExecucaoArquivo;
import sistema.rotinas.primefaces.model.rotina.RotinaExecucaoLoja;

public interface IRotinaExecucaoService {

    RotinaExecucao iniciarExecucao(TipoRotinaEnum tipo, OrigemExecucaoEnum origem, String solicitante);

    RotinaExecucao finalizarExecucao(Long execucaoId, StatusExecucaoEnum status, String mensagemResumo, String erroGeral);

    RotinaExecucaoLoja iniciarLoja(Long execucaoId, Loja loja);

    RotinaExecucaoLoja finalizarLoja(Long execucaoLojaId, StatusExecucaoEnum status, String mensagem, String erro);

    RotinaExecucaoArquivo iniciarArquivo(Long execucaoId,
                                        Long execucaoLojaId,
                                        Loja loja,
                                        String patternEsperado,
                                        String nomeArquivo,
                                        Boolean required,
                                        EtapaArquivoEnum etapa,
                                        String origem,
                                        String destino);

    RotinaExecucaoArquivo finalizarArquivo(Long execucaoArquivoId,
                                          StatusExecucaoEnum status,
                                          EtapaArquivoEnum etapaFinal,
                                          String mensagem,
                                          String erro,
                                          Long tamanhoOrigemBytes,
                                          Long tamanhoDestinoBytes,
                                          LocalDateTime lastModifiedOrigem,
                                          LocalDateTime lastModifiedDestino);
}
