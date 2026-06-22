package sistema.rotinas.primefaces.service.loyalty;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import sistema.rotinas.primefaces.enums.OrigemExecucaoEnum;
import sistema.rotinas.primefaces.enums.StatusExecucaoEnum;
import sistema.rotinas.primefaces.model.loyalty.RotinaExecucaoLoyalty;
import sistema.rotinas.primefaces.repository.loyalty.RotinaExecucaoLoyaltyRepository;

@Service
public class LoyaltyExecucaoPersistenceService {

    private final RotinaExecucaoLoyaltyRepository execucaoRepository;

    public LoyaltyExecucaoPersistenceService(RotinaExecucaoLoyaltyRepository execucaoRepository) {
        this.execucaoRepository = execucaoRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RotinaExecucaoLoyalty criarExecucao(OrigemExecucaoEnum origemExecucao,
                                               boolean selecionarTodas,
                                               LocalDate dataInicial,
                                               LocalDate dataFinal,
                                               LocalDateTime inicioEm) {
        RotinaExecucaoLoyalty execucao = new RotinaExecucaoLoyalty();
        execucao.setOrigemExecucao(origemExecucao);
        execucao.setSelecionarTodas(selecionarTodas);
        execucao.setDataInicial(dataInicial);
        execucao.setDataFinal(dataFinal);
        execucao.setInicioEm(inicioEm);
        execucao.setStatus(StatusExecucaoEnum.SUCESSO);
        return execucaoRepository.save(execucao);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RotinaExecucaoLoyalty finalizarExecucao(Long execucaoId,
                                                   Integer totalLojas,
                                                   Integer totalLotes,
                                                   Integer totalCuponsConsultados,
                                                   Integer totalCuponsEnviados,
                                                   Integer totalCuponsFalha,
                                                   StatusExecucaoEnum status,
                                                   String mensagemResumo,
                                                   String erroGeral,
                                                   LocalDateTime fimEm,
                                                   Long tempoTotalMs) {
        RotinaExecucaoLoyalty execucao = execucaoRepository.findById(execucaoId).orElseThrow();

        if (totalLojas != null) execucao.setTotalLojas(totalLojas);
        if (totalLotes != null) execucao.setTotalLotes(totalLotes);
        if (totalCuponsConsultados != null) execucao.setTotalCuponsConsultados(totalCuponsConsultados);
        if (totalCuponsEnviados != null) execucao.setTotalCuponsEnviados(totalCuponsEnviados);
        if (totalCuponsFalha != null) execucao.setTotalCuponsFalha(totalCuponsFalha);

        execucao.setStatus(status);
        execucao.setMensagemResumo(mensagemResumo);
        execucao.setErroGeral(erroGeral);
        execucao.setFimEm(fimEm);
        execucao.setTempoTotalMs(tempoTotalMs);

        return execucaoRepository.save(execucao);
    }
}