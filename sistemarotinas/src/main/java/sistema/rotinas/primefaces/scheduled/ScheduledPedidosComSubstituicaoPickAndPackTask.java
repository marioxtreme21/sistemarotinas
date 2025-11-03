package sistema.rotinas.primefaces.scheduled;

import java.io.File;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import sistema.rotinas.primefaces.dto.ItemSubstituidoDTO;
import sistema.rotinas.primefaces.dto.PedidoClienteDTO;
import sistema.rotinas.primefaces.dto.RelatorioItemSubstituicaoDTO;
import sistema.rotinas.primefaces.model.Loja;
import sistema.rotinas.primefaces.service.*;
import sistema.rotinas.primefaces.util.PastaUploadUtil;
import sistema.rotinas.primefaces.util.RelatorioUtil;

@Component
public class ScheduledPedidosComSubstituicaoPickAndPackTask {

    private static final Logger log = LoggerFactory.getLogger(ScheduledPedidosComSubstituicaoPickAndPackTask.class);

    @Autowired private LojaService lojaService;

    @Autowired
    @Qualifier("oracleJdbcTemplate") // garante que será o JdbcTemplate do Oracle
    private JdbcTemplate oracleJdbcTemplate;

    @Autowired private VtexOmsApiService vtexOmsApiService;
    @Autowired private PickAndPackApiService pickAndPackApiService;
    @Autowired private NotificacaoService notificacaoService;
    @Autowired private RelatorioExcelPickAndPackService relatorioExcelPickAndPackService;

    //@Scheduled(cron = "0 00 01 * * ?", zone = "America/Bahia")
    public void executarTarefa() {
        log.info("🚀 Iniciando tarefa: Pedidos com Substituição Pick and Pack");

        LocalDate dataAnterior = LocalDate.now().minusDays(1);
        String dataFormatada = dataAnterior.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        List<String> arquivosGerados = new ArrayList<>();

        for (Loja loja : lojaService.getAllLojas().stream()
                .filter(l -> Boolean.TRUE.equals(l.getEcommerceAtivo()) && Boolean.TRUE.equals(l.getPickAndPackAtivo()))
                .toList()) {

            log.info("🔍 Processando loja: {} codLojaEconect={}", loja.getNome(), loja.getCodLojaEconect());

            List<PedidoClienteDTO> pedidos = oracleJdbcTemplate.query(
                """
                SELECT pe.num, pe.nom_cli, pe.dat AS dataPedido, pe.DAT_HOR_SIT AS dataFaturamento
                  FROM eco_ped pe
                 WHERE pe.DAT_HOR_SIT BETWEEN to_timestamp(? || ' 00:00:00','DD-MM-YYYY HH24:MI:SS')
                   AND to_timestamp(? || ' 23:59:59','DD-MM-YYYY HH24:MI:SS')
                   AND pe.sit IN (3,6,7) AND pe.cod_loj = ?
                 ORDER BY pe.cod_loj
                """,
                (rs, rn) -> {
                    PedidoClienteDTO dto = new PedidoClienteDTO();
                    dto.setNumPedido(rs.getLong("num"));
                    dto.setNomeCliente(rs.getString("nom_cli"));
                    dto.setDataPedido(formatData(rs.getDate("dataPedido")));
                    dto.setDataFaturamento(formatData(rs.getTimestamp("dataFaturamento")));
                    return dto;
                }, dataFormatada, dataFormatada, Integer.valueOf(loja.getCodLojaEconect()));

            List<RelatorioItemSubstituicaoDTO> relatorioItens = new ArrayList<>();

            for (PedidoClienteDTO pedido : pedidos) {
                String orderId = vtexOmsApiService.obterOrderIdPorSequence(pedido.getNumPedido().toString());
                if (orderId == null) continue;

                try {
                    TimeUnit.MILLISECONDS.sleep(200);
                    List<ItemSubstituidoDTO> itens = pickAndPackApiService.getItensSubstituidos(orderId);
                    for (ItemSubstituidoDTO item : itens) {
                        Map<String,Object> infoOrig = consultarRmsInfo(item.getSkuOriginal(), loja.getCodLojaRmsDg());
                        Map<String,Object> infoSub = item.getSkuSubstituido()!=null
                                ? consultarRmsInfo(item.getSkuSubstituido(), loja.getCodLojaRmsDg()) : null;

                        relatorioItens.add(buildDTO(item, pedido, loja.getCodLojaRms(), true, infoOrig));
                        if (item.getSkuSubstituido() != null)
                            relatorioItens.add(buildDTO(item, pedido, loja.getCodLojaRms(), false, infoSub));
                    }
                } catch (Exception e) {
                    log.error("Erro no pedido {}: {}", pedido.getNumPedido(), e.getMessage(), e);
                }
            }

            if (!relatorioItens.isEmpty()) {
                try {
                    File outDir = new File(PastaUploadUtil.PASTA_RELATORIOS);
                    if (!outDir.exists() && !outDir.mkdirs()) {
                        throw new RuntimeException("❌ Falha ao criar diretório: " + outDir.getAbsolutePath());
                    }

                    String nomePdf = String.format("RelatorioItensSubstituidosPickAndPac_%s_%s.pdf",
                            loja.getCodLojaRms(), dataFormatada);
                    String pathPdf = new File(outDir, nomePdf).getAbsolutePath();

                    arquivosGerados.add(RelatorioUtil.gerarRelatorioPDF(
                        "relatorioitenssubstituidopickandpack.jasper",
                        relatorioItens,
                        loja.getCodLojaRms(),
                        pathPdf
                    ));

                    String xlsx = relatorioExcelPickAndPackService.gerarRelatorioXLSX(
                        loja.getCodLojaRms(), dataFormatada, relatorioItens);
                    if (xlsx != null) arquivosGerados.add(xlsx);

                } catch (Exception e) {
                    log.error("Erro ao gerar relatórios para loja {}: {}", loja.getNome(), e.getMessage(), e);
                }
            }
        }

        if (!arquivosGerados.isEmpty()) {
            notificacaoService.notificarRelatoriosGeradosComAnexo(
                arquivosGerados, dataAnterior.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));

            arquivosGerados.stream().map(File::new).forEach(f -> {
                try {
                    if (f.exists() && f.delete())
                        log.info("🗑️ Deletado: {}", f.getAbsolutePath());
                    else
                        log.warn("⚠️ Falha ao deletar: {}", f.getAbsolutePath());
                } catch (Exception e) {
                    log.error("❌ Erro ao tentar deletar arquivo: {}", f.getAbsolutePath(), e);
                }
            });
        }

