package sistema.rotinas.primefaces.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sistema.rotinas.primefaces.enums.EtapaArquivoEnum;
import sistema.rotinas.primefaces.enums.OrigemExecucaoEnum;
import sistema.rotinas.primefaces.enums.StatusExecucaoEnum;
import sistema.rotinas.primefaces.enums.TipoRotinaEnum;
import sistema.rotinas.primefaces.model.Loja;
import sistema.rotinas.primefaces.model.rotina.RotinaExecucao;
import sistema.rotinas.primefaces.model.rotina.RotinaExecucaoArquivo;
import sistema.rotinas.primefaces.model.rotina.RotinaExecucaoLoja;
import sistema.rotinas.primefaces.repository.RotinaExecucaoArquivoRepository;
import sistema.rotinas.primefaces.repository.RotinaExecucaoLojaRepository;
import sistema.rotinas.primefaces.repository.RotinaExecucaoRepository;
import sistema.rotinas.primefaces.service.interfaces.IRotinaExecucaoService;

@Service
public class RotinaExecucaoService implements IRotinaExecucaoService {

    @Autowired
    private RotinaExecucaoRepository execucaoRepo;

    @Autowired
    private RotinaExecucaoLojaRepository execucaoLojaRepo;

    @Autowired
    private RotinaExecucaoArquivoRepository execucaoArquivoRepo;

    @Override
    @Transactional
    public RotinaExecucao iniciarExecucao(TipoRotinaEnum tipo, OrigemExecucaoEnum origem, String solicitante) {
        RotinaExecucao e = new RotinaExecucao();
        e.setTipoRotina(tipo);
        e.setOrigemExecucao(origem);
        e.setSolicitante(solicitante);
        e.setStatus(StatusExecucaoEnum.EM_ANDAMENTO);
        e.setInicioEm(LocalDateTime.now());
        return execucaoRepo.save(e);
    }

    @Override
    @Transactional
    public RotinaExecucao finalizarExecucao(Long execucaoId, StatusExecucaoEnum status, String mensagemResumo, String erroGeral) {
        RotinaExecucao e = execucaoRepo.findById(execucaoId)
                .orElseThrow(() -> new IllegalArgumentException("Execução não encontrada: " + execucaoId));

        e.setFimEm(LocalDateTime.now());
        if (e.getInicioEm() != null) {
            long ms = java.time.Duration.between(e.getInicioEm(), e.getFimEm()).toMillis();
            e.setTempoTotalMs(ms);
        }
        e.setStatus(status);
        e.setMensagemResumo(mensagemResumo);
        e.setErroGeral(erroGeral);

        return execucaoRepo.save(e);
    }

    @Override
    @Transactional
    public RotinaExecucaoLoja iniciarLoja(Long execucaoId, Loja loja) {
        RotinaExecucao execucao = execucaoRepo.findById(execucaoId)
                .orElseThrow(() -> new IllegalArgumentException("Execução não encontrada: " + execucaoId));

        RotinaExecucaoLoja el = new RotinaExecucaoLoja();
        el.setExecucao(execucao);
        el.setLoja(loja);
        el.setInicioEm(LocalDateTime.now());
        el.setStatus(StatusExecucaoEnum.EM_ANDAMENTO);

        if (loja != null) {
            el.setCodLojaRms(loja.getCodLojaRms());
            el.setNomeLoja(loja.getNome());
        }

        execucao.setTotalLojas(safeInt(execucao.getTotalLojas()) + 1);
        execucaoRepo.save(execucao);

        return execucaoLojaRepo.save(el);
    }

    @Override
    @Transactional
    public RotinaExecucaoLoja finalizarLoja(Long execucaoLojaId, StatusExecucaoEnum status, String mensagem, String erro) {
        RotinaExecucaoLoja el = execucaoLojaRepo.findById(execucaoLojaId)
                .orElseThrow(() -> new IllegalArgumentException("Execução-loja não encontrada: " + execucaoLojaId));

        el.setFimEm(LocalDateTime.now());
        if (el.getInicioEm() != null) {
            long ms = java.time.Duration.between(el.getInicioEm(), el.getFimEm()).toMillis();
            el.setTempoTotalMs(ms);
        }

        el.setStatus(status);
        el.setMensagem(mensagem);
        el.setErro(erro);

        RotinaExecucao execucao = el.getExecucao();
        if (execucao != null) {
            if (status == StatusExecucaoEnum.SUCESSO) {
                execucao.setLojasSucesso(safeInt(execucao.getLojasSucesso()) + 1);
            } else if (status == StatusExecucaoEnum.FALHA) {
                execucao.setLojasFalha(safeInt(execucao.getLojasFalha()) + 1);
            } else if (status == StatusExecucaoEnum.FALHA_PARCIAL) {
                // se quiser, aqui você pode contar parcial em "falha" ou criar um campo lojasParcial depois
            }
            execucaoRepo.save(execucao);
        }

        return execucaoLojaRepo.save(el);
    }

