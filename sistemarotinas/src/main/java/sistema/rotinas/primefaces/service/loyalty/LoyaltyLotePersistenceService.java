package sistema.rotinas.primefaces.service.loyalty;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import sistema.rotinas.primefaces.enums.StatusExecucaoEnum;
import sistema.rotinas.primefaces.model.Loja;
import sistema.rotinas.primefaces.model.loyalty.RotinaExecucaoLoyalty;
import sistema.rotinas.primefaces.model.loyalty.RotinaExecucaoLoyaltyLote;
import sistema.rotinas.primefaces.repository.LojaRepository;
import sistema.rotinas.primefaces.repository.loyalty.RotinaExecucaoLoyaltyLoteRepository;
import sistema.rotinas.primefaces.repository.loyalty.RotinaExecucaoLoyaltyRepository;

@Service
public class LoyaltyLotePersistenceService {

    private final RotinaExecucaoLoyaltyLoteRepository loteRepository;
    private final RotinaExecucaoLoyaltyRepository execucaoRepository;
    private final LojaRepository lojaRepository;

    public LoyaltyLotePersistenceService(RotinaExecucaoLoyaltyLoteRepository loteRepository,
                                         RotinaExecucaoLoyaltyRepository execucaoRepository,
                                         LojaRepository lojaRepository) {
        this.loteRepository = loteRepository;
        this.execucaoRepository = execucaoRepository;
        this.lojaRepository = lojaRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RotinaExecucaoLoyaltyLote criarLote(Long execucaoId,
                                               Long lojaId,
                                               LocalDate dataMovimento,
                                               LocalDateTime inicioEm) {
        RotinaExecucaoLoyalty execucao = execucaoRepository.findById(execucaoId).orElseThrow();
        Loja loja = lojaRepository.findById(lojaId).orElseThrow();

        RotinaExecucaoLoyaltyLote lote = new RotinaExecucaoLoyaltyLote();
        lote.setExecucao(execucao);
        lote.setLoja(loja);
        lote.setDataMovimento(dataMovimento);
        lote.setInicioEm(inicioEm);
        lote.setStatus(StatusExecucaoEnum.SUCESSO);

        return loteRepository.save(lote);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RotinaExecucaoLoyaltyLote finalizarLote(Long loteId,
                                                   Integer qtdCuponsConsultados,
                                                   Integer qtdCuponsEnviados,
                                                   Integer qtdCuponsFalha,
                                                   Integer qtdPendentesReprocessamento,
                                                   StatusExecucaoEnum status,
                                                   String mensagemResumo,
                                                   LocalDateTime fimEm,
                                                   Long tempoTotalMs) {
        RotinaExecucaoLoyaltyLote lote = loteRepository.findById(loteId).orElseThrow();

        lote.setQtdCuponsConsultados(qtdCuponsConsultados);
        lote.setQtdCuponsEnviados(qtdCuponsEnviados);
        lote.setQtdCuponsFalha(qtdCuponsFalha);
        lote.setQtdPendentesReprocessamento(qtdPendentesReprocessamento);
        lote.setStatus(status);
        lote.setMensagemResumo(mensagemResumo);
        lote.setFimEm(fimEm);
        lote.setTempoTotalMs(tempoTotalMs);

        return loteRepository.save(lote);
    }
}