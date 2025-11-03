package sistema.rotinas.primefaces.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sistema.rotinas.primefaces.dto.PriceUpdateRunResult;

import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

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
       ✅ NOVOS: Relatório de Preços Alterados (PDFs em anexo)
       =========================================================== */

    /**
     * Envia um único e-mail com TODOS os PDFs gerados (várias lojas) anexados.
     * @param caminhosArquivosPdf paths completos dos PDFs
     * @param dtIni texto/valor exibido como data/hora inicial (ex: "23/09/2025 00:00:00")
     * @param dtFim texto/valor exibido como data/hora final   (ex: "23/09/2025 07:00:00")
     */
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

    /**
     * Envia um e-mail por loja (um PDF por e-mail).
     * @param caminhoPdf path do PDF gerado
     * @param codLojaRms código RMS da loja (apenas para o assunto/corpo)
     * @param dtIni período inicial
     * @param dtFim período final
     */
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

    /* =========================================================== */

    private static String td(String v) {
        return "<td style=\"white-space:nowrap;\">" + v + "</td>";
    }
    private static String nz(String v) {
        return (v == null || v.isBlank()) ? "-" : v;
    }
}