        log.info("✅ Tarefa finalizada.");
    }

    private RelatorioItemSubstituicaoDTO buildDTO(ItemSubstituidoDTO item, PedidoClienteDTO pedido,
                                                  String codRms, boolean isOriginal, Map<String,Object> info) {
        RelatorioItemSubstituicaoDTO dto = new RelatorioItemSubstituicaoDTO();
        dto.setLojaRms(codRms);
        dto.setPedido(pedido.getNumPedido().toString());
        dto.setCliente(pedido.getNomeCliente());
        dto.setCodigo(isOriginal ? item.getSkuOriginal() : item.getSkuSubstituido());
        dto.setEan(isOriginal ? item.getEanOriginal() : item.getEanSubstituido());
        dto.setDescricao(isOriginal ? item.getItemNameOriginal() : item.getItemNameSubstituido());
        dto.setPreco(isOriginal ? item.getPrecoOriginal() : item.getPrecoSubstituido());
        dto.setQuantidade(isOriginal ? item.getQuantidadeOriginal() : item.getQuantidadeSubstituida());
        dto.setTipoItem(isOriginal ? "ORIGINAL" : "SUBSTITUIDO");
        dto.setDataPedido(pedido.getDataPedido());
        dto.setDataFaturamento(pedido.getDataFaturamento());
        if (!isOriginal)
            dto.setDiferencaPercentualPreco(calcularDiff(item.getPrecoOriginal(), item.getPrecoSubstituido()));
        if (info != null) {
            Object est = info.get("est_rms");
            if (est != null)
                dto.setEstRms(String.valueOf((int)Math.floor(Double.parseDouble(est.toString()))));
            dto.setDataUltimaEntrada(formatDateSql(info.get("dat_ult_ent")));
        }
        return dto;
    }

    private Map<String,Object> consultarRmsInfo(String sku, String codLojaRmsDg) {
        if (codLojaRmsDg == null || codLojaRmsDg.isBlank()) return null;
        List<Map<String,Object>> res = oracleJdbcTemplate.queryForList("""
            SELECT get_cod_produto AS codigo, get_cod_local AS loja_rms, get_estoque AS est_rms,
                   rmsto_date(get_dt_ult_ent) AS dat_ult_ent
              FROM AA2CESTQ@rms
             WHERE TRUNC(get_cod_produto/10)=? AND get_cod_local=?
        """, Integer.valueOf(sku), Integer.valueOf(codLojaRmsDg));
        return res.isEmpty() ? null : res.get(0);
    }

    private Double calcularDiff(Double o, Double s) {
        return (o == null || o == 0) ? null : ((s - o) / o) * 100;
    }

    private String formatData(java.util.Date d) {
        return new SimpleDateFormat("dd-MM-yyyy").format(d);
    }

    private String formatDateSql(Object dt) {
        if (dt instanceof Timestamp ts)
            return new SimpleDateFormat("dd-MM-yyyy").format(ts);
        if (dt instanceof java.sql.Date sd)
            return new SimpleDateFormat("dd-MM-yyyy").format(sd);
        return null;
    }
}
