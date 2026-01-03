package sistema.rotinas.primefaces.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sistema.rotinas.primefaces.dto.rotina.price.*;
import sistema.rotinas.primefaces.enums.StatusExecucaoEnum;
import sistema.rotinas.primefaces.enums.TipoRotinaEnum;
import sistema.rotinas.primefaces.model.rotina.*;
import sistema.rotinas.primefaces.repository.*;

@Service
public class NotificacaoRotinaService {

    @Autowired
    private EmailService emailService;

    @Autowired
    private RotinaExecucaoRepository execRepo;

    @Autowired
    private RotinaExecucaoLojaRepository execLojaRepo;

    @Autowired
    private RotinaExecucaoArquivoRepository execArqRepo;

    @Autowired
    private RotinaExecucaoArquivoEtapaRepository etapaRepo;

    private static final List<String> DESTINATARIOS_NOTIFICACAO_ROTINA_PRICE =
            //List.of("relatoriorotinasprice@hiperideal.com.br");
            List.of("mario.emmanuel@hiperideal.com.br");

    private static final DateTimeFormatter FMT_DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final DateTimeFormatter FMT_DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final DateTimeFormatter FMT_DATA_HORA_ARQ =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Mantido para compatibilidade (se algo do projeto já chama).
     * Para PRICE, se tiver execucaoId, manda o e-mail completo.
     */
    public void notificarExecucaoRotina(RotinaExecucao exec) {
        if (exec == null) return;

        try {
            if (exec.getTipoRotina() == TipoRotinaEnum.PRICE && exec.getExecucaoId() != null) {
                notificarFinalizacaoRotinaPrice(exec.getExecucaoId());
                return;
            }
        } catch (Exception ignore) {}

        // fallback simples
        String tipo = (exec.getTipoRotina() != null ? exec.getTipoRotina().name() : "-");
        String status = (exec.getStatus() != null ? exec.getStatus().name() : "-");

        String assunto = "⏱️ Rotina " + tipo + " — Status: " + status + " — "
                + LocalDateTime.now().format(FMT_DATA_HORA);

        String corpo = "<div style='font-family:Arial;font-size:13px;'>"
                + "<h3>Resumo da execução</h3>"
                + "<p><b>ID:</b> " + esc(nz(exec.getExecucaoId())) + "</p>"
                + "<p><b>Status:</b> " + esc(status) + "</p>"
                + "<p><b>Início:</b> " + esc(fmt(exec.getInicioEm())) + "</p>"
                + "<p><b>Fim:</b> " + esc(fmt(exec.getFimEm())) + "</p>"
                + "<p style='color:#888;'>E-mail automático.</p>"
                + "</div>";

        emailService.enviarEmailSimples(DESTINATARIOS_NOTIFICACAO_ROTINA_PRICE, assunto, corpo);
    }

    public void notificarFinalizacaoRotinaPrice(Long execucaoId) {
        if (execucaoId == null) return;

        RotinaExecucao exec = execRepo.findById(execucaoId).orElse(null);
        if (exec == null) return;

        if (exec.getTipoRotina() != TipoRotinaEnum.PRICE) return;

        RotinaPriceEmailDTO dto = montarDTO(execucaoId, exec);

        String assunto = montarAssunto(dto);
        String corpo = montarHtml(dto);

        emailService.enviarEmailSimples(DESTINATARIOS_NOTIFICACAO_ROTINA_PRICE, assunto, corpo);
    }

