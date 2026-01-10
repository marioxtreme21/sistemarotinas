package sistema.rotinas.primefaces.service.tv;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sistema.rotinas.primefaces.dto.tv.RotinaSaudeDiaDto;
import sistema.rotinas.primefaces.dto.tv.RotinaSaudeDiaDto.PendenciaLoja;
import sistema.rotinas.primefaces.dto.tv.RotinaSaudeDiaDto.SaudeRotinaItem;
import sistema.rotinas.primefaces.enums.StatusExecucaoEnum;
import sistema.rotinas.primefaces.enums.TipoRotinaEnum;
import sistema.rotinas.primefaces.model.ArquivosMgv;
import sistema.rotinas.primefaces.model.ArquivosPrice;
import sistema.rotinas.primefaces.model.Loja;
import sistema.rotinas.primefaces.model.rotina.RotinaExecucaoLoja;
import sistema.rotinas.primefaces.repository.RotinaExecucaoLojaRepository;
import sistema.rotinas.primefaces.service.interfaces.IArquivosMgvService;
import sistema.rotinas.primefaces.service.interfaces.IArquivosPriceService;

@Service
public class TvSaudeDiaService {

    private static final DateTimeFormatter FMT_DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_DDMM_HHMM = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    @Value("${tv.zone:America/Bahia}")
    private String zoneId;

    @Value("${tv.saude.pendencias.max:10}")
    private int maxPendencias;

    @Autowired private IArquivosPriceService arquivosPriceService;
    @Autowired private IArquivosMgvService arquivosMgvService;

    @Autowired private RotinaExecucaoLojaRepository execLojaRepo;

    @Transactional(readOnly = true)
    public RotinaSaudeDiaDto carregar() {

        ZoneId zone = ZoneId.of(zoneId);
        LocalDate hoje = LocalDate.now(zone);

        LocalDateTime ini = hoje.atStartOfDay();
        LocalDateTime fim = hoje.atTime(LocalTime.MAX);

        SaudeRotinaItem price = montarSaudePrice(ini, fim);
        SaudeRotinaItem mgv   = montarSaudeMgv(ini, fim);

        return new RotinaSaudeDiaDto(hoje.format(FMT_DIA), zone.getId(), price, mgv);
    }

    private SaudeRotinaItem montarSaudePrice(LocalDateTime ini, LocalDateTime fim) {

        SaudeRotinaItem item = new SaudeRotinaItem(TipoRotinaEnum.PRICE.name());

        List<ArquivosPrice> cfgs = safeList(arquivosPriceService.findAll());

        // ✅ dedup por lojaId (mais confiável que distinct em entidade)
        List<Loja> lojas = dedupLojasPorId(
                cfgs.stream()
                        .map(this::safeLojaFromPriceCfg)
                        .filter(Objects::nonNull)
                        .toList()
        );

        item.setTotalLojasComConfig(lojas.size());

        List<PendenciaLoja> pendencias = new ArrayList<>();

        for (Loja loja : lojas) {

            Long lojaId = safeLojaId(loja);

            RotinaExecucaoLoja el = (lojaId != null)
                    ? execLojaRepo
                        .findTopByLoja_LojaIdAndExecucao_TipoRotinaAndExecucao_InicioEmBetweenOrderByExecucao_InicioEmDesc(
                                lojaId, TipoRotinaEnum.PRICE, ini, fim
                        )
                        .orElse(null)
                    : null;

            if (el == null) {
                item.setSemExecucaoHoje(item.getSemExecucaoHoje() + 1);
                pendencias.add(new PendenciaLoja(
                        safeCodConsinco(loja),
                        safeNomeLoja(loja),
                        "SEM_EXECUCAO",
                        "-",
                        null
                ));
                continue;
            }

            StatusExecucaoEnum st = el.getStatus();
            if (st == StatusExecucaoEnum.SUCESSO) item.setOk(item.getOk() + 1);
            else if (st == StatusExecucaoEnum.FALHA_PARCIAL) item.setParcial(item.getParcial() + 1);
            else item.setFalha(item.getFalha() + 1);

            if (st != StatusExecucaoEnum.SUCESSO) {
                pendencias.add(new PendenciaLoja(
                        safeCodConsinco(loja),
                        safeNomeLoja(loja),
                        (st != null ? st.name() : "SEM_STATUS"),
                        formatUltimaExecucao(el),
                        safeExecucaoId(el)
                ));
            }
        }

        item.setPendencias(limitPendenciasOrdenadas(pendencias));
        return item;
    }

