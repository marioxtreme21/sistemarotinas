package sistema.rotinas.primefaces.service.loyalty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import sistema.rotinas.primefaces.dto.loyalty.LoyaltyApiResponseDTO;
import sistema.rotinas.primefaces.dto.loyalty.LoyaltyCupomOrigemDTO;
import sistema.rotinas.primefaces.dto.loyalty.LoyaltyCupomPayloadDTO;
import sistema.rotinas.primefaces.enums.StatusExecucaoEnum;
import sistema.rotinas.primefaces.model.Loja;
import sistema.rotinas.primefaces.model.loyalty.RotinaExecucaoLoyalty;
import sistema.rotinas.primefaces.model.loyalty.RotinaExecucaoLoyaltyCupom;
import sistema.rotinas.primefaces.model.loyalty.RotinaExecucaoLoyaltyLote;
import sistema.rotinas.primefaces.repository.LojaRepository;
import sistema.rotinas.primefaces.repository.loyalty.RotinaExecucaoLoyaltyCupomRepository;
import sistema.rotinas.primefaces.repository.loyalty.RotinaExecucaoLoyaltyLoteRepository;
import sistema.rotinas.primefaces.repository.loyalty.RotinaExecucaoLoyaltyRepository;

@Service
public class LoyaltyCupomPersistenceService {

    private final RotinaExecucaoLoyaltyCupomRepository cupomRepository;
    private final RotinaExecucaoLoyaltyRepository execucaoRepository;
    private final RotinaExecucaoLoyaltyLoteRepository loteRepository;
    private final LojaRepository lojaRepository;
    private final ObjectMapper objectMapper;

