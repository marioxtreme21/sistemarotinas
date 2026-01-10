package sistema.rotinas.primefaces.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import org.hibernate.LazyInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sistema.rotinas.primefaces.enums.OrigemExecucaoEnum;
import sistema.rotinas.primefaces.enums.StatusExecucaoEnum;
import sistema.rotinas.primefaces.enums.TipoRotinaEnum;
import sistema.rotinas.primefaces.model.ArquivosMgv;
import sistema.rotinas.primefaces.model.rotina.RotinaExecucao;
import sistema.rotinas.primefaces.model.rotina.RotinaExecucaoLoja;
import sistema.rotinas.primefaces.repository.RotinaExecucaoLojaRepository;
import sistema.rotinas.primefaces.repository.RotinaExecucaoRepository;
import sistema.rotinas.primefaces.service.interfaces.IArquivosMgvService;
import sistema.rotinas.primefaces.service.interfaces.ILojaService;

@Service
public class RotinaMgvAutoSelectorService {

    private static final Logger LOG = LoggerFactory.getLogger("ROTINA_MGV");

    @Autowired private ILojaService lojaService; // mantido (pode ser útil em evoluções)
    @Autowired private IArquivosMgvService arquivosMgvService;

    @Autowired private RotinaExecucaoRepository execRepo;
    @Autowired private RotinaExecucaoLojaRepository execLojaRepo;

    /**
     * Regras:
     * - sempre considera apenas lojas com ArquivosMgv cadastrado
     * - se a loja teve SUCESSO hoje (em qualquer janela AUTO), NÃO executa mais hoje
     * - na primeira janela (janelaRetry=false): executa todas elegíveis que ainda não tiveram sucesso hoje
     * - nas janelas de retry (janelaRetry=true): executa SOMENTE as que tiveram FALHA/FALHA_PARCIAL hoje
     *   e ainda não tiveram sucesso hoje
     */
    @Transactional(readOnly = true)
    public List<Long> selecionarLojasElegiveisHoje(boolean janelaRetry, String tagJanela) {

        LocalDate hoje = LocalDate.now();
        LocalDateTime ini = hoje.atStartOfDay();
        LocalDateTime fim = hoje.atTime(LocalTime.MAX);

        LOG.info("[SCHED][MGV][{}] Selecionando lojas elegíveis. janelaRetry={} data={}",
                tagJanela, janelaRetry, hoje);

        // 1) somente lojas com cadastro ArquivosMgv
        List<ArquivosMgv> cfgs = safeList(arquivosMgvService.findAll());
        Set<Long> lojaIdsComCfg = cfgs.stream()
                .map(this::safeLojaIdFromCfg)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        LOG.info("[SCHED][MGV][{}] Lojas com cfg ArquivosMgv: {}", tagJanela, lojaIdsComCfg.size());

        if (lojaIdsComCfg.isEmpty()) {
            LOG.warn("[SCHED][MGV][{}] Nenhuma loja com ArquivosMgv cadastrado.", tagJanela);
            return new ArrayList<>();
        }

        // 2) pega execuções do dia (somente MGV + AUTO)
        List<RotinaExecucao> execAll = safeList(execRepo.findAll());

        List<RotinaExecucao> execucoesHoje = execAll.stream()
                .filter(e -> e != null
                        && e.getTipoRotina() == TipoRotinaEnum.MGV
                        && e.getOrigemExecucao() == OrigemExecucaoEnum.AUTOMATICA
                        && e.getInicioEm() != null
                        && !e.getInicioEm().isBefore(ini)
                        && !e.getInicioEm().isAfter(fim))
                .toList();

        Set<Long> execIdsHoje = execucoesHoje.stream()
                .map(RotinaExecucao::getExecucaoId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        LOG.info("[SCHED][MGV][{}] Execuções AUTO hoje encontradas: {} (execIds={})",
                tagJanela, execIdsHoje.size(), (execIdsHoje.size() <= 20 ? execIdsHoje : "many"));

        // 3) se não tem execução hoje ainda:
        if (execIdsHoje.isEmpty()) {
            if (janelaRetry) {
                // retry só roda se já existiu execução hoje para ter falhas registradas
                LOG.info("[SCHED][MGV][{}] Janela de retry, mas NÃO existe execução AUTO hoje no banco. Nada a fazer.",
                        tagJanela);
                return new ArrayList<>();
            }

            LOG.info("[SCHED][MGV][{}] Primeira janela do dia (sem histórico AUTO hoje). Elegíveis={}",
                    tagJanela, lojaIdsComCfg.size());

            return new ArrayList<>(lojaIdsComCfg);
        }

        // 4) carrega execuções por loja do dia (apenas das execuções AUTO de hoje)
        List<RotinaExecucaoLoja> lojasAll = safeList(execLojaRepo.findAll());

        List<RotinaExecucaoLoja> lojasExecHoje = lojasAll.stream()
                .filter(l -> l != null
                        && safeExecucaoIdFromExecLoja(l) != null
                        && execIdsHoje.contains(safeExecucaoIdFromExecLoja(l)))
                .toList();

        LOG.info("[SCHED][MGV][{}] Registros RotinaExecucaoLoja hoje (AUTO) encontrados: {}",
                tagJanela, lojasExecHoje.size());

        // 5) agrupa por lojaId
        Map<Long, List<RotinaExecucaoLoja>> porLojaId = lojasExecHoje.stream()
                .map(l -> new AbstractMap.SimpleEntry<>(safeLojaIdFromExecLoja(l), l))
                .filter(e -> e.getKey() != null)
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                ));