    @Override
    @Transactional
    public RotinaExecucaoArquivo iniciarArquivo(Long execucaoId,
                                               Long execucaoLojaId,
                                               Loja loja,
                                               String patternEsperado,
                                               String nomeArquivo,
                                               Boolean required,
                                               EtapaArquivoEnum etapa,
                                               String origem,
                                               String destino) {

        RotinaExecucao execucao = execucaoRepo.findById(execucaoId)
                .orElseThrow(() -> new IllegalArgumentException("Execução não encontrada: " + execucaoId));

        RotinaExecucaoLoja el = null;
        if (execucaoLojaId != null) {
            el = execucaoLojaRepo.findById(execucaoLojaId)
                    .orElseThrow(() -> new IllegalArgumentException("Execução-loja não encontrada: " + execucaoLojaId));
        }

        RotinaExecucaoArquivo ea = new RotinaExecucaoArquivo();
        ea.setExecucao(execucao);
        ea.setExecucaoLoja(el);
        ea.setLoja(loja);

        if (loja != null) {
            ea.setCodLojaRms(loja.getCodLojaRms());
        } else if (el != null) {
            ea.setCodLojaRms(el.getCodLojaRms());
        }

        ea.setPatternEsperado(patternEsperado);
        ea.setNomeArquivo(nomeArquivo);
        ea.setRequired(required != null ? required : Boolean.TRUE);
        ea.setEtapa(etapa != null ? etapa : EtapaArquivoEnum.VALIDACAO_ARQUIVOS);
        ea.setStatus(StatusExecucaoEnum.EM_ANDAMENTO);
        ea.setOrigem(origem);
        ea.setDestino(destino);
        ea.setInicioEm(LocalDateTime.now());

        // novo campo: começa como "não verificado"
        ea.setOrigemAtualizada(null);

        execucao.setTotalArquivos(safeInt(execucao.getTotalArquivos()) + 1);
        execucaoRepo.save(execucao);

        return execucaoArquivoRepo.save(ea);
    }

    @Override
    @Transactional
    public RotinaExecucaoArquivo finalizarArquivo(Long execucaoArquivoId,
                                                 StatusExecucaoEnum status,
                                                 EtapaArquivoEnum etapaFinal,
                                                 String mensagem,
                                                 String erro,
                                                 Long tamanhoOrigemBytes,
                                                 Long tamanhoDestinoBytes,
                                                 LocalDateTime lastModifiedOrigem,
                                                 LocalDateTime lastModifiedDestino) {

        RotinaExecucaoArquivo ea = execucaoArquivoRepo.findById(execucaoArquivoId)
                .orElseThrow(() -> new IllegalArgumentException("Execução-arquivo não encontrada: " + execucaoArquivoId));

        ea.setFimEm(LocalDateTime.now());
        if (ea.getInicioEm() != null) {
            long ms = java.time.Duration.between(ea.getInicioEm(), ea.getFimEm()).toMillis();
            ea.setTempoTotalMs(ms);
        }

        ea.setStatus(status != null ? status : StatusExecucaoEnum.FALHA);
        if (etapaFinal != null) ea.setEtapa(etapaFinal);

        ea.setMensagem(mensagem);
        ea.setErro(erro);
        ea.setTamanhoOrigemBytes(tamanhoOrigemBytes);
        ea.setTamanhoDestinoBytes(tamanhoDestinoBytes);
        ea.setLastModifiedOrigem(lastModifiedOrigem);
        ea.setLastModifiedDestino(lastModifiedDestino);

        // ✅ NOVO: calcula origemAtualizada aqui (não interrompe nada)
        ea.setOrigemAtualizada(calcularOrigemAtualizada(ea.getExecucao(), lastModifiedOrigem));

        RotinaExecucao execucao = ea.getExecucao();
        if (execucao != null) {
            if (ea.getStatus() == StatusExecucaoEnum.SUCESSO) {
                execucao.setArquivosSucesso(safeInt(execucao.getArquivosSucesso()) + 1);
            } else if (ea.getStatus() == StatusExecucaoEnum.FALHA) {
                execucao.setArquivosFalha(safeInt(execucao.getArquivosFalha()) + 1);
            } else if (ea.getStatus() == StatusExecucaoEnum.FALHA_PARCIAL) {
                // idem: se quiser contar parcial depois, você cria campos específicos no cabeçalho
            }
            execucaoRepo.save(execucao);
        }

        return execucaoArquivoRepo.save(ea);
    }

    private Boolean calcularOrigemAtualizada(RotinaExecucao execucao, LocalDateTime lastModifiedOrigem) {
        if (execucao == null || execucao.getInicioEm() == null) return null;
        if (lastModifiedOrigem == null) return null;

        LocalDate dataExecucao = execucao.getInicioEm().toLocalDate();
        LocalDate dataOrigem = lastModifiedOrigem.toLocalDate();

        return dataOrigem.isEqual(dataExecucao);
    }

    private static int safeInt(Integer v) {
        return v == null ? 0 : v.intValue();
    }
}
