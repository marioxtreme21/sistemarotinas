// src/main/java/sistema/ecommerce/primefaces/repository/PdvDoctoPagtoRepositoryImpl.java
package sistema.rotinas.primefaces.repository.oracle;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PdvDoctoPagtoRepositoryImpl implements PdvDoctoPagtoRepository {

    private static final Logger log = LoggerFactory.getLogger(PdvDoctoPagtoRepositoryImpl.class);

    private final JdbcTemplate oracle;

    public PdvDoctoPagtoRepositoryImpl(@Qualifier("oracleExternoJdbcTemplate") JdbcTemplate oracle) {
        this.oracle = oracle;
    }
    
    
    public Long buscarSeqDoctoPorNumeroNfceECheckout(Long numeroNfce, Integer nroCheckout) {
        // Obs.: o nome do campo é com C: NROCHECKOUT. 
        // Se na sua base for "NROCHEKOUT" (sem C), troque no SQL abaixo.
        List<Long> seqs = oracle.queryForList(
            "select seqdocto from pdv_docto where numerodf = ? and nrocheckout = ?",
            new Object[]{numeroNfce, nroCheckout},
            Long.class
        );
        if (seqs == null || seqs.isEmpty()) {
            return null;
        }
        if (seqs.size() > 1) {
            log.warn("Mais de um SEQDOCTO para NUMERODF={} e NROCHECKOUT={}: {}. Usando o primeiro.",
                     numeroNfce, nroCheckout, seqs);
        }
        return seqs.get(0);
    }

    @Override
    public Long buscarSeqDoctoPorNumeroNfce(Long numeroNfce) {
        try {
            return oracle.queryForObject(
                "select seqdocto from pdv_docto where numerodf = ?",
                new Object[]{numeroNfce}, Long.class
            );
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
    

    @Override
    public Integer buscarNroEmpresaPorSeqDocto(Long seqDocto) {
        try {
            return oracle.queryForObject(
                "select nroempresa from pdv_docto where seqdocto = ?",
                new Object[]{seqDocto}, Integer.class
            );
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
    
    

   

 // PdvDoctoPagtoRepositoryImpl
    @Override
    public int inserirLinhaFreteSeNaoExisteClonandoPrincipal(Long seqDocto, BigDecimal valorFrete) {
        final String sql = """
            insert into pdv_doctopagto
                (seqdocto, seqpagto, nroempresa, tipopagto, tipoevento,
                 nroformapagto, vlrlancamento, vlrdesconto, vlracrescimo,
                 dtamovimento, dtahorlancto, codmovimento, qtdedocto,
                 seqoperador, seqsupervisor, statuspagto, codautorizacaotef,
                 dtahorainsercao)
            select
                t.seqdocto,
                (select nvl(max(p.seqpagto),0) + 1
                   from pdv_doctopagto p
                  where p.seqdocto = t.seqdocto) as seqpagto,
                t.nroempresa,
                'R' as tipopagto,
                'R' as tipoevento,
                t.nroformapagto,
                ?                          as vlrlancamento,
                nvl(t.vlrdesconto, 0)      as vlrdesconto,
                nvl(t.vlracrescimo, 0)     as vlracrescimo,
                t.dtamovimento             as dtamovimento,
                t.dtahorlancto             as dtahorlancto,
                4                          as codmovimento,
                t.qtdedocto                as qtdedocto,
                t.seqoperador              as seqoperador,
                t.seqsupervisor            as seqsupervisor,
                t.statuspagto              as statuspagto,
                t.codautorizacaotef        as codautorizacaotef,
                t.dtahorainsercao          as dtahorainsercao
            from pdv_doctopagto t
            where t.seqdocto = ?
              and t.tipoevento = 'P'
              and not exists (
                    select 1
                      from pdv_doctopagto x
                     where x.seqdocto   = t.seqdocto
                       and x.tipoevento = 'R'
                       and x.codmovimento = 4
              )
            fetch first 1 rows only
            """;

        int rows = oracle.update(sql, ps -> {
            ps.setBigDecimal(1, valorFrete);
            ps.setLong(2, seqDocto);
        });

        if (rows == 1) {
            log.info("Linha de frete inserida (idempotente). SEQDOCTO={}, frete={}", seqDocto, valorFrete);
        } else {
            log.info("Linha de frete já existia (codmovimento=4). Nada inserido. SEQDOCTO={}", seqDocto);
        }
        return rows;
    }

    @Override
    public int atualizarPagamentoPrincipalSomandoFrete(Long seqDocto, BigDecimal frete) {
        // 1) Quantas linhas principais existem?
        Integer qtd = oracle.queryForObject(
            "select count(*) from pdv_doctopagto where seqdocto = ? and tipoevento = 'P'",
            new Object[]{seqDocto}, Integer.class
        );
        if (qtd == null || qtd == 0) return 0;

        if (qtd == 1) {
            // 2) Caso simples: só uma linha principal
            return oracle.update(
                "update pdv_doctopagto " +
                "   set vlrlancamento = nvl(vlrlancamento, 0) + ? " +
                " where seqdocto = ? " +
                "   and tipoevento = 'P'",
                frete, seqDocto
            );
        }

        // 3) Há mais de uma linha principal: escolher a de maior VLRLANCAMENTO
        Long seqPagtoMaior = oracle.queryForObject(
            "select seqpagto " +
            "  from (select seqpagto " +
            "          from pdv_doctopagto " +
            "         where seqdocto = ? " +
            "           and tipoevento = 'P' " +
            "         order by nvl(vlrlancamento,0) desc, seqpagto desc) " +
            " where rownum = 1",
            new Object[]{seqDocto}, Long.class
        );

        return oracle.update(
            "update pdv_doctopagto " +
            "   set vlrlancamento = nvl(vlrlancamento, 0) + ? " +
            " where seqdocto = ? " +
            "   and seqpagto = ? " +
            "   and tipoevento = 'P'",
            frete, seqDocto, seqPagtoMaior
        );
    }


	
}
