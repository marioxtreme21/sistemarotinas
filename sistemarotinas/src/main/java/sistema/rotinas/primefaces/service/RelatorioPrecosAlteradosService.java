package sistema.rotinas.primefaces.service;

import java.io.File;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import sistema.rotinas.primefaces.dto.RelatorioPrecosAlteradosDTO;
import sistema.rotinas.primefaces.model.Loja;
import sistema.rotinas.primefaces.repository.LojaRepository;
import sistema.rotinas.primefaces.service.interfaces.IRelatorioPrecosAlteradosService;
import sistema.rotinas.primefaces.util.PastaUploadUtil;
import sistema.rotinas.primefaces.util.RelatorioUtil;

@Service
public class RelatorioPrecosAlteradosService implements IRelatorioPrecosAlteradosService {

    private static final Logger log = LoggerFactory.getLogger(RelatorioPrecosAlteradosService.class);

    @Autowired
    @Qualifier("oracleExternoJdbcTemplate")
    private JdbcTemplate oracleExternoJdbcTemplate;

    @Autowired
    private LojaRepository lojaRepository;

    private static final String SQL = """
        WITH base AS (
          /* Garante um único EAN por (loja, produto): o MAIOR CODACESSO */
          SELECT *
          FROM (
            SELECT
                pa.NROEMPRESA,
                pa.SEQPRODUTO,
                pe.CODACESSO,
                p.DESCCOMPLETA,
                pa.PROMOCAO,
                pa.PRECO_NORMAL,
                pa.PRECO_PROMOCIONAL,
                pa.DTAINICIOPROM,
                pa.DTAFIMPROM,
                pa.DTABASEEXPORTACAO,
                pa.QTDEMBALAGEM,
                NVL(p.SEQFAMILIA, mp.SEQFAMILIA) AS seqfamilia,
                ROW_NUMBER() OVER (
                  PARTITION BY pa.NROEMPRESA, pa.SEQPRODUTO
                  ORDER BY pe.CODACESSO DESC
                ) AS rn
            FROM PDV_PRODALTERACAO_SOCIN pa
            JOIN PDV_PRODPRECOEMBALAGEMSOCIN pe
              ON pe.SEQPRODUTO = pa.SEQPRODUTO
             AND pe.NROEMPRESA = pa.NROEMPRESA
            JOIN PDV_PRODUTOSOCIN p
              ON p.CODPRODUTO = pa.SEQPRODUTO
            LEFT JOIN CONSINCO.MAP_PRODUTO mp
              ON mp.SEQPRODUTO = pa.SEQPRODUTO
            WHERE pa.NROEMPRESA = ?
              AND pa.DTABASEEXPORTACAO BETWEEN
                  TO_DATE(?,'DD/MM/YYYY HH24:MI:SS') AND
                  TO_DATE(?,'DD/MM/YYYY HH24:MI:SS')
              AND pa.PROMOCAO    IN ('N','P')
              AND pa.STATUSVENDA = 'A'
          )
          WHERE rn = 1
        ),
        cat_n1 AS (
          SELECT fdc.SEQFAMILIA,
                 cat.SEQCATEGORIA  AS cod_cat_n1,
                 cat.CATEGORIA     AS desc_cat_n1,
                 ROW_NUMBER() OVER (PARTITION BY fdc.SEQFAMILIA ORDER BY cat.SEQCATEGORIA) AS rn
          FROM CONSINCO.MAP_FAMDIVCATEG fdc
          JOIN CONSINCO.MAP_CATEGORIA cat
            ON cat.SEQCATEGORIA     = fdc.SEQCATEGORIA
           AND cat.NIVELHIERARQUIA  = 1
           AND cat.STATUSCATEGOR    = 'A'
          WHERE fdc.NRODIVISAO = 1
            AND fdc.STATUS     = 'A'
        ),
        cat_n2 AS (
          SELECT fdc.SEQFAMILIA,
                 cat.SEQCATEGORIA    AS cod_cat_n2,
                 cat.SEQCATEGORIAPAI AS cod_cat_n1_parent,
                 cat.CATEGORIA       AS desc_cat_n2,
                 ROW_NUMBER() OVER (PARTITION BY fdc.SEQFAMILIA ORDER BY cat.SEQCATEGORIA) AS rn
          FROM CONSINCO.MAP_FAMDIVCATEG fdc
          JOIN CONSINCO.MAP_CATEGORIA cat
            ON cat.SEQCATEGORIA     = fdc.SEQCATEGORIA
           AND cat.NIVELHIERARQUIA  = 2
           AND cat.STATUSCATEGOR    = 'A'
          WHERE fdc.NRODIVISAO = 1
            AND fdc.STATUS     = 'A'
        ),
        final AS (
          SELECT
              b.NROEMPRESA                                   AS loja,
              b.SEQPRODUTO                                   AS codigo,
              b.CODACESSO                                    AS ean,
              b.DESCCOMPLETA                                 AS descricao,
              b.PROMOCAO,
              b.PRECO_NORMAL,
              b.PRECO_PROMOCIONAL,
              b.DTAINICIOPROM                                AS data_inicio_promocao,
              b.DTAFIMPROM                                   AS data_fim_promocao,
              TO_CHAR(b.DTABASEEXPORTACAO,'DD/MM/YYYY HH24:MI:SS') AS data_alteracao,
              b.DTABASEEXPORTACAO                            AS dt_alteracao_ts,
              b.seqfamilia,
              (cat1.desc_cat_n1 || ' - ' || TO_CHAR(cat1.cod_cat_n1)) AS ordem_categoria_n1,
              cat2.desc_cat_n2                                       AS ordem_categoria_n2,
              cat1.cod_cat_n1,
              cat2.cod_cat_n2,
              b.QTDEMBALAGEM                                         AS qtdembalagem
          FROM base b
          LEFT JOIN cat_n1 cat1
            ON cat1.SEQFAMILIA = b.seqfamilia AND cat1.rn = 1
          LEFT JOIN cat_n2 cat2
            ON cat2.SEQFAMILIA = b.seqfamilia AND cat2.rn = 1
           AND cat2.cod_cat_n1_parent  = cat1.cod_cat_n1
        )
        SELECT
          f.loja,
          f.codigo,
          f.ean,
          f.descricao,
          f.promocao,
          f.preco_normal,
          f.preco_promocional,
          f.data_inicio_promocao,
          f.data_fim_promocao,
          f.data_alteracao,
          f.seqfamilia,
          f.ordem_categoria_n1,
          f.ordem_categoria_n2,
          f.cod_cat_n1,
          f.cod_cat_n2,
          f.qtdembalagem,
          CASE
            WHEN NVL(LAG(f.cod_cat_n1) OVER (
                 ORDER BY f.cod_cat_n1, NVL(f.cod_cat_n2, 999999),
                          f.seqfamilia, f.dt_alteracao_ts, f.loja, f.qtdembalagem
               ), -1) <> f.cod_cat_n1
          THEN 1 ELSE 0 END AS break_n1,
          CASE
            WHEN NVL(LAG(f.cod_cat_n1) OVER (
                 ORDER BY f.cod_cat_n1, NVL(f.cod_cat_n2, 999999),
                          f.seqfamilia, f.dt_alteracao_ts, f.loja, f.qtdembalagem
               ), -1) <> f.cod_cat_n1
              OR NVL(LAG(NVL(f.cod_cat_n2, -1)) OVER (
                 ORDER BY f.cod_cat_n1, NVL(f.cod_cat_n2, 999999),
                          f.seqfamilia, f.dt_alteracao_ts, f.loja, f.qtdembalagem
               ), -1) <> NVL(f.cod_cat_n2, -1)
          THEN 1 ELSE 0 END AS break_n2
        FROM final f
        ORDER BY
          NVL(f.cod_cat_n2, 999999),
          f.cod_cat_n1,
          f.data_inicio_promocao,
          f.descricao,
          f.seqfamilia,
          f.dt_alteracao_ts,
          f.loja,
          f.qtdembalagem
        """;

