package sistema.rotinas.primefaces.repository.oracle;

import java.math.BigDecimal;

public interface PdvDoctoPagtoRepository {
	
	
	Long buscarSeqDoctoPorNumeroNfce(Long numeroNfce);

    Integer buscarNroEmpresaPorSeqDocto(Long seqDocto); // útil para auditoria/relatórios (opcional)


    /**
     * Insere a linha de FRETE clonando a linha principal (TIPOEVENTO='P') do mesmo SEQDOCTO,
     * alterando apenas TIPOPAGTO='R', TIPOEVENTO='R', VLRLANCAMENTO=:valorFrete, CODMOVIMENTO=4
     * e SEQPAGTO=MAX+1.
     */
    int atualizarPagamentoPrincipalSomandoFrete(Long seqDocto, BigDecimal valorFrete);
    
 // PdvDoctoPagtoRepository
    int inserirLinhaFreteSeNaoExisteClonandoPrincipal(Long seqDocto, BigDecimal valorFrete);
    
    Long buscarSeqDoctoPorNumeroNfceECheckout(Long numeroNfce, Integer nroCheckout);
    
 
    
    
}
