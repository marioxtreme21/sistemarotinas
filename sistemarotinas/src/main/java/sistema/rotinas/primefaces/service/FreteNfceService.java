package sistema.rotinas.primefaces.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sistema.rotinas.primefaces.dto.PedidoFreteDTO;
import sistema.rotinas.primefaces.repository.mysql.MovEcoRepository;
import sistema.rotinas.primefaces.repository.oracle.PdvDoctoPagtoRepository;

@Service
public class FreteNfceService {

    private static final Logger log = LoggerFactory.getLogger(FreteNfceService.class);

    private final MovEcoRepository movEcoRepository;
    private final PdvDoctoPagtoRepository pdvDoctoPagtoRepository;

    public FreteNfceService(MovEcoRepository movEcoRepository,
                            PdvDoctoPagtoRepository pdvDoctoPagtoRepository) {
        this.movEcoRepository = movEcoRepository;
        this.pdvDoctoPagtoRepository = pdvDoctoPagtoRepository;
    }

    public void processar(LocalDate dataInicial, BigDecimal freteMinimo) {
        // A query do MySQL já considera frete > freteMinimo; passe ZERO para pegar apenas > 0.
        List<PedidoFreteDTO> pedidos = movEcoRepository.listarPedidosComFrete(dataInicial, freteMinimo);
        log.info("Pedidos com frete > {} a partir de {}: {}", freteMinimo, dataInicial, pedidos.size());

        for (PedidoFreteDTO p : pedidos) {
            try {
                processarPedido(p);
            } catch (Exception e) {
                log.error("Erro processando NFCE {}: {}", p.getNumeroNfce(), e.getMessage(), e);
            }
        }
    }

    @Transactional("oracleExternoTransactionManager")
    protected void processarPedido(PedidoFreteDTO p) {
        // ⬇️ trocado para buscar por NUMERODF e NROCHECKOUT (numeroPdv)
        Long seqDocto = pdvDoctoPagtoRepository
                .buscarSeqDoctoPorNumeroNfceECheckout(p.getNumeroNfce(), p.getNumeroPdv());

        if (seqDocto == null) {
            log.warn("PDV_DOCTO não encontrado para NUMERODF={} e NROCHECKOUT={}", 
                     p.getNumeroNfce(), p.getNumeroPdv());
            return;
        }

        // 1) tenta inserir a linha de frete só se não existir (idempotente)
        int inseridas = pdvDoctoPagtoRepository
                .inserirLinhaFreteSeNaoExisteClonandoPrincipal(seqDocto, p.getValorFrete());

        // 2) se inseriu, então soma no principal; se não, não mexe (evita duplicar)
        if (inseridas == 1) {
            int u = pdvDoctoPagtoRepository
                    .atualizarPagamentoPrincipalSomandoFrete(seqDocto, p.getValorFrete());
            log.info("Pagamento principal atualizado (+{}). SEQDOCTO={}, linhas={}", 
                     p.getValorFrete(), seqDocto, u);
        } else {
            log.info("Ajuste já aplicado anteriormente. SEQDOCTO={} — pulando soma no principal.", seqDocto);
        }
    }
    
    
    
}