    @Override
    public List<RelatorioPrecosAlteradosDTO> consultar(Integer nroEmpresa, String dtIni, String dtFim) {
        return oracleExternoJdbcTemplate.query(SQL, rs -> {
            List<RelatorioPrecosAlteradosDTO> list = new ArrayList<>();
            while (rs.next()) {
                RelatorioPrecosAlteradosDTO d = new RelatorioPrecosAlteradosDTO();
                d.setLoja(rs.getInt("loja"));
                d.setCodigo(rs.getLong("codigo"));
                d.setEan(rs.getString("ean"));
                d.setDescricao(rs.getString("descricao"));
                d.setPromocao(rs.getString("promocao"));
                d.setPrecoNormal(rs.getDouble("preco_normal"));
                d.setPrecoPromocional(rs.getDouble("preco_promocional"));

                Timestamp di = rs.getTimestamp("data_inicio_promocao");
                Timestamp df = rs.getTimestamp("data_fim_promocao");
                d.setDataInicioPromocao(di != null ? toStr(di) : null);
                d.setDataFimPromocao(df != null ? toStr(df) : null);

                d.setDataAlteracao(rs.getString("data_alteracao"));
                d.setSeqfamilia(rs.getLong("seqfamilia"));
                d.setOrdemCategoriaN1(rs.getString("ordem_categoria_n1"));
                d.setOrdemCategoriaN2(rs.getString("ordem_categoria_n2"));

                // conversão segura (Oracle pode devolver BigDecimal)
                d.setCodCatN1(getInt(rs, "cod_cat_n1"));
                d.setCodCatN2(getInt(rs, "cod_cat_n2"));

                d.setQtdembalagem(rs.getInt("qtdembalagem"));
                d.setBreakN1(rs.getInt("break_n1"));
                d.setBreakN2(rs.getInt("break_n2"));
                list.add(d);
            }
            return list;
        }, nroEmpresa, dtIni, dtFim);
    }

