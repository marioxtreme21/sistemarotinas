package sistema.rotinas.primefaces.service.loyalty;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sistema.rotinas.primefaces.enums.StatusExecucaoEnum;
import sistema.rotinas.primefaces.model.loyalty.RotinaExecucaoLoyalty;
import sistema.rotinas.primefaces.model.loyalty.RotinaExecucaoLoyaltyCupom;
import sistema.rotinas.primefaces.model.loyalty.RotinaExecucaoLoyaltyLote;
import sistema.rotinas.primefaces.repository.loyalty.RotinaExecucaoLoyaltyCupomRepository;
import sistema.rotinas.primefaces.repository.loyalty.RotinaExecucaoLoyaltyLoteRepository;
import sistema.rotinas.primefaces.repository.loyalty.RotinaExecucaoLoyaltyRepository;
import sistema.rotinas.primefaces.service.EmailService;

@Service
public class NotificacaoLoyaltyService {

    @Autowired
    private EmailService emailService;

    @Autowired
    private RotinaExecucaoLoyaltyRepository execucaoRepository;

    @Autowired
    private RotinaExecucaoLoyaltyLoteRepository loteRepository;

    @Autowired
    private RotinaExecucaoLoyaltyCupomRepository cupomRepository;

    private static final List<String> DESTINATARIOS_NOTIFICACAO_LOYALTY =
            // List.of("relatoriorotinasloyalty@hiperideal.com.br");
            List.of("mario.emmanuel@hiperideal.com.br");

    private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final int MAX_LOTES_DETALHADOS_RESUMO = 30;
    private static final int MAX_PENDENCIAS_RESUMO = 30;

    public void notificarFinalizacaoLoyalty(Long execucaoLoyaltyId) {
        if (execucaoLoyaltyId == null) {
            return;
        }

        RotinaExecucaoLoyalty exec = buscarExecucao(execucaoLoyaltyId);
        if (exec == null) {
            return;
        }

        List<RotinaExecucaoLoyaltyLote> lotes = buscarLotesDaExecucao(execucaoLoyaltyId);
        List<RotinaExecucaoLoyaltyCupom> pendencias = buscarPendenciasDaExecucao(execucaoLoyaltyId);

        String assunto = montarAssunto(exec, false);
        String corpo = montarHtmlDetalhado(exec, lotes, pendencias);

        emailService.enviarEmailSimples(DESTINATARIOS_NOTIFICACAO_LOYALTY, assunto, corpo);
    }

    public void notificarFinalizacaoLoyaltyResumida(Long execucaoLoyaltyId) {
        if (execucaoLoyaltyId == null) {
            return;
        }

        RotinaExecucaoLoyalty exec = buscarExecucao(execucaoLoyaltyId);
        if (exec == null) {
            return;
        }

        List<RotinaExecucaoLoyaltyLote> lotes = buscarLotesDaExecucao(execucaoLoyaltyId);
        List<RotinaExecucaoLoyaltyCupom> pendencias = buscarPendenciasDaExecucao(execucaoLoyaltyId);

        String assunto = montarAssunto(exec, true);
        String corpo = montarHtmlResumido(exec, lotes, pendencias);

        emailService.enviarEmailSimples(DESTINATARIOS_NOTIFICACAO_LOYALTY, assunto, corpo);
    }

    private RotinaExecucaoLoyalty buscarExecucao(Long execucaoLoyaltyId) {
        return execucaoRepository.findById(execucaoLoyaltyId).orElse(null);
    }

    private List<RotinaExecucaoLoyaltyLote> buscarLotesDaExecucao(Long execucaoLoyaltyId) {
        return loteRepository.findAll().stream()
                .filter(l -> l != null
                        && l.getExecucao() != null
                        && Objects.equals(execucaoLoyaltyId, l.getExecucao().getExecucaoLoyaltyId()))
                .sorted(Comparator
                        .comparing((RotinaExecucaoLoyaltyLote l) -> l.getLoja() != null ? nz(l.getLoja().getCodLojaRms()) : "")
                        .thenComparing(RotinaExecucaoLoyaltyLote::getDataMovimento, Comparator.nullsLast(LocalDate::compareTo)))
                .collect(Collectors.toList());
    }

    private List<RotinaExecucaoLoyaltyCupom> buscarPendenciasDaExecucao(Long execucaoLoyaltyId) {
        return cupomRepository.findByReprocessamentoPendenteTrueOrderByDataMovimentoAscExecucaoLoyaltyCupomIdAsc().stream()
                .filter(c -> c != null
                        && c.getExecucao() != null
                        && Objects.equals(execucaoLoyaltyId, c.getExecucao().getExecucaoLoyaltyId()))
                .sorted(Comparator
                        .comparing(RotinaExecucaoLoyaltyCupom::getDataMovimento, Comparator.nullsLast(LocalDate::compareTo))
                        .thenComparing(RotinaExecucaoLoyaltyCupom::getExecucaoLoyaltyCupomId, Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.toList());
    }

    private String montarAssunto(RotinaExecucaoLoyalty exec, boolean resumido) {
        String tag;
        if (exec.getStatus() == StatusExecucaoEnum.SUCESSO) tag = "✅ [SUCESSO]";
        else if (exec.getStatus() == StatusExecucaoEnum.FALHA_PARCIAL) tag = "⚠️ [FALHA_PARCIAL]";
        else if (exec.getStatus() == StatusExecucaoEnum.FALHA) tag = "❌ [FALHA]";
        else tag = "⏱️ [STATUS]";

        int totalLojas = exec.getTotalLojas() != null ? exec.getTotalLojas() : 0;

        String periodo;
        if (exec.getDataInicial() != null && exec.getDataFinal() != null) {
            if (exec.getDataInicial().equals(exec.getDataFinal())) {
                periodo = exec.getDataInicial().format(FMT_DATA);
            } else {
                periodo = exec.getDataInicial().format(FMT_DATA) + " até " + exec.getDataFinal().format(FMT_DATA);
            }
        } else {
            periodo = LocalDate.now().format(FMT_DATA);
        }

        String tipo = resumido ? " [RESUMIDO]" : "";
        return tag + tipo + " Rotina Loyalty - Lojas Hiperideal (" + totalLojas + ") - " + periodo;
    }

    private String montarHtmlDetalhado(RotinaExecucaoLoyalty exec,
                                       List<RotinaExecucaoLoyaltyLote> lotes,
                                       List<RotinaExecucaoLoyaltyCupom> pendencias) {

        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"font-family:Arial, Helvetica, sans-serif;font-size:13px;\">");

        sb.append("<div style='display:flex;align-items:center;gap:12px;margin-bottom:10px;'>")
          .append("<img src='cid:logoHiperideal' alt='Hiperideal' height='38' style='display:block;'/>")
          .append("<div>")
          .append("<div style='font-size:16px;font-weight:bold;'>Notificação - Rotina Loyalty</div>")
          .append("<div style='color:#666;'>Execução concluída</div>")
          .append("</div>")
          .append("</div>");

        appendVisaoGeral(sb, exec);

        if (exec.getMensagemResumo() != null && !exec.getMensagemResumo().isBlank()) {
            sb.append("<h3 style='margin:12px 0 6px 0;'>Resumo</h3>")
              .append("<pre style='background:#f6f6f6;padding:10px;border:1px solid #ddd;white-space:pre-wrap;'>")
              .append(esc(exec.getMensagemResumo()))
              .append("</pre>");
        }

        if (exec.getErroGeral() != null && !exec.getErroGeral().isBlank()) {
            sb.append("<h3 style='margin:12px 0 6px 0;'>Erro geral</h3>")
              .append("<pre style='background:#fff1f1;padding:10px;border:1px solid #f2b5b5;white-space:pre-wrap;'>")
              .append(esc(exec.getErroGeral()))
              .append("</pre>");
        }

        sb.append("<h3 style='margin:12px 0 6px 0;'>Visão por lote</h3>");
        sb.append("<table border='1' cellspacing='0' cellpadding='6' style='border-collapse:collapse;width:100%;'>")
          .append("<thead style='background:#f0f0f0;'><tr>")
          .append("<th style='text-align:left;'>Loja</th>")
          .append("<th style='text-align:left;'>Cód. RMS</th>")
          .append("<th style='text-align:left;'>Cód. Econect</th>")
          .append("<th style='text-align:left;'>Data</th>")
          .append("<th style='text-align:left;'>Status</th>")
          .append("<th style='text-align:right;'>Consultados</th>")
          .append("<th style='text-align:right;'>Enviados</th>")
          .append("<th style='text-align:right;'>Falhas</th>")
          .append("<th style='text-align:right;'>Pendentes</th>")
          .append("<th style='text-align:left;'>Início</th>")
          .append("<th style='text-align:left;'>Fim</th>")
          .append("</tr></thead><tbody>");

        for (RotinaExecucaoLoyaltyLote lote : lotes) {
            String nomeLoja = lote.getLoja() != null ? lote.getLoja().getNome() : "-";
            String codRms = lote.getLoja() != null ? lote.getLoja().getCodLojaRms() : "-";
            String codEconect = lote.getLoja() != null ? lote.getLoja().getCodLojaEconect() : "-";

            sb.append("<tr>")
              .append("<td>").append(esc(nz(nomeLoja))).append("</td>")
              .append("<td>").append(esc(nz(codRms))).append("</td>")
              .append("<td>").append(esc(nz(codEconect))).append("</td>")
              .append("<td>").append(esc(fmtData(lote.getDataMovimento()))).append("</td>")
              .append("<td>").append(badge(lote.getStatus())).append("</td>")
              .append("<td style='text-align:right;'>").append(nvl(lote.getQtdCuponsConsultados())).append("</td>")
              .append("<td style='text-align:right;'>").append(nvl(lote.getQtdCuponsEnviados())).append("</td>")
              .append("<td style='text-align:right;'>").append(nvl(lote.getQtdCuponsFalha())).append("</td>")
              .append("<td style='text-align:right;'>").append(nvl(lote.getQtdPendentesReprocessamento())).append("</td>")
              .append("<td>").append(esc(fmt(lote.getInicioEm()))).append("</td>")
              .append("<td>").append(esc(fmt(lote.getFimEm()))).append("</td>")
              .append("</tr>");
        }

        sb.append("</tbody></table>");

        appendPendenciasCompletas(sb, pendencias);

        sb.append("<p style='color:#888;margin-top:12px;'>E-mail gerado automaticamente pelo Sistema de Rotinas.</p>");
        sb.append("</div>");
        return sb.toString();
    }

    private String montarHtmlResumido(RotinaExecucaoLoyalty exec,
                                      List<RotinaExecucaoLoyaltyLote> lotes,
                                      List<RotinaExecucaoLoyaltyCupom> pendencias) {

        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"font-family:Arial, Helvetica, sans-serif;font-size:13px;\">");

        sb.append("<div style='display:flex;align-items:center;gap:12px;margin-bottom:10px;'>")
          .append("<img src='cid:logoHiperideal' alt='Hiperideal' height='38' style='display:block;'/>")
          .append("<div>")
          .append("<div style='font-size:16px;font-weight:bold;'>Notificação - Rotina Loyalty [Resumo]</div>")
          .append("<div style='color:#666;'>Execução concluída - volume alto</div>")
          .append("</div>")
          .append("</div>");

        appendVisaoGeral(sb, exec);

        if (exec.getMensagemResumo() != null && !exec.getMensagemResumo().isBlank()) {
            sb.append("<h3 style='margin:12px 0 6px 0;'>Resumo</h3>")
              .append("<pre style='background:#f6f6f6;padding:10px;border:1px solid #ddd;white-space:pre-wrap;'>")
              .append(esc(exec.getMensagemResumo()))
              .append("</pre>");
        }

        if (exec.getErroGeral() != null && !exec.getErroGeral().isBlank()) {
            sb.append("<h3 style='margin:12px 0 6px 0;'>Erro geral</h3>")
              .append("<pre style='background:#fff1f1;padding:10px;border:1px solid #f2b5b5;white-space:pre-wrap;'>")
              .append(esc(exec.getErroGeral()))
              .append("</pre>");
        }

        long lotesComFalha = lotes.stream()
                .filter(l -> l.getStatus() == StatusExecucaoEnum.FALHA || l.getStatus() == StatusExecucaoEnum.FALHA_PARCIAL)
                .count();

        sb.append("<h3 style='margin:12px 0 6px 0;'>Indicadores resumidos</h3>");
        sb.append("<table border='1' cellspacing='0' cellpadding='6' style='border-collapse:collapse;width:100%;'>")
          .append("<thead style='background:#f0f0f0;'><tr>")
          .append("<th style='text-align:left;'>Indicador</th>")
          .append("<th style='text-align:left;'>Valor</th>")
          .append("</tr></thead><tbody>")
          .append(tr("Total de lotes localizados", esc(String.valueOf(lotes.size()))))
          .append(tr("Lotes com falha ou falha parcial", esc(String.valueOf(lotesComFalha))))
          .append(tr("Pendências de reprocessamento", esc(String.valueOf(pendencias.size()))))
          .append("</tbody></table>");

        List<RotinaExecucaoLoyaltyLote> lotesProblema = lotes.stream()
                .filter(l -> l.getStatus() == StatusExecucaoEnum.FALHA || l.getStatus() == StatusExecucaoEnum.FALHA_PARCIAL)
                .limit(MAX_LOTES_DETALHADOS_RESUMO)
                .collect(Collectors.toList());

        sb.append("<h3 style='margin:12px 0 6px 0;'>Lotes com problema");
        if (lotesComFalha > MAX_LOTES_DETALHADOS_RESUMO) {
            sb.append(" (primeiros ").append(MAX_LOTES_DETALHADOS_RESUMO).append(")");
        }
        sb.append("</h3>");

        if (lotesProblema.isEmpty()) {
            sb.append("<div style='color:#137333;font-weight:bold;'>Nenhum lote com problema identificado 🎉</div>");
        } else {
            sb.append("<table border='1' cellspacing='0' cellpadding='6' style='border-collapse:collapse;width:100%;'>")
              .append("<thead style='background:#f0f0f0;'><tr>")
              .append("<th style='text-align:left;'>Loja</th>")
              .append("<th style='text-align:left;'>Cód. RMS</th>")
              .append("<th style='text-align:left;'>Cód. Econect</th>")
              .append("<th style='text-align:left;'>Data</th>")
              .append("<th style='text-align:left;'>Status</th>")
              .append("<th style='text-align:right;'>Consultados</th>")
              .append("<th style='text-align:right;'>Enviados</th>")
              .append("<th style='text-align:right;'>Falhas</th>")
              .append("<th style='text-align:right;'>Pendentes</th>")
              .append("</tr></thead><tbody>");

            for (RotinaExecucaoLoyaltyLote lote : lotesProblema) {
                String nomeLoja = lote.getLoja() != null ? lote.getLoja().getNome() : "-";
                String codRms = lote.getLoja() != null ? lote.getLoja().getCodLojaRms() : "-";
                String codEconect = lote.getLoja() != null ? lote.getLoja().getCodLojaEconect() : "-";

                sb.append("<tr>")
                  .append("<td>").append(esc(nz(nomeLoja))).append("</td>")
                  .append("<td>").append(esc(nz(codRms))).append("</td>")
                  .append("<td>").append(esc(nz(codEconect))).append("</td>")
                  .append("<td>").append(esc(fmtData(lote.getDataMovimento()))).append("</td>")
                  .append("<td>").append(badge(lote.getStatus())).append("</td>")
                  .append("<td style='text-align:right;'>").append(nvl(lote.getQtdCuponsConsultados())).append("</td>")
                  .append("<td style='text-align:right;'>").append(nvl(lote.getQtdCuponsEnviados())).append("</td>")
                  .append("<td style='text-align:right;'>").append(nvl(lote.getQtdCuponsFalha())).append("</td>")
                  .append("<td style='text-align:right;'>").append(nvl(lote.getQtdPendentesReprocessamento())).append("</td>")
                  .append("</tr>");
            }

            sb.append("</tbody></table>");
        }

        sb.append("<h3 style='margin:12px 0 6px 0;'>Pendências de reprocessamento");
        if (pendencias.size() > MAX_PENDENCIAS_RESUMO) {
            sb.append(" (primeiras ").append(MAX_PENDENCIAS_RESUMO).append(")");
        }
        sb.append("</h3>");

        if (pendencias.isEmpty()) {
            sb.append("<div style='color:#137333;font-weight:bold;'>Nenhuma pendência encontrada 🎉</div>");
        } else {
            sb.append("<table border='1' cellspacing='0' cellpadding='6' style='border-collapse:collapse;width:100%;'>")
              .append("<thead style='background:#fafafa;'><tr>")
              .append("<th style='text-align:left;'>Loja</th>")
              .append("<th style='text-align:left;'>Data</th>")
              .append("<th style='text-align:left;'>PDV</th>")
              .append("<th style='text-align:left;'>Cupom</th>")
              .append("<th style='text-align:left;'>Tentativas</th>")
              .append("<th style='text-align:left;'>HTTP</th>")
              .append("<th style='text-align:left;'>Erro</th>")
              .append("</tr></thead><tbody>");

            for (RotinaExecucaoLoyaltyCupom p : pendencias.stream().limit(MAX_PENDENCIAS_RESUMO).collect(Collectors.toList())) {
                String nomeLoja = p.getLoja() != null ? p.getLoja().getNome() : "-";

                sb.append("<tr>")
                  .append("<td>").append(esc(nz(nomeLoja))).append("</td>")
                  .append("<td>").append(esc(fmtData(p.getDataMovimento()))).append("</td>")
                  .append("<td>").append(esc(nz(p.getIdPdv()))).append("</td>")
                  .append("<td>").append(esc(nz(p.getNumCupom()))).append("</td>")
                  .append("<td>").append(esc(nz(p.getTentativasEnvio()))).append("</td>")
                  .append("<td>").append(esc(nz(p.getHttpStatus()))).append("</td>")
                  .append("<td>").append(esc(nz(p.getErro()))).append("</td>")
                  .append("</tr>");
            }

            sb.append("</tbody></table>");
        }

        sb.append("<p style='color:#888;margin-top:12px;'>E-mail resumido gerado automaticamente pelo Sistema de Rotinas para execuções com alto volume.</p>");
        sb.append("</div>");
        return sb.toString();
    }

    private void appendVisaoGeral(StringBuilder sb, RotinaExecucaoLoyalty exec) {
        sb.append("<h3 style='margin:10px 0 6px 0;'>Visão geral</h3>");
        sb.append("<table border='1' cellspacing='0' cellpadding='6' style='border-collapse:collapse;width:100%;'>")
          .append("<thead style='background:#f0f0f0;'><tr>")
          .append("<th style='text-align:left;'>Campo</th>")
          .append("<th style='text-align:left;'>Valor</th>")
          .append("</tr></thead><tbody>");

        sb.append(tr("ID Execução", esc(nz(exec.getExecucaoLoyaltyId()))));
        sb.append(tr("Status", badge(exec.getStatus())));
        sb.append(tr("Origem", esc(nz(exec.getOrigemExecucao()))));
        sb.append(tr("Selecionar todas", esc(Boolean.TRUE.equals(exec.getSelecionarTodas()) ? "Sim" : "Não")));
        sb.append(tr("Data inicial", esc(fmtData(exec.getDataInicial()))));
        sb.append(tr("Data final", esc(fmtData(exec.getDataFinal()))));
        sb.append(tr("Início", esc(fmt(exec.getInicioEm()))));
        sb.append(tr("Fim", esc(fmt(exec.getFimEm()))));
        sb.append(tr("Duração", esc(fmtDuracao(resolveTempoTotalMs(exec)))));
        sb.append(tr("Total lojas", esc(String.valueOf(nvl(exec.getTotalLojas())))));
        sb.append(tr("Total lotes", esc(String.valueOf(nvl(exec.getTotalLotes())))));
        sb.append(tr("Total cupons consultados", esc(String.valueOf(nvl(exec.getTotalCuponsConsultados())))));
        sb.append(tr("Total cupons enviados", esc(String.valueOf(nvl(exec.getTotalCuponsEnviados())))));
        sb.append(tr("Total cupons falha", esc(String.valueOf(nvl(exec.getTotalCuponsFalha())))));

        sb.append("</tbody></table>");
    }

    private void appendPendenciasCompletas(StringBuilder sb, List<RotinaExecucaoLoyaltyCupom> pendencias) {
        sb.append("<h3 style='margin:12px 0 6px 0;'>Pendências de reprocessamento</h3>");

        if (pendencias.isEmpty()) {
            sb.append("<div style='color:#137333;font-weight:bold;'>Nenhuma pendência encontrada 🎉</div>");
            return;
        }

        sb.append("<table border='1' cellspacing='0' cellpadding='6' style='border-collapse:collapse;width:100%;'>")
          .append("<thead style='background:#fafafa;'><tr>")
          .append("<th style='text-align:left;'>Loja</th>")
          .append("<th style='text-align:left;'>Data</th>")
          .append("<th style='text-align:left;'>PDV</th>")
          .append("<th style='text-align:left;'>Cupom</th>")
          .append("<th style='text-align:left;'>Tentativas</th>")
          .append("<th style='text-align:left;'>HTTP</th>")
          .append("<th style='text-align:left;'>Mensagem</th>")
          .append("<th style='text-align:left;'>Erro</th>")
          .append("</tr></thead><tbody>");

        for (RotinaExecucaoLoyaltyCupom p : pendencias) {
            String nomeLoja = p.getLoja() != null ? p.getLoja().getNome() : "-";

            sb.append("<tr>")
              .append("<td>").append(esc(nz(nomeLoja))).append("</td>")
              .append("<td>").append(esc(fmtData(p.getDataMovimento()))).append("</td>")
              .append("<td>").append(esc(nz(p.getIdPdv()))).append("</td>")
              .append("<td>").append(esc(nz(p.getNumCupom()))).append("</td>")
              .append("<td>").append(esc(nz(p.getTentativasEnvio()))).append("</td>")
              .append("<td>").append(esc(nz(p.getHttpStatus()))).append("</td>")
              .append("<td>").append(esc(nz(p.getMensagem()))).append("</td>")
              .append("<td>").append(esc(nz(p.getErro()))).append("</td>")
              .append("</tr>");
        }

        sb.append("</tbody></table>");
    }

    private static String tr(String c, String v) {
        return "<tr><td style='font-weight:bold;white-space:nowrap;'>" + esc(c) + "</td><td>" + (v == null ? "-" : v) + "</td></tr>";
    }

    private static String badge(StatusExecucaoEnum st) {
        if (st == null) return "<span style='padding:2px 8px;border-radius:10px;background:#eee;color:#333;font-weight:bold;'>-</span>";

        String bg, fg, label;
        if (st == StatusExecucaoEnum.SUCESSO) {
            bg = "#e6f4ea"; fg = "#137333"; label = "SUCESSO";
        } else if (st == StatusExecucaoEnum.FALHA_PARCIAL) {
            bg = "#fff4e5"; fg = "#b45309"; label = "FALHA_PARCIAL";
        } else if (st == StatusExecucaoEnum.FALHA) {
            bg = "#fde8e8"; fg = "#b91c1c"; label = "FALHA";
        } else {
            bg = "#eee"; fg = "#333"; label = st.name();
        }

        return "<span style='padding:2px 8px;border-radius:10px;background:" + bg + ";color:" + fg + ";font-weight:bold;white-space:nowrap;'>"
                + esc(label) + "</span>";
    }

    private static String fmt(LocalDateTime dt) {
        return dt == null ? "-" : dt.format(FMT_DATA_HORA);
    }

    private static String fmtData(LocalDate dt) {
        return dt == null ? "-" : dt.format(FMT_DATA);
    }

    private static Long resolveTempoTotalMs(RotinaExecucaoLoyalty exec) {
        if (exec == null) return null;
        if (exec.getTempoTotalMs() != null) return exec.getTempoTotalMs();
        if (exec.getInicioEm() != null && exec.getFimEm() != null) {
            Duration d = Duration.between(exec.getInicioEm(), exec.getFimEm());
            if (d.isNegative()) d = Duration.ZERO;
            return d.toMillis();
        }
        return null;
    }

    private static String fmtDuracao(Long ms) {
        if (ms == null) return "-";
        if (ms < 0) ms = 0L;
        Duration d = Duration.ofMillis(ms);
        long h = d.toHours();
        int m = d.toMinutesPart();
        int s = d.toSecondsPart();
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private static int nvl(Integer v) {
        return v == null ? 0 : v;
    }

    private static String nz(Object v) {
        if (v == null) return "-";
        String s = String.valueOf(v);
        return s.isBlank() ? "-" : s;
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}