        List<Long> selecionadas = new ArrayList<>();
        int bloqueadasPorSucesso = 0;
        int elegiveisRetry = 0;
        int semHistoricoHoje = 0;

        for (Long lojaId : lojaIdsComCfg) {

            List<RotinaExecucaoLoja> hist = porLojaId.getOrDefault(lojaId, new ArrayList<>());
            if (hist.isEmpty()) semHistoricoHoje++;

            boolean teveSucessoHoje = hist.stream()
                    .anyMatch(x -> x != null && x.getStatus() == StatusExecucaoEnum.SUCESSO);

            if (teveSucessoHoje) {
                bloqueadasPorSucesso++;
                continue;
            }

            if (!janelaRetry) {
                // primeira janela: roda tudo que não teve sucesso ainda (inclui lojas sem histórico hoje)
                selecionadas.add(lojaId);
                continue;
            }

            // retry: somente se teve falha/parcial hoje
            boolean teveFalhaOuParcialHoje = hist.stream().anyMatch(x ->
                    x != null && (x.getStatus() == StatusExecucaoEnum.FALHA
                              || x.getStatus() == StatusExecucaoEnum.FALHA_PARCIAL));

            if (teveFalhaOuParcialHoje) {
                selecionadas.add(lojaId);
                elegiveisRetry++;
            }
        }

        LOG.info("[SCHED][MGV][{}] Seleção concluída. selecionadas={} bloqueadasPorSucesso={} retryElegiveis={} semHistoricoHoje={} totalComCfg={}",
                tagJanela, selecionadas.size(), bloqueadasPorSucesso, elegiveisRetry, semHistoricoHoje, lojaIdsComCfg.size());

        return selecionadas;
    }

    // =========================
    // Safe helpers (minimizando risco de LazyInitializationException)
    // =========================

    private Long safeLojaIdFromCfg(ArquivosMgv cfg) {
        try {
            if (cfg == null || cfg.getLoja() == null) return null;
            return cfg.getLoja().getLojaId();
        } catch (LazyInitializationException lie) {
            LOG.warn("[SCHED][MGV] Lazy ao ler lojaId do cfg ArquivosMgv (ignorado). msg={}", lie.getMessage());
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private Long safeExecucaoIdFromExecLoja(RotinaExecucaoLoja l) {
        try {
            if (l == null || l.getExecucao() == null) return null;
            return l.getExecucao().getExecucaoId();
        } catch (LazyInitializationException lie) {
            LOG.warn("[SCHED][MGV] Lazy ao ler execucaoId em RotinaExecucaoLoja (ignorado). msg={}", lie.getMessage());
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private Long safeLojaIdFromExecLoja(RotinaExecucaoLoja l) {
        try {
            if (l == null || l.getLoja() == null) return null;
            return l.getLoja().getLojaId();
        } catch (LazyInitializationException lie) {
            LOG.warn("[SCHED][MGV] Lazy ao ler lojaId em RotinaExecucaoLoja (ignorado). msg={}", lie.getMessage());
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static <T> List<T> safeList(List<T> v) {
        return v == null ? new ArrayList<>() : v;
    }
}