    private Integer getInt(ResultSet rs, String col) throws SQLException {
        Object o = rs.getObject(col);
        if (o == null) return null;
        if (o instanceof Integer i) return i;
        if (o instanceof Long l) return Math.toIntExact(l);
        if (o instanceof BigDecimal bd) return bd.intValue();
        return Integer.valueOf(o.toString());
    }

    private String toStr(Timestamp ts) {
        return new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(ts);
    }

    @Override
    public String gerarPdfLoja(Integer nroEmpresa, String dtIni, String dtFim) {
        List<RelatorioPrecosAlteradosDTO> dados = consultar(nroEmpresa, dtIni, dtFim);
        if (dados.isEmpty()) {
            log.info("Sem dados para gerar PDF. Loja={} Período={} - {}", nroEmpresa, dtIni, dtFim);
            return null;
        }

        File outDir = new File(PastaUploadUtil.PASTA_RELATORIOS);
        if (!outDir.exists() && !outDir.mkdirs()) {
            log.error("Não foi possível criar diretório de saída: {}", outDir.getAbsolutePath());
            return null;
        }

        String dia = dtIni.replaceAll("\\D", "");
        String ddMMyyyy = (dia.length() >= 8) ? dia.substring(0, 8) : dia;
        String nome = String.format("relatorio_alterados_%d_%s.pdf", nroEmpresa, ddMMyyyy);
        String pathPdf = new File(outDir, nome).getAbsolutePath();

        try {
            String gerado = RelatorioUtil.gerarRelatorioPDF(
                "relatorioprecosalterados.jasper",
                dados,
                String.valueOf(nroEmpresa),
                pathPdf
            );
            log.info("PDF gerado com sucesso: {}", gerado);
            return gerado;
        } catch (Exception e) {
            log.error("Falha ao gerar PDF para loja {} período {} - {}. Erro: {}",
                      nroEmpresa, dtIni, dtFim, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public List<String> gerarPdfParaLojas(List<Long> lojasIds, String dtIni, String dtFim) {
        List<String> paths = new ArrayList<>();
        List<Loja> lojas = (lojasIds == null || lojasIds.isEmpty())
                ? lojaRepository.findAll()
                : lojaRepository.findAllById(lojasIds);

        for (Loja l : lojas) {
            try {
                if (l.getCodLojaRms() == null || l.getCodLojaRms().isBlank()) {
                    log.warn("Loja {} sem codLojaRms — ignorando.", l.getLojaId());
                    continue;
                }
                Integer nro = Integer.valueOf(l.getCodLojaRms());
                String path = gerarPdfLoja(nro, dtIni, dtFim);
                if (path != null) {
                    paths.add(path);
                } else {
                    log.warn("PDF não gerado (sem dados ou erro) para loja {} ({})",
                             l.getLojaId(), l.getCodLojaRms());
                }
            } catch (Exception e) {
                log.error("Erro ao processar loja {} (codLojaRms={}): {}",
                          l.getLojaId(), l.getCodLojaRms(), e.getMessage(), e);
            }
        }
        return paths;
    }
}
