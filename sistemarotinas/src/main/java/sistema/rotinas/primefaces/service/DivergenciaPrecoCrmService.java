package sistema.rotinas.primefaces.service;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import sistema.rotinas.primefaces.dto.DivergenciaPrecoCrmDto;
import sistema.rotinas.primefaces.dto.DivergenciaResumoLojaDto;
import sistema.rotinas.primefaces.model.Loja;
import sistema.rotinas.primefaces.service.interfaces.ILojaService;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Service
public class DivergenciaPrecoCrmService {

    @Autowired
    private ILojaService lojaService;

    @Autowired
    private NotificacaoService notificacaoService;

    @Autowired
    @Qualifier("mysqlExternalJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Value("${sistema.rotinas.relatorios.dir:}")
    private String relatoriosDir;

    private static final String SQL = """
        SELECT
  cfg.cod_loj AS loja,
  e.codigo_produto,
  e.codigo_ean,
  p.descricao,
  pcrm.preco  AS preco_crm,
  pnorm.preco AS preco_normal
FROM (
  SELECT
    cod_loj,
    MAX(CASE WHEN cod_niv % 10 = 1 THEN cod_niv END) AS niv_normal,
    MAX(CASE WHEN cod_niv % 10 = 7 THEN cod_niv END) AS niv_crm
  FROM cfg_carga_prc_loj
  WHERE cod_loj = ?           
  GROUP BY cod_loj
) AS cfg

JOIN (
  SELECT n1.codigo_nivel, n1.codigo_produto, n1.preco, n1.data_preco
  FROM nivel_preco_produto n1
  JOIN (
    SELECT codigo_nivel, codigo_produto, MAX(data_preco) AS data_preco
    FROM nivel_preco_produto
    GROUP BY codigo_nivel, codigo_produto
  ) mx
    ON mx.codigo_nivel   = n1.codigo_nivel
   AND mx.codigo_produto = n1.codigo_produto
   AND mx.data_preco     = n1.data_preco
) AS pcrm
  ON pcrm.codigo_nivel = cfg.niv_crm

JOIN (
  SELECT n1.codigo_nivel, n1.codigo_produto, n1.preco, n1.data_preco
  FROM nivel_preco_produto n1
  JOIN (
    SELECT codigo_nivel, codigo_produto, MAX(data_preco) AS data_preco
    FROM nivel_preco_produto
    GROUP BY codigo_nivel, codigo_produto
  ) mx
    ON mx.codigo_nivel   = n1.codigo_nivel
   AND mx.codigo_produto = n1.codigo_produto
   AND mx.data_preco     = n1.data_preco
) AS pnorm
  ON pnorm.codigo_nivel   = cfg.niv_normal
 AND pnorm.codigo_produto = pcrm.codigo_produto

JOIN ean AS e
  ON e.codigo_ean = pcrm.codigo_produto

JOIN produto AS p
  ON p.codigo_produto = e.codigo_produto

WHERE pcrm.preco > pnorm.preco
ORDER BY e.codigo_produto
        """;

    public void executarManual(Loja lojaSelecionadaOuNullParaTodas) throws Exception {

        List<Loja> lojas;
        if (lojaSelecionadaOuNullParaTodas == null) {
            lojas = lojaService.getAllLojas();
        } else {
            lojas = List.of(lojaSelecionadaOuNullParaTodas);
        }

        List<File> anexos = new ArrayList<>();
        List<DivergenciaResumoLojaDto> resumo = new ArrayList<>();

        for (Loja loja : lojas) {
            if (loja == null) continue;

            if (loja.getCodLojaEconect() == null || loja.getCodLojaEconect().isBlank()) {
                continue;
            }

            final Long codLojParam;
            try {
                codLojParam = Long.valueOf(loja.getCodLojaEconect().trim());
            } catch (NumberFormatException nfe) {
                continue;
            }

            List<DivergenciaPrecoCrmDto> linhas = jdbcTemplate.query(
                    SQL,
                    (rs, rowNum) -> {
                        DivergenciaPrecoCrmDto dto = new DivergenciaPrecoCrmDto();
                        dto.setLoja(rs.getLong("loja"));
                        dto.setCodigoProduto(rs.getLong("codigo_produto"));
                        dto.setCodigoEan(rs.getString("codigo_ean"));
                        dto.setDescricao(rs.getString("descricao"));
                        dto.setPrecoCrm(rs.getBigDecimal("preco_crm"));
                        dto.setPrecoNormal(rs.getBigDecimal("preco_normal"));
                        return dto;
                    },
                    codLojParam
            );

            int total = linhas.size();

            // ✅ SOMENTE lojas com divergência entram no resumo e geram anexo
            if (total > 0) {
                resumo.add(new DivergenciaResumoLojaDto(
                        loja.getNome(),
                        loja.getCodLojaRms(),
                        total
                ));

                File xlsx = gerarXlsxPorLoja(loja, linhas);
                anexos.add(xlsx);
            }
        }

        // ✅ Se não houve divergência em nenhuma loja: não envia e-mail
        if (anexos.isEmpty() || resumo.isEmpty()) {
            return;
        }

        List<String> caminhos = anexos.stream()
                .map(File::getAbsolutePath)
                .toList();

        notificacaoService.notificarRelatorioDivergenciaPrecoCrm(caminhos, resumo);
    }

    private File gerarXlsxPorLoja(Loja loja, List<DivergenciaPrecoCrmDto> linhas) throws Exception {

        String codLojaRms = (loja.getCodLojaRms() == null || loja.getCodLojaRms().isBlank())
                ? String.valueOf(loja.getLojaId())
                : loja.getCodLojaRms().trim();

        String nomeArquivo = "relatorio_divergencia_preco_crm_x_preco_normal_loja_" + codLojaRms + ".xlsx";

        File dir;
        if (relatoriosDir != null && !relatoriosDir.isBlank()) {
            dir = new File(relatoriosDir);
            dir.mkdirs();
        } else {
            dir = Files.createTempDirectory("relatorios-rotinas-").toFile();
        }

        File arquivo = new File(dir, nomeArquivo);

        try (XSSFWorkbook wb = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(arquivo)) {

            var sheet = wb.createSheet("Divergencias");

            Row h = sheet.createRow(0);
            h.createCell(0).setCellValue("loja");
            h.createCell(1).setCellValue("codigo_produto");
            h.createCell(2).setCellValue("codigo_ean");
            h.createCell(3).setCellValue("descricao");
            h.createCell(4).setCellValue("preco_crm");
            h.createCell(5).setCellValue("preco_normal");

            int r = 1;
            for (DivergenciaPrecoCrmDto dto : linhas) {
                Row row = sheet.createRow(r++);

                row.createCell(0).setCellValue(dto.getLoja() == null ? "" : String.valueOf(dto.getLoja()));
                row.createCell(1).setCellValue(dto.getCodigoProduto() == null ? "" : String.valueOf(dto.getCodigoProduto()));
                row.createCell(2).setCellValue(dto.getCodigoEan() == null ? "" : dto.getCodigoEan());
                row.createCell(3).setCellValue(dto.getDescricao() == null ? "" : dto.getDescricao());

                BigDecimal crm = dto.getPrecoCrm();
                BigDecimal normal = dto.getPrecoNormal();

                row.createCell(4).setCellValue(crm == null ? "" : crm.toPlainString());
                row.createCell(5).setCellValue(normal == null ? "" : normal.toPlainString());
            }

            wb.write(fos);
        }

        return arquivo;
    }
}