    public LoyaltyCupomPersistenceService(RotinaExecucaoLoyaltyCupomRepository cupomRepository,
                                          RotinaExecucaoLoyaltyRepository execucaoRepository,
                                          RotinaExecucaoLoyaltyLoteRepository loteRepository,
                                          LojaRepository lojaRepository,
                                          ObjectMapper objectMapper) {
        this.cupomRepository = cupomRepository;
        this.execucaoRepository = execucaoRepository;
        this.loteRepository = loteRepository;
        this.lojaRepository = lojaRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RotinaExecucaoLoyaltyCupom salvarResultadoCupom(Long execucaoId,
                                                           Long loteId,
                                                           Long lojaId,
                                                           LoyaltyCupomOrigemDTO venda,
                                                           LoyaltyCupomPayloadDTO payload,
                                                           LoyaltyApiResponseDTO response,
                                                           boolean sucessoEnvio) {
        RotinaExecucaoLoyalty execucao = execucaoRepository.findById(execucaoId).orElseThrow();
        RotinaExecucaoLoyaltyLote lote = loteRepository.findById(loteId).orElseThrow();
        Loja loja = lojaRepository.findById(lojaId).orElseThrow();

        RotinaExecucaoLoyaltyCupom cupom = new RotinaExecucaoLoyaltyCupom();
        cupom.setExecucao(execucao);
        cupom.setLote(lote);
        cupom.setLoja(loja);
        cupom.setDataMovimento(venda.dtMovimento());
        cupom.setIdPdv(venda.idPdv());
        cupom.setNumCupom(venda.numCupom());
        cupom.setIdClienteMd5(payload.idCliente());
        cupom.setPayloadJson(toJsonSeguro(payload));
        cupom.setTentativasEnvio(1);
        cupom.setDataUltimoEnvio(LocalDateTime.now());
        cupom.setHttpStatus(response != null ? response.httpStatus() : null);
        cupom.setMensagem(response != null ? response.responseBody() : null);

        if (sucessoEnvio) {
            cupom.setStatusEnvio(StatusExecucaoEnum.SUCESSO);
            cupom.setReprocessamentoPendente(false);
            cupom.setErro(null);
        } else {
            cupom.setStatusEnvio(StatusExecucaoEnum.FALHA);
            cupom.setReprocessamentoPendente(true);
            cupom.setErro(response != null ? response.erro() : "Falha sem resposta.");
        }

        return cupomRepository.save(cupom);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void salvarResultadosCuponsEmLote(Long execucaoId,
                                             Long loteId,
                                             Long lojaId,
                                             List<LoyaltyCupomOrigemDTO> vendas,
                                             List<LoyaltyCupomPayloadDTO> payloads,
                                             LoyaltyApiResponseDTO response,
                                             boolean sucessoEnvio) {
        if (vendas == null || payloads == null || vendas.size() != payloads.size()) {
            throw new IllegalArgumentException("Vendas e payloads do lote estão inconsistentes.");
        }

        RotinaExecucaoLoyalty execucao = execucaoRepository.findById(execucaoId).orElseThrow();
        RotinaExecucaoLoyaltyLote lote = loteRepository.findById(loteId).orElseThrow();
        Loja loja = lojaRepository.findById(lojaId).orElseThrow();

        List<RotinaExecucaoLoyaltyCupom> registros = new ArrayList<>();

        for (int i = 0; i < vendas.size(); i++) {
            LoyaltyCupomOrigemDTO venda = vendas.get(i);
            LoyaltyCupomPayloadDTO payload = payloads.get(i);

            RotinaExecucaoLoyaltyCupom cupom = new RotinaExecucaoLoyaltyCupom();
            cupom.setExecucao(execucao);
            cupom.setLote(lote);
            cupom.setLoja(loja);
            cupom.setDataMovimento(venda.dtMovimento());
            cupom.setIdPdv(venda.idPdv());
            cupom.setNumCupom(venda.numCupom());
            cupom.setIdClienteMd5(payload.idCliente());
            cupom.setPayloadJson(toJsonSeguro(payload));
            cupom.setTentativasEnvio(1);
            cupom.setDataUltimoEnvio(LocalDateTime.now());
            cupom.setHttpStatus(response != null ? response.httpStatus() : null);
            cupom.setMensagem(response != null ? response.responseBody() : null);

            if (sucessoEnvio) {
                cupom.setStatusEnvio(StatusExecucaoEnum.SUCESSO);
                cupom.setReprocessamentoPendente(false);
                cupom.setErro(null);
            } else {
                cupom.setStatusEnvio(StatusExecucaoEnum.FALHA);
                cupom.setReprocessamentoPendente(true);
                cupom.setErro(response != null ? response.erro() : "Falha sem resposta.");
            }

            registros.add(cupom);
        }

        cupomRepository.saveAll(registros);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void atualizarReprocessamento(Long cupomId,
                                         LoyaltyApiResponseDTO response,
                                         StatusExecucaoEnum statusEnvio,
                                         boolean reprocessamentoPendente,
                                         String erro,
                                         String payloadJson) {
        RotinaExecucaoLoyaltyCupom cupom = cupomRepository.findById(cupomId).orElseThrow();

        int tentativasAtuais = cupom.getTentativasEnvio() == null ? 0 : cupom.getTentativasEnvio();
        cupom.setTentativasEnvio(tentativasAtuais + 1);
        cupom.setDataUltimoEnvio(LocalDateTime.now());
        cupom.setHttpStatus(response != null ? response.httpStatus() : null);
        cupom.setMensagem(response != null ? response.responseBody() : null);
        cupom.setStatusEnvio(statusEnvio);
        cupom.setReprocessamentoPendente(reprocessamentoPendente);
        cupom.setErro(erro);
        cupom.setPayloadJson(payloadJson);

        cupomRepository.save(cupom);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void atualizarReprocessamentoComErro(Long cupomId, String erro) {
        RotinaExecucaoLoyaltyCupom cupom = cupomRepository.findById(cupomId).orElseThrow();

        int tentativasAtuais = cupom.getTentativasEnvio() == null ? 0 : cupom.getTentativasEnvio();
        cupom.setTentativasEnvio(tentativasAtuais + 1);
        cupom.setDataUltimoEnvio(LocalDateTime.now());
        cupom.setStatusEnvio(StatusExecucaoEnum.FALHA);
        cupom.setReprocessamentoPendente(true);
        cupom.setErro(erro);

        cupomRepository.save(cupom);
    }

    private String toJsonSeguro(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return null;
        }
    }
}