    private SaudeRotinaItem montarSaudeMgv(LocalDateTime ini, LocalDateTime fim) {

        SaudeRotinaItem item = new SaudeRotinaItem(TipoRotinaEnum.MGV.name());

        List<ArquivosMgv> cfgs = safeList(arquivosMgvService.findAll());

        // ✅ dedup por lojaId
        List<Loja> lojas = dedupLojasPorId(
                cfgs.stream()
                        .map(this::safeLojaFromMgvCfg)
                        .filter(Objects::nonNull)
                        .toList()
        );

        item.setTotalLojasComConfig(lojas.size());

        List<PendenciaLoja> pendencias = new ArrayList<>();

        for (Loja loja : lojas) {

            Long lojaId = safeLojaId(loja);

            RotinaExecucaoLoja el = (lojaId != null)
                    ? execLojaRepo
                        .findTopByLoja_LojaIdAndExecucao_TipoRotinaAndExecucao_InicioEmBetweenOrderByExecucao_InicioEmDesc(
                                lojaId, TipoRotinaEnum.MGV, ini, fim
                        )
                        .orElse(null)
                    : null;

            if (el == null) {
                item.setSemExecucaoHoje(item.getSemExecucaoHoje() + 1);
                pendencias.add(new PendenciaLoja(
                        safeCodConsinco(loja),
                        safeNomeLoja(loja),
                        "SEM_EXECUCAO",
                        "-",
                        null
                ));
                continue;
            }

            StatusExecucaoEnum st = el.getStatus();
            if (st == StatusExecucaoEnum.SUCESSO) item.setOk(item.getOk() + 1);
            else if (st == StatusExecucaoEnum.FALHA_PARCIAL) item.setParcial(item.getParcial() + 1);
            else item.setFalha(item.getFalha() + 1);

            if (st != StatusExecucaoEnum.SUCESSO) {
                pendencias.add(new PendenciaLoja(
                        safeCodConsinco(loja),
                        safeNomeLoja(loja),
                        (st != null ? st.name() : "SEM_STATUS"),
                        formatUltimaExecucao(el),
                        safeExecucaoId(el)
                ));
            }
        }

        item.setPendencias(limitPendenciasOrdenadas(pendencias));
        return item;
    }

    private List<PendenciaLoja> limitPendenciasOrdenadas(List<PendenciaLoja> pendencias) {
        int lim = Math.max(0, maxPendencias);

        Comparator<PendenciaLoja> byPrioridade = Comparator.comparingInt(p -> prioridade(nz(p.getStatus())));
        return pendencias.stream()
                .sorted(byPrioridade.thenComparing(p -> nz(p.getCodLojaConsinco())))
                .limit(lim)
                .toList();
    }

    private int prioridade(String status) {
        // menor = mais urgente
        if ("FALHA".equals(status)) return 1;
        if ("FALHA_PARCIAL".equals(status)) return 2;
        if ("SEM_EXECUCAO".equals(status)) return 3;
        if ("EM_ANDAMENTO".equals(status)) return 4;
        return 9;
    }

    private String formatUltimaExecucao(RotinaExecucaoLoja el) {
        try {
            LocalDateTime ref = null;
            if (el != null && el.getExecucao() != null) {
                ref = (el.getExecucao().getFimEm() != null ? el.getExecucao().getFimEm() : el.getExecucao().getInicioEm());
            }
            if (ref == null) return "-";
            return ref.format(FMT_DDMM_HHMM);
        } catch (Exception e) {
            return "-";
        }
    }

    private Long safeExecucaoId(RotinaExecucaoLoja el) {
        try {
            return (el != null && el.getExecucao() != null) ? el.getExecucao().getExecucaoId() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Loja safeLojaFromPriceCfg(ArquivosPrice cfg) {
        try { return (cfg != null ? cfg.getLoja() : null); } catch (Exception e) { return null; }
    }

    private Loja safeLojaFromMgvCfg(ArquivosMgv cfg) {
        try { return (cfg != null ? cfg.getLoja() : null); } catch (Exception e) { return null; }
    }

    private Long safeLojaId(Loja loja) {
        try { return (loja != null ? loja.getLojaId() : null); } catch (Exception e) { return null; }
    }

    private String safeNomeLoja(Loja loja) {
        try { return (loja != null && loja.getNome() != null ? loja.getNome() : "-"); } catch (Exception e) { return "-"; }
    }

    // ✅ "Cód. L CONSINCO" = codLojaRms (seu padrão)
    private String safeCodConsinco(Loja loja) {
        try {
            String c = (loja != null ? loja.getCodLojaRms() : null);
            return (c == null || c.isBlank()) ? "-" : c;
        } catch (Exception e) {
            return "-";
        }
    }

    private List<Loja> dedupLojasPorId(List<Loja> lojas) {
        Map<Long, Loja> map = new LinkedHashMap<>();
        for (Loja l : safeList(lojas)) {
            Long id = safeLojaId(l);
            if (id != null && !map.containsKey(id)) {
                map.put(id, l);
            }
        }
        return new ArrayList<>(map.values());
    }

    private static String nz(String v) { return (v == null) ? "" : v; }

    private static <T> List<T> safeList(List<T> v) {
        return v == null ? new ArrayList<>() : v;
    }
}