    // =========================
    // DTO (montagem)
    // =========================
    private RotinaPriceEmailDTO montarDTO(Long execucaoId, RotinaExecucao exec) {

        // lojas
        List<RotinaExecucaoLoja> lojas = execLojaRepo.findAll().stream()
                .filter(l -> l != null && l.getExecucao() != null
                        && Objects.equals(execucaoId, l.getExecucao().getExecucaoId()))
                .sorted(Comparator.comparing(RotinaExecucaoLoja::getCodLojaRms, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        List<Long> execLojaIds = lojas.stream()
                .map(RotinaExecucaoLoja::getExecucaoLojaId)
                .filter(Objects::nonNull)
                .toList();

        // arquivos
        List<RotinaExecucaoArquivo> arquivos = execArqRepo.findAll().stream()
                .filter(a -> a != null && a.getExecucaoLoja() != null
                        && a.getExecucaoLoja().getExecucaoLojaId() != null
                        && execLojaIds.contains(a.getExecucaoLoja().getExecucaoLojaId()))
                .sorted(Comparator
                        .comparing((RotinaExecucaoArquivo a) -> a.getExecucaoLoja() != null ? a.getExecucaoLoja().getExecucaoLojaId() : null,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(RotinaExecucaoArquivo::getExecucaoArquivoId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        Map<Long, List<RotinaExecucaoArquivo>> arquivosPorLoja = arquivos.stream()
                .filter(a -> a.getExecucaoLoja() != null && a.getExecucaoLoja().getExecucaoLojaId() != null)
                .collect(Collectors.groupingBy(a -> a.getExecucaoLoja().getExecucaoLojaId()));

        List<LojaEmailDTO> lojasDTO = new ArrayList<>();

        for (RotinaExecucaoLoja l : lojas) {
            LojaEmailDTO ld = new LojaEmailDTO();
            ld.setExecucaoLojaId(l.getExecucaoLojaId());
            ld.setCodLojaRms(l.getCodLojaRms());
            ld.setNomeLoja(l.getNomeLoja());
            ld.setStatus(l.getStatus());
            ld.setInicioEm(l.getInicioEm());
            ld.setFimEm(l.getFimEm());
            ld.setMensagem(l.getMensagem());
            ld.setErro(l.getErro());

            List<RotinaExecucaoArquivo> arqs = arquivosPorLoja.getOrDefault(l.getExecucaoLojaId(), new ArrayList<>());
            List<ArquivoEmailDTO> arqsDTO = new ArrayList<>();

            for (RotinaExecucaoArquivo a : arqs) {
                ArquivoEmailDTO ad = new ArquivoEmailDTO();
                ad.setExecucaoArquivoId(a.getExecucaoArquivoId());
                ad.setPatternEsperado(a.getPatternEsperado());
                ad.setNomeArquivo(a.getNomeArquivo());
                ad.setRequired(a.getRequired());
                ad.setStatusFinal(a.getStatus());
                ad.setEtapaFinal(a.getEtapa());
                ad.setTempoTotalMs(a.getTempoTotalMs());
                ad.setOrigem(a.getOrigem());
                ad.setDestino(a.getDestino());
                ad.setMensagem(a.getMensagem());
                ad.setErro(a.getErro());

                // ✅ NOVO (sem quebrar compatibilidade do DTO):
                // tenta “injetar” campos adicionais se existirem no DTO; se não existirem, coloca na mensagem.
                // (Esses dados são importantes para validar “arquivo do dia”.)
                LocalDateTime lmOrigem = safeGetLastModifiedOrigem(a);
                LocalDateTime lmDestino = safeGetLastModifiedDestino(a);
                Boolean atualizado = safeGetOrigemAtualizada(a);

                boolean setouAlgo =
                        safeInvokeSetter(ad, "setLastModifiedOrigem", LocalDateTime.class, lmOrigem)
                     || safeInvokeSetter(ad, "setLastModifiedDestino", LocalDateTime.class, lmDestino)
                     || safeInvokeSetter(ad, "setOrigemAtualizada", Boolean.class, atualizado);

                if (!setouAlgo) {
                    // fallback: agrega na mensagem (não remove nada que já existia)
                    String extra = montarLinhaInfoArquivo(lmOrigem, lmDestino, atualizado);
                    if (extra != null && !extra.isBlank()) {
                        ad.setMensagem(mergeMsg(ad.getMensagem(), extra));
                    }
                }

                // etapas (usa repo existente)
                List<RotinaExecucaoArquivoEtapa> etapas = (a.getExecucaoArquivoId() == null)
                        ? new ArrayList<>()
                        : etapaRepo.findByExecucaoArquivoExecucaoArquivoIdOrderByEtapaIdAsc(a.getExecucaoArquivoId());

                List<EtapaEmailDTO> etapasDTO = new ArrayList<>();
                for (RotinaExecucaoArquivoEtapa t : etapas) {
                    EtapaEmailDTO td = new EtapaEmailDTO();
                    td.setEtapaId(t.getEtapaId());
                    td.setEtapa(t.getEtapa());
                    td.setStatus(t.getStatus());
                    td.setInicioEm(t.getInicioEm());
                    td.setFimEm(t.getFimEm());
                    td.setTempoTotalMs(t.getTempoTotalMs());
                    td.setOrigem(t.getOrigem());
                    td.setDestino(t.getDestino());
                    td.setMensagem(t.getMensagem());
                    td.setErro(t.getErro());
                    etapasDTO.add(td);
                }
                ad.setEtapas(etapasDTO);

                arqsDTO.add(ad);
            }

            ld.setArquivos(arqsDTO);
            lojasDTO.add(ld);
        }

        RotinaPriceEmailDTO dto = new RotinaPriceEmailDTO();
        dto.setExecucaoId(exec.getExecucaoId());
        dto.setTipo(exec.getTipoRotina());
        dto.setOrigem(exec.getOrigemExecucao());
        dto.setStatus(exec.getStatus());
        dto.setSolicitante(exec.getSolicitante());
        dto.setInicioEm(exec.getInicioEm());
        dto.setFimEm(exec.getFimEm());
        dto.setTempoTotalMs(resolveTempoTotalMs(exec));
        dto.setMensagemResumo(exec.getMensagemResumo());
        dto.setErroGeral(exec.getErroGeral());
        dto.setLojas(lojasDTO);

        preencherContadores(dto);
        return dto;
    }

    private void preencherContadores(RotinaPriceEmailDTO dto) {
        int totalLojas = 0, lojasOk = 0, lojasParcial = 0, lojasFalha = 0;
        int totalArqs = 0, arqsOk = 0, arqsParcial = 0, arqsFalha = 0;

        if (dto.getLojas() != null) {
            totalLojas = dto.getLojas().size();

            for (LojaEmailDTO l : dto.getLojas()) {
                if (l.getStatus() == StatusExecucaoEnum.SUCESSO) lojasOk++;
                else if (l.getStatus() == StatusExecucaoEnum.FALHA_PARCIAL) lojasParcial++;
                else if (l.getStatus() == StatusExecucaoEnum.FALHA) lojasFalha++;

                if (l.getArquivos() != null) {
                    totalArqs += l.getArquivos().size();
                    for (ArquivoEmailDTO a : l.getArquivos()) {
                        if (a.getStatusFinal() == StatusExecucaoEnum.SUCESSO) arqsOk++;
                        else if (a.getStatusFinal() == StatusExecucaoEnum.FALHA_PARCIAL) arqsParcial++;
                        else if (a.getStatusFinal() == StatusExecucaoEnum.FALHA) arqsFalha++;
                    }
                }
            }
        }

        dto.setTotalLojas(totalLojas);
        dto.setLojasSucesso(lojasOk);
        dto.setLojasParcial(lojasParcial);
        dto.setLojasFalha(lojasFalha);

        dto.setTotalArquivos(totalArqs);
        dto.setArquivosSucesso(arqsOk);
        dto.setArquivosParcial(arqsParcial);
        dto.setArquivosFalha(arqsFalha);
    }

    // =========================
    // Assunto + HTML
    // =========================
    private String montarAssunto(RotinaPriceEmailDTO dto) {
        String data = dto.getInicioEm() != null ? dto.getInicioEm().toLocalDate().format(FMT_DATA) : LocalDate.now().format(FMT_DATA);

        String tag;
        if (dto.getStatus() == StatusExecucaoEnum.SUCESSO) tag = "✅ [SUCESSO]";
        else if (dto.getStatus() == StatusExecucaoEnum.FALHA_PARCIAL) tag = "⚠️ [FALHA_PARCIAL]";
        else if (dto.getStatus() == StatusExecucaoEnum.FALHA) tag = "❌ [FALHA]";
        else tag = "⏱️ [STATUS]";

        int total = (dto.getTotalLojas() != null ? dto.getTotalLojas() : 0);

        return tag + " Rotina Price - Lojas Hiperideal (" + total + ") - " + data;
    }

    private String montarHtml(RotinaPriceEmailDTO dto) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"font-family:Arial, Helvetica, sans-serif;font-size:13px;\">");

        // Header
        sb.append("<div style='display:flex;align-items:center;gap:12px;margin-bottom:10px;'>")
          .append("<img src='cid:logoHiperideal' alt='Hiperideal' height='38' style='display:block;'/>")
          .append("<div>")
          .append("<div style='font-size:16px;font-weight:bold;'>Notificação - Rotina PRICE</div>")
          .append("<div style='color:#666;'>Execução concluída</div>")
          .append("</div>")
          .append("</div>");

        // ===== 1) Visão geral =====
        sb.append("<h3 style='margin:10px 0 6px 0;'>Visão geral</h3>");
        sb.append("<table border='1' cellspacing='0' cellpadding='6' style='border-collapse:collapse;width:100%;'>")
          .append("<thead style='background:#f0f0f0;'><tr>")
          .append("<th style='text-align:left;'>Campo</th>")
          .append("<th style='text-align:left;'>Valor</th>")
          .append("</tr></thead><tbody>");

        sb.append(tr("ID Execução", esc(nz(dto.getExecucaoId()))));
        sb.append(tr("Status", badge(dto.getStatus())));
        sb.append(tr("Origem", esc(nz(dto.getOrigem()))));
        sb.append(tr("Solicitante", esc(nz(dto.getSolicitante()))));
        sb.append(tr("Início", esc(fmt(dto.getInicioEm()))));
        sb.append(tr("Fim", esc(fmt(dto.getFimEm()))));
        sb.append(tr("Duração", esc(fmtDuracao(dto.getTempoTotalMs()))));

        sb.append(tr("Total lojas", esc(String.valueOf(nvl(dto.getTotalLojas())))));
        sb.append(tr("Lojas sucesso", esc(String.valueOf(nvl(dto.getLojasSucesso())))));
        sb.append(tr("Lojas falha_parcial", esc(String.valueOf(nvl(dto.getLojasParcial())))));
        sb.append(tr("Lojas falha", esc(String.valueOf(nvl(dto.getLojasFalha())))));

        sb.append(tr("Total arquivos", esc(String.valueOf(nvl(dto.getTotalArquivos())))));
        sb.append(tr("Arquivos sucesso", esc(String.valueOf(nvl(dto.getArquivosSucesso())))));
        sb.append(tr("Arquivos falha_parcial", esc(String.valueOf(nvl(dto.getArquivosParcial())))));
        sb.append(tr("Arquivos falha", esc(String.valueOf(nvl(dto.getArquivosFalha())))));

        sb.append("</tbody></table>");

        if (dto.getMensagemResumo() != null && !dto.getMensagemResumo().isBlank()) {
            sb.append("<h3 style='margin:12px 0 6px 0;'>Resumo</h3>")
              .append("<pre style='background:#f6f6f6;padding:10px;border:1px solid #ddd;white-space:pre-wrap;'>")
              .append(esc(dto.getMensagemResumo()))
              .append("</pre>");
        }

        if (dto.getErroGeral() != null && !dto.getErroGeral().isBlank()) {
            sb.append("<h3 style='margin:12px 0 6px 0;'>Erro geral</h3>")
              .append("<pre style='background:#fff1f1;padding:10px;border:1px solid #f2b5b5;white-space:pre-wrap;'>")
              .append(esc(dto.getErroGeral()))
              .append("</pre>");
        }

        // ===== 2) Visão por loja =====
        sb.append("<h3 style='margin:12px 0 6px 0;'>Visão por loja</h3>");
        sb.append("<table border='1' cellspacing='0' cellpadding='6' style='border-collapse:collapse;width:100%;'>")
          .append("<thead style='background:#f0f0f0;'><tr>")
          .append("<th style='text-align:left;'>Cód. L CONSINCO</th>")
          .append("<th style='text-align:left;'>Loja</th>")
          .append("<th style='text-align:left;'>Status</th>")
          .append("<th style='text-align:left;'>Início</th>")
          .append("<th style='text-align:left;'>Fim</th>")
          .append("<th style='text-align:left;'>Arquivos (nome / data)</th>") // ✅ NOVO
          .append("<th style='text-align:left;'>Mensagem</th>")
          .append("<th style='text-align:left;'>Erro</th>")
          .append("</tr></thead><tbody>");

        if (dto.getLojas() != null) {
            for (LojaEmailDTO l : dto.getLojas()) {
                sb.append("<tr>")
                  .append("<td>").append(esc(nz(l.getCodLojaRms()))).append("</td>")
                  .append("<td>").append(esc(nz(l.getNomeLoja()))).append("</td>")
                  .append("<td>").append(badge(l.getStatus())).append("</td>")
                  .append("<td>").append(esc(fmt(l.getInicioEm()))).append("</td>")
                  .append("<td>").append(esc(fmt(l.getFimEm()))).append("</td>")
                  .append("<td>").append(renderResumoArquivosLoja(l)).append("</td>") // ✅ NOVO
                  .append("<td>").append(esc(nz(l.getMensagem()))).append("</td>")
                  .append("<td>").append(esc(nz(l.getErro()))).append("</td>")
                  .append("</tr>");
            }
        }

        sb.append("</tbody></table>");

        // ===== 3) Detalhes só de falhas/parciais =====
        sb.append("<h3 style='margin:12px 0 6px 0;'>Detalhes (somente falhas/parciais)</h3>");

        boolean temProblema = dto.getLojas() != null && dto.getLojas().stream().anyMatch(l ->
                l.getStatus() == StatusExecucaoEnum.FALHA
                || l.getStatus() == StatusExecucaoEnum.FALHA_PARCIAL
                || (l.getArquivos() != null && l.getArquivos().stream().anyMatch(a ->
                        a.getStatusFinal() == StatusExecucaoEnum.FALHA || a.getStatusFinal() == StatusExecucaoEnum.FALHA_PARCIAL)));

        if (!temProblema) {
            sb.append("<div style='color:#137333;font-weight:bold;'>Nenhuma falha/parcial encontrada 🎉</div>");
        } else {
            for (LojaEmailDTO l : dto.getLojas()) {

                boolean lojaProb = l.getStatus() == StatusExecucaoEnum.FALHA || l.getStatus() == StatusExecucaoEnum.FALHA_PARCIAL;

                List<ArquivoEmailDTO> arqProb = (l.getArquivos() == null) ? new ArrayList<>() :
                        l.getArquivos().stream()
                                .filter(a -> a.getStatusFinal() == StatusExecucaoEnum.FALHA || a.getStatusFinal() == StatusExecucaoEnum.FALHA_PARCIAL)
                                .toList();

                if (!lojaProb && arqProb.isEmpty()) continue;

                sb.append("<div style='margin:10px 0;padding:8px;border:1px solid #ddd;border-radius:6px;'>");
                sb.append("<div style='font-weight:bold;margin-bottom:6px;'>")
                  .append(esc(nz(l.getCodLojaRms()))).append(" - ").append(esc(nz(l.getNomeLoja())))
                  .append(" — ").append(badge(l.getStatus()))
                  .append("</div>");

                if (!arqProb.isEmpty()) {
                    sb.append("<table border='1' cellspacing='0' cellpadding='6' style='border-collapse:collapse;width:100%;'>")
                      .append("<thead style='background:#fafafa;'><tr>")
                      .append("<th style='text-align:left;'>Arquivo</th>")
                      .append("<th style='text-align:left;'>Pattern</th>")
                      .append("<th style='text-align:left;'>Status</th>")
                      .append("<th style='text-align:left;'>Etapa final</th>")
                      .append("<th style='text-align:left;'>Mensagem</th>")
                      .append("<th style='text-align:left;'>Erro</th>")
                      .append("</tr></thead><tbody>");

                    for (ArquivoEmailDTO a : arqProb) {
                        sb.append("<tr>")
                          .append("<td>").append(esc(nz(a.getNomeArquivo()))).append("</td>")
                          .append("<td>").append(esc(nz(a.getPatternEsperado()))).append("</td>")
                          .append("<td>").append(badge(a.getStatusFinal())).append("</td>")
                          .append("<td>").append(esc(nz(a.getEtapaFinal()))).append("</td>")
                          .append("<td>").append(esc(nz(a.getMensagem()))).append("</td>")
                          .append("<td>").append(esc(nz(a.getErro()))).append("</td>")
                          .append("</tr>");

                        // Etapas do arquivo problemático
                        if (a.getEtapas() != null && !a.getEtapas().isEmpty()) {
                            sb.append("<tr><td colspan='6'>")
                              .append("<div style='font-weight:bold;margin:6px 0;'>Etapas</div>")
                              .append("<table border='1' cellspacing='0' cellpadding='5' style='border-collapse:collapse;width:100%;'>")
                              .append("<thead style='background:#f0f0f0;'><tr>")
                              .append("<th>ID</th><th>Etapa</th><th>Status</th><th>Início</th><th>Fim</th><th>Mensagem</th><th>Erro</th>")
                              .append("</tr></thead><tbody>");

                            for (EtapaEmailDTO t : a.getEtapas()) {
                                sb.append("<tr>")
                                  .append("<td>").append(esc(nz(t.getEtapaId()))).append("</td>")
                                  .append("<td>").append(esc(nz(t.getEtapa()))).append("</td>")
                                  .append("<td>").append(badge(t.getStatus())).append("</td>")
                                  .append("<td>").append(esc(fmt(t.getInicioEm()))).append("</td>")
                                  .append("<td>").append(esc(fmt(t.getFimEm()))).append("</td>")
                                  .append("<td>").append(esc(nz(t.getMensagem()))).append("</td>")
                                  .append("<td>").append(esc(nz(t.getErro()))).append("</td>")
                                  .append("</tr>");
                            }

                            sb.append("</tbody></table>")
                              .append("</td></tr>");
                        }
                    }

                    sb.append("</tbody></table>");
                } else {
                    sb.append("<div style='color:#666;'>Loja ficou ")
                      .append(esc(statusLabel(l.getStatus())))
                      .append(" mas não há arquivos marcados como falha/parcial.</div>");
                }

                sb.append("</div>");
            }
        }

        sb.append("<p style='color:#888;margin-top:12px;'>E-mail gerado automaticamente pelo Sistema de Rotinas.</p>");
        sb.append("</div>");
        return sb.toString();
    }

    // =========================
    // ✅ NOVO: resumo de arquivos na linha da loja
    // =========================
    private String renderResumoArquivosLoja(LojaEmailDTO l) {
        if (l == null || l.getArquivos() == null || l.getArquivos().isEmpty()) {
            return "<span style='color:#666;'>-</span>";
        }

        // mostra todos, mas de forma compacta; (se quiser limitar, troque aqui)
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='display:flex;flex-direction:column;gap:4px;'>");

        for (ArquivoEmailDTO a : l.getArquivos()) {
            // tenta obter lastModified via getters (se existirem no DTO), senão tenta “extrair” de mensagem (fallback)
            LocalDateTime lmOrigem = safeGetDtoDateTime(a, "getLastModifiedOrigem");
            LocalDateTime lmDestino = safeGetDtoDateTime(a, "getLastModifiedDestino");
            Boolean atualizado = safeGetDtoBoolean(a, "getOrigemAtualizada");

            String dt =
                    (lmOrigem != null) ? lmOrigem.format(FMT_DATA_HORA_ARQ)
                  : (lmDestino != null) ? lmDestino.format(FMT_DATA_HORA_ARQ)
                  : null;

            String nome = nz(a.getNomeArquivo());
            String st = (a.getStatusFinal() != null ? a.getStatusFinal().name() : "-");

            String badgeData = "";
            if (dt != null) {
                badgeData = " | <span style='color:#111;'><b>Data:</b> " + esc(dt) + "</span>";
            } else {
                badgeData = " | <span style='color:#666;'><b>Data:</b> -</span>";
            }

            String badgeAt = "";
            if (atualizado != null) {
                if (Boolean.TRUE.equals(atualizado)) {
                    badgeAt = " | <span style='color:#137333;font-weight:bold;'>ATUALIZADO</span>";
                } else {
                    badgeAt = " | <span style='color:#b91c1c;font-weight:bold;'>DESATUALIZADO</span>";
                }
            }

            sb.append("<div style='white-space:nowrap;'>")
              .append("<span style='font-weight:bold;'>").append(esc(nome)).append("</span>")
              .append(" | <span style='color:#333;'><b>Status:</b> ").append(esc(st)).append("</span>")
              .append(badgeData)
              .append(badgeAt)
              .append("</div>");
        }

        sb.append("</div>");
        return sb.toString();
    }

    // =========================
    // Helpers
    // =========================
    private static String tr(String c, String v) {
        return "<tr><td style='font-weight:bold;white-space:nowrap;'>" + esc(c) + "</td><td>" + (v == null ? "-" : v) + "</td></tr>";
    }

    private static String statusLabel(StatusExecucaoEnum st) {
        if (st == null) return "-";
        if (st == StatusExecucaoEnum.FALHA_PARCIAL) return "FALHA_PARCIAL";
        return st.name();
    }

    private static String badge(StatusExecucaoEnum st) {
        if (st == null) return "<span style='padding:2px 8px;border-radius:10px;background:#eee;color:#333;font-weight:bold;'>-</span>";

        String bg, fg, label;
        if (st == StatusExecucaoEnum.SUCESSO) { bg="#e6f4ea"; fg="#137333"; label="SUCESSO"; }
        else if (st == StatusExecucaoEnum.FALHA_PARCIAL) { bg="#fff4e5"; fg="#b45309"; label="FALHA_PARCIAL"; }
        else if (st == StatusExecucaoEnum.FALHA) { bg="#fde8e8"; fg="#b91c1c"; label="FALHA"; }
        else { bg="#eee"; fg="#333"; label=st.name(); }

        return "<span style='padding:2px 8px;border-radius:10px;background:" + bg + ";color:" + fg + ";font-weight:bold;white-space:nowrap;'>"
                + esc(label) + "</span>";
    }

    private static String fmt(LocalDateTime dt) {
        return dt == null ? "-" : dt.format(FMT_DATA_HORA);
    }

    private static Long resolveTempoTotalMs(RotinaExecucao exec) {
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

    private static int nvl(Integer v) { return v == null ? 0 : v; }

    private static String nz(Object v) {
        if (v == null) return "-";
        String s = String.valueOf(v);
        return (s.isBlank() ? "-" : s);
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String mergeMsg(String a, String b) {
        if (a == null || a.isBlank()) return b;
        if (b == null || b.isBlank()) return a;
        return a + "\n" + b;
    }

    // =========================
    // ✅ NOVO: utilitários para incluir data/hora do arquivo sem quebrar DTO
    // =========================
    private static LocalDateTime safeGetLastModifiedOrigem(RotinaExecucaoArquivo a) {
        try { return a != null ? a.getLastModifiedOrigem() : null; } catch (Exception e) { return null; }
    }

    private static LocalDateTime safeGetLastModifiedDestino(RotinaExecucaoArquivo a) {
        try { return a != null ? a.getLastModifiedDestino() : null; } catch (Exception e) { return null; }
    }

    private static Boolean safeGetOrigemAtualizada(RotinaExecucaoArquivo a) {
        try { return a != null ? a.getOrigemAtualizada() : null; } catch (Exception e) { return null; }
    }

    private static boolean safeInvokeSetter(Object target, String setter, Class<?> paramType, Object value) {
        if (target == null) return false;
        try {
            var m = target.getClass().getMethod(setter, paramType);
            m.invoke(target, value);
            return true;
        } catch (NoSuchMethodException nsme) {
            return false;
        } catch (Exception ignore) {
            return false;
        }
    }

    private static LocalDateTime safeGetDtoDateTime(Object dto, String getter) {
        if (dto == null) return null;
        try {
            var m = dto.getClass().getMethod(getter);
            Object v = m.invoke(dto);
            return (v instanceof LocalDateTime) ? (LocalDateTime) v : null;
        } catch (Exception ignore) {
            return null;
        }
    }

    private static Boolean safeGetDtoBoolean(Object dto, String getter) {
        if (dto == null) return null;
        try {
            var m = dto.getClass().getMethod(getter);
            Object v = m.invoke(dto);
            return (v instanceof Boolean) ? (Boolean) v : null;
        } catch (Exception ignore) {
            return null;
        }
    }

    private static String montarLinhaInfoArquivo(LocalDateTime lmOrigem, LocalDateTime lmDestino, Boolean atualizado) {
        String dt =
                (lmOrigem != null) ? lmOrigem.format(FMT_DATA_HORA_ARQ)
              : (lmDestino != null) ? lmDestino.format(FMT_DATA_HORA_ARQ)
              : null;

        String at;
        if (atualizado == null) at = null;
        else at = Boolean.TRUE.equals(atualizado) ? "ATUALIZADO" : "DESATUALIZADO";

        if (dt == null && at == null) return null;

        StringBuilder sb = new StringBuilder();
        sb.append("Arquivo - Data: ").append(dt != null ? dt : "-");
        if (at != null) sb.append(" | ").append(at);
        return sb.toString();
    }
}