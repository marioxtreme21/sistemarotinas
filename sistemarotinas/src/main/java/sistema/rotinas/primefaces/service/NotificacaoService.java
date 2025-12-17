package sistema.rotinas.primefaces.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sistema.rotinas.primefaces.dto.PriceUpdateRunResult;
import sistema.rotinas.primefaces.dto.VendaLojaResumo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class NotificacaoService {

    @Autowired
    private EmailService emailService;

    // ===========================================================
    // ✅ Destinatários padrão — fácil de editar nesta classe
    // ===========================================================
    private static final List<String> DESTINATARIOS_POS_REVISAO =
            List.of("relatoriorotinasrcg@hiperideal.com.br");

    private static final List<String> DESTINATARIOS_PICKPACK =
            List.of("relatorioecommerce@hiperideal.com.br");

    private static final List<String> DESTINATARIOS_PRICE_SUMMARY =
            List.of("relatorioecommerce@hiperideal.com.br");
    // List.of("mario.emmanuel@hiperideal.com.br");

    // ✅ NOVO: destinatários específicos para o relatório de preços alterados
    private static final List<String> DESTINATARIOS_RELATORIO_PRECO =
            List.of("relatorioalterados@hiperideal.com.br");
    //List.of("mario.emmanuel@hiperideal.com.br");

    // ✅ NOVO: destinatários para Relatório de Vendas por Loja
    private static final List<String> DESTINATARIOS_RELATORIO_VENDAS_LOJAS =
            List.of("relatoriovendas@hiperideal.com.br");

    // ✅ CID da imagem inline do logo para o Relatório de Vendas por Loja
    // Deve ser o MESMO ID usado em EmailService.addInline("logoRelatorioVendas", ...)
    private static final String LOGO_CID_RELATORIO_VENDAS = "logoRelatorioVendas";

    /* ===========================================================
       POS: revisão de data
       =========================================================== */
    public void notificarRevisaoDataPOS(LocalDate data, String usuario) {
        String assunto = "🔔 Data marcada como Revisada - POS (" + data + ")";
        String corpo = String.format("""
            <strong>Data Revisada:</strong> %s<br/>
            <strong>Responsável:</strong> %s<br/>
            <strong>Data da Revisão:</strong> %s<br/>
            """, data, usuario, LocalDate.now());

        emailService.enviarEmailSimples(
            DESTINATARIOS_POS_REVISAO, assunto, "<pre>" + corpo + "</pre>"
        );
    }

    /* ===========================================================
       TEF: arquivo corrigido
       =========================================================== */
    public void notificarArquivoCorrigido(LocalDate dataVenda, String nomeArquivo) {
        String assunto = "📁 Arquivo Corrigido Gerado - Relatório TEF";
        String corpo = String.format("""
            <strong>Data da Venda:</strong> %s<br/>
            <strong>Arquivo Gerado:</strong> %s<br/>
            <strong>Data de Geração:</strong> %s<br/>
            """, dataVenda, nomeArquivo, LocalDate.now());

        emailService.enviarEmailSimples(
            DESTINATARIOS_POS_REVISAO, assunto, "<pre>" + corpo + "</pre>"
        );
    }

    /* ===========================================================
       Pick & Pack: relatórios PDF com anexos
       =========================================================== */
    public void notificarRelatoriosGeradosComAnexo(List<String> caminhosArquivosPdf, String dataReferencia) {
        String dataHoje = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        String assunto = "📊 Relatório de Itens Substituídos Pick and Pack — Data Geração: "
                + dataHoje + ", referente aos pedidos faturados em: " + dataReferencia;

        String corpo = "<p>Prezados,</p>"
                + "<p>Segue em anexo o relatório dos itens substituídos pelo Pick and Pack referente às lojas.</p>"
                + "<p><strong>Data de Geração do Relatório:</strong> " + dataHoje + "<br/>"
                + "<strong>Relatório referente aos pedidos faturados:</strong> " + dataReferencia + "</p>"
                + "<p>Atenciosamente,<br/>Sistema Ecommerce</p>";

        emailService.enviarEmailComAnexosPaths(
            DESTINATARIOS_PICKPACK, assunto, corpo, caminhosArquivosPdf
        );
    }

    /* ===========================================================
       Price Update: resumo em tabela HTML
       =========================================================== */
    public void notificarResumoPriceUpdate(List<PriceUpdateRunResult> resultados, boolean execucaoTotal) {
        if (resultados == null || resultados.isEmpty()) return;

        resultados = resultados.stream()
                .sorted(Comparator.comparing(r -> r.getLojaNome() == null ? "" : r.getLojaNome()))
                .toList();

        String titulo = execucaoTotal
                ? "Resumo — Atualização de Preços (Todas as lojas)"
                : "Resumo — Atualização de Preços (Lojas selecionadas)";

        String assunto = "📈 " + titulo + " — " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"font-family:Arial, Helvetica, sans-serif;font-size:13px;\">");
        sb.append("<h3>").append(titulo).append("</h3>");

        int totConsultados = 0, totOk = 0, totFalha = 0, totRepOk = 0, totRepFalha = 0, totProc = 0;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        sb.append("<table border=\"1\" cellspacing=\"0\" cellpadding=\"6\" style=\"border-collapse:collapse;\">")
          .append("<thead style=\"background:#f0f0f0;\">")
          .append("<tr>")
          .append("<th>Loja</th><th>Política</th><th>Warehouse</th>")
          .append("<th>Início</th><th>Término</th>")
          .append("<th>Consultados</th><th>Enviados OK</th><th>Falha Envio</th>")
          .append("<th>Reprocessados OK</th><th>Reprocessados Falha</th>")
          .append("<th>Total Processados</th><th>Obs.</th>")
          .append("</tr></thead><tbody>");

        for (PriceUpdateRunResult r : resultados) {
            totConsultados += r.getQtdConsultados();
            totOk          += r.getQtdEnviadosOk();
            totFalha       += r.getQtdFalhaEnvio();
            totRepOk       += r.getQtdReprocessadosOk();
            totRepFalha    += r.getQtdReprocessadosFalha();
            totProc        += r.getQtdProcessadosTotal();

            sb.append("<tr>")
              .append(td(nz(r.getLojaNome())))
              .append(td(nz(r.getPoliticaComercial())))
              .append(td(nz(r.getWarehouse())))
              .append(td(r.getInicio() != null ? r.getInicio().format(fmt) : "-"))
              .append(td(r.getFim() != null ? r.getFim().format(fmt) : "-"))
              .append(td(String.valueOf(r.getQtdConsultados())))
              .append(td(String.valueOf(r.getQtdEnviadosOk())))
              .append(td(String.valueOf(r.getQtdFalhaEnvio())))
              .append(td(String.valueOf(r.getQtdReprocessadosOk())))
              .append(td(String.valueOf(r.getQtdReprocessadosFalha())))
              .append(td(String.valueOf(r.getQtdProcessadosTotal())))
              .append(td(nz(r.getObservacoes())))
              .append("</tr>");
        }

        // Linha de totais
        sb.append("<tr style=\"font-weight:bold;background:#fafafa;\">")
          .append(td("TOTAL")).append(td("")).append(td(""))
          .append(td("")).append(td(""))
          .append(td(String.valueOf(totConsultados)))
          .append(td(String.valueOf(totOk)))
          .append(td(String.valueOf(totFalha)))
          .append(td(String.valueOf(totRepOk)))
          .append(td(String.valueOf(totRepFalha)))
          .append(td(String.valueOf(totProc)))
          .append(td(""))
          .append("</tr>");

        sb.append("</tbody></table>");
        sb.append("<p style=\"color:#888;\">Relatório gerado em ")
          .append(LocalDateTime.now().format(fmt))
          .append("</p>");
        sb.append("</div>");

        emailService.enviarEmailSimples(
            DESTINATARIOS_PRICE_SUMMARY, assunto, sb.toString()
        );
    }

    /* ===========================================================
       ✅ Relatório de Preços Alterados (PDFs em anexo)
       =========================================================== */

    public void notificarRelatorioPrecosAlteradosComAnexos(List<String> caminhosArquivosPdf, String dtIni, String dtFim) {
        if (caminhosArquivosPdf == null || caminhosArquivosPdf.isEmpty()) return;

        String hoje = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String assunto = "🏷️ Relatório de Preços Alterados — Geração: " + hoje;

        String corpo = new StringBuilder()
            .append("<p>Prezados,</p>")
            .append("<p>Segue(m) em anexo o(s) relatório(s) de preços alterados por loja.</p>")
            .append("<p><strong>Obs: Os relátorio são gerados e enviado automaticamente todos os dias durante a madrugada.</strong> ").append("<br/>")
            .append("<p>Atenciosamente,<br/>Sistema de Rotinas TI Hiperideal</p>")
            .toString();

        emailService.enviarEmailComAnexosPaths(
            DESTINATARIOS_RELATORIO_PRECO, assunto, corpo, caminhosArquivosPdf
        );
    }

    public void notificarRelatorioPrecosAlteradosPorLoja(String caminhoPdf, String codLojaRms, String dtIni, String dtFim) {
        if (caminhoPdf == null || caminhoPdf.isBlank()) return;

        String hoje = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String assunto = "🏷️ Relatório de Preços Alterados — Loja " + codLojaRms
                + " — Geração: " + hoje + " | Período: " + dtIni + " a " + dtFim;

        String corpo = new StringBuilder()
            .append("<p>Prezados,</p>")
            .append("<p>Segue em anexo o relatório de preços alterados da loja <strong>")
            .append(codLojaRms).append("</strong>.</p>")
            .append("<p><strong>Obs: Os relátorio são gerados e enviado automaticamente todos os dias durante a madrugada.</strong> ").append("<br/>")
            .append("<strong>Data de geração:</strong> ").append(hoje).append("</p>")
            .append("<p>Atenciosamente,<br/>Sistema de Rotinas TI Hiperideal</p>")
            .toString();

        emailService.enviarEmailComAnexosPaths(
            DESTINATARIOS_RELATORIO_PRECO, assunto, corpo, List.of(caminhoPdf)
        );
    }

    /* ===========================================================
       ✅ Relatório de Vendas por Loja (HTML com logo + layout melhorado)
       =========================================================== */

    public void notificarRelatorioVendasLojas(List<VendaLojaResumo> dados,
                                              LocalDate dataInicial,
                                              LocalDate dataFinal,
                                              String codLojaEconect) {

        if (dados == null || dados.isEmpty()) {
            return;
        }

        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        long totalClientes = dados.stream()
                .mapToLong(VendaLojaResumo::getNumeroClientes)
                .sum();

        BigDecimal totalVendas = dados.stream()
                .map(VendaLojaResumo::getTotalVenda)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal ticketMedioGeral = totalClientes > 0
                ? totalVendas.divide(BigDecimal.valueOf(totalClientes), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        String periodoStr;
        if (dataInicial.equals(dataFinal)) {
            periodoStr = dataInicial.format(df);
        } else {
            periodoStr = dataInicial.format(df) + " até " + dataFinal.format(df);
        }

        String assunto = "🧾 Relatório de Vendas por Loja — " + periodoStr;

        StringBuilder sb = new StringBuilder();

        // Wrapper de fonte
        sb.append("<div style=\"font-family:Arial, Helvetica, sans-serif;font-size:13px;\">");

        // Texto introdutório
        sb.append("<p>Segue abaixo o total de clientes e o total de vendas por loja na data informada: ")
          .append("<strong>").append(periodoStr).append("</strong></p>");

        // Cores em harmonia com o logo
        String corBorda = "#2E7D32";
        String corHeader = "#2E7D32";
        String corHeaderTexto = "#FFFFFF";
        String corLinha = "#DCEACB";
        String corTotal = "#A5D6A7";

        sb.append("<table border='1' cellpadding='4' cellspacing='0' ")
          .append("style='border-collapse:collapse;border: ").append(corBorda).append(" solid 2px;'>");

        // ================== CABEÇALHO DENTRO DA TABELA ==================
        // 1ª linha: logo + título
        sb.append("<tr>")
          .append("<td rowspan='2' style='background:#FFFFFF;text-align:center;'>")
          // 🔽 AQUI diminuímos o tamanho do logo
          .append("<img src='cid:").append(LOGO_CID_RELATORIO_VENDAS)
          .append("' alt='Hiperideal' height='80' style='display:block;margin:auto;'/>")
          .append("</td>")
          .append("<td colspan='3' style='background:").append(corHeader)
          .append(";color:").append(corHeaderTexto)
          .append(";text-align:center;font-size:16px;font-weight:bold;'>")
          .append("Relatório de Vendas por Loja")
          .append("</td>")
          .append("</tr>");

        // 2ª linha: período
        sb.append("<tr>")
          .append("<td colspan='3' style='background:").append(corHeader)
          .append(";color:").append(corHeaderTexto)
          .append(";text-align:center;font-size:12px;'>")
          .append("Data: ").append(periodoStr)
          .append("</td>")
          .append("</tr>");

        // 3ª linha: cabeçalho das colunas
        sb.append("<tr>")
          .append("<td width='260' style='background:").append(corHeader)
          .append(";color:").append(corHeaderTexto)
          .append(";font-weight:bold;text-align:left;'>LOJA</td>")
          .append("<td width='120' style='background:").append(corHeader)
          .append(";color:").append(corHeaderTexto)
          .append(";font-weight:bold;text-align:right;'>Nº CLIENTES</td>")
          .append("<td width='140' style='background:").append(corHeader)
          .append(";color:").append(corHeaderTexto)
          .append(";font-weight:bold;text-align:right;'>TOTAL VENDA</td>")
          .append("<td width='160' style='background:").append(corHeader)
          .append(";color:").append(corHeaderTexto)
          .append(";font-weight:bold;text-align:right;'>TICKET MÉDIO</td>")
          .append("</tr>");

        // ================== LINHAS POR LOJA ==================
        for (VendaLojaResumo r : dados) {
            sb.append("<tr>")
              // LOJA em uma única linha (nowrap)
              .append("<td style='background:").append(corLinha)
              .append(";white-space:nowrap;text-align:left;'><strong>")
              .append(r.getDescricaoLoja())
              .append("</strong></td>")

              .append("<td style='background:").append(corLinha)
              .append(";text-align:right;'><strong>")
              .append(r.getNumeroClientes())
              .append("</strong></td>")

              .append("<td style='background:").append(corLinha)
              .append(";text-align:right;'><strong>")
              .append(nf.format(r.getTotalVenda()))
              .append("</strong></td>")

              .append("<td style='background:").append(corLinha)
              .append(";text-align:right;'><strong>")
              .append(nf.format(r.getTicketMedio()))
              .append("</strong></td>")
              .append("</tr>");
        }

        // Linha separadora "em branco"
        sb.append("<tr>")
          .append("<td style='text-align:center;'><strong>-</strong></td>")
          .append("<td style='text-align:center;'><strong>-</strong></td>")
          .append("<td style='text-align:center;'><strong>-</strong></td>")
          .append("<td style='text-align:center;'><strong>-</strong></td>")
          .append("</tr>");

        // Totais
        sb.append("<tr>")
          .append("<td style='background:").append(corTotal)
          .append(";font-weight:bold;text-align:left;'>TOTAL GERAL</td>")
          .append("<td style='background:").append(corTotal)
          .append(";font-weight:bold;text-align:right;'>")
          .append(totalClientes)
          .append("</td>")
          .append("<td style='background:").append(corTotal)
          .append(";font-weight:bold;text-align:right;'>")
          .append(nf.format(totalVendas))
          .append("</td>")
          .append("<td style='background:").append(corTotal)
          .append(";font-weight:bold;text-align:right;'>")
          .append(nf.format(ticketMedioGeral))
          .append("</td>")
          .append("</tr>");

        sb.append("</table><br/>");

        sb.append("Obs.: Este e-mail é gerado e enviado automaticamente, contendo as vendas diárias obtidas com base nos valores do sistema ECONECT, ")
          .append("podendo haver divergências caso alguma loja ou PDV esteja em situação off-line ou com algum problema técnico.<br/>")
          .append("Quando ocorrer esse tipo de situação, será informado no dia seguinte pela equipe de TI responsável pela correção das vendas.<br/><br/>")
          .append("Att,<br/>")
          .append("Sistema de Rotinas TI Hiperideal<br/>");

        sb.append("</div>");

        emailService.enviarEmailSimples(
            DESTINATARIOS_RELATORIO_VENDAS_LOJAS, assunto, sb.toString()
        );
    }

    /* =========================================================== */

    private static String td(String v) {
        return "<td style=\"white-space:nowrap;\">" + v + "</td>";
    }
    private static String nz(String v) {
        return (v == null || v.isBlank()) ? "-" : v;
    }
}
