package sistema.rotinas.primefaces.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC; // ✅
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sistema.rotinas.primefaces.consinco.ViewEcommercePrecoReader;
import sistema.rotinas.primefaces.dto.PriceUpdateRunResult;
import sistema.rotinas.primefaces.dto.ProdutoPrecoDTO;
import sistema.rotinas.primefaces.model.Loja;
import sistema.rotinas.primefaces.model.PriceUpdate;
import sistema.rotinas.primefaces.repository.LojaRepository;
import sistema.rotinas.primefaces.repository.PriceUpdateRepository;
import sistema.rotinas.primefaces.service.interfaces.IPriceUpdateService;
import sistema.rotinas.primefaces.vtex.PriceUpdateContext;
import sistema.rotinas.primefaces.vtex.VtexPricingClient;
import sistema.rotinas.primefaces.vtex.VtexPricingException;

@Service
public class PriceUpdateService implements IPriceUpdateService {

    private static final Logger log = LoggerFactory.getLogger(PriceUpdateService.class);

    // 🔢 Configuração de frequência dos logs de progresso
    private static final int  PROGRESS_STEP_ITEMS   = 200; // log a cada 200 itens
    private static final long PROGRESS_STEP_SECONDS = 15L; // ou a cada 15s

    @Autowired
    private PriceUpdateRepository priceUpdateRepository;

    @Autowired
    private LojaRepository lojaRepository;

    @Autowired
    private VtexPricingClient vtexPricingClient;

    // ⚠️ Reader da view Consinco
    @Autowired
    private ViewEcommercePrecoReader viewConsincoPrecoReader;

    @PersistenceContext
    private EntityManager entityManager;

    /* ===========================================================
       CRUD básico
       =========================================================== */

    @Override
    @Transactional
    public List<PriceUpdate> getAll() {
        if (log.isDebugEnabled()) log.debug("[PriceUpdateService] getAll()");
        return priceUpdateRepository.findAll();
    }

    @Override
    @Transactional
    public PriceUpdate save(PriceUpdate priceUpdate) {
        if (log.isDebugEnabled()) {
            log.debug("[PriceUpdateService] save() loja={} sku={} desc='{}'",
                priceUpdate.getLoja() != null ? priceUpdate.getLoja().getLojaId() : null,
                priceUpdate.getCodigo(), priceUpdate.getDescricao());
        }
        if (priceUpdate.getLoja() != null && priceUpdate.getCodigo() != null) {
            Optional<PriceUpdate> existente = priceUpdateRepository
                .findByLoja_LojaIdAndCodigo(priceUpdate.getLoja().getLojaId(), priceUpdate.getCodigo());
            if (existente.isPresent()) {
                log.debug("[PriceUpdateService] Atualizando PriceUpdate existente id={}", existente.get().getPriceUpdateId());
                PriceUpdate e = existente.get();
                e.setDescricao(priceUpdate.getDescricao());
                e.setValue(priceUpdate.getValue());
                e.setListPrice(priceUpdate.getListPrice());
                e.setMinQuantity(priceUpdate.getMinQuantity());
                e.setDataInicial(priceUpdate.getDataInicial());
                e.setDataFinal(priceUpdate.getDataFinal());
                e.setDataUltimoEnvio(priceUpdate.getDataUltimoEnvio());
                e.setStatusEnvioVtex(priceUpdate.getStatusEnvioVtex());
                e.setReprocessamento(priceUpdate.getReprocessamento());
                return priceUpdateRepository.save(e);
            }
        }
        log.debug("[PriceUpdateService] Criando novo PriceUpdate.");
        return priceUpdateRepository.save(priceUpdate);
    }

    @Override
    @Transactional
    public PriceUpdate findById(Long id) {
        if (log.isDebugEnabled()) log.debug("[PriceUpdateService] findById() id={}", id);
        return priceUpdateRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        log.info("[PriceUpdateService] deleteById() id={}", id);
        priceUpdateRepository.deleteById(id);
    }

    @Override
    @Transactional
    public PriceUpdate update(PriceUpdate priceUpdate) {
        log.info("[PriceUpdateService] update() id={}", priceUpdate.getPriceUpdateId());
        if (priceUpdate.getPriceUpdateId() == null) {
            throw new IllegalArgumentException("ID obrigatório para atualizar PriceUpdate.");
        }
        if (priceUpdateRepository.existsById(priceUpdate.getPriceUpdateId())) {
            return priceUpdateRepository.save(priceUpdate);
        }
        throw new IllegalArgumentException("PriceUpdate com ID " + priceUpdate.getPriceUpdateId() + " não encontrado.");
    }

    /* ===========================================================
       Paginação / Ordenação
       =========================================================== */

    @Override
    @Transactional
    public List<PriceUpdate> findAll(int first, int pageSize, String sortField, boolean ascendente) {
        if (log.isDebugEnabled()) {
            log.debug("[PriceUpdateService] findAll() first={} pageSize={} sortField={} asc={}",
                first, pageSize, sortField, ascendente);
        }
        String sql = "SELECT * FROM price_update";
        if (sortField != null) {
            sql += " ORDER BY " + sortField + (ascendente ? " ASC" : " DESC");
        }
        Query query = entityManager.createNativeQuery(sql, PriceUpdate.class);
        query.setFirstResult(first);
        query.setMaxResults(pageSize);
        @SuppressWarnings("unchecked")
        List<PriceUpdate> result = query.getResultList();
        if (log.isDebugEnabled()) log.debug("[PriceUpdateService] findAll() -> {} registros", result.size());
        return result;
    }

    @Override
    @Transactional
    public int count() {
        if (log.isDebugEnabled()) log.debug("[PriceUpdateService] count()");
        Query query = entityManager.createNativeQuery("SELECT COUNT(*) FROM price_update");
        int c = ((Number) query.getSingleResult()).intValue();
        if (log.isDebugEnabled()) log.debug("[PriceUpdateService] count() -> {}", c);
        return c;
    }

    /* ===========================================================
       Pesquisa por critérios
       =========================================================== */

    @Override
    @Transactional
    public List<PriceUpdate> findByCriteria(String campo, String condicao, String valor,
                                            int first, int pageSize, String sortField, boolean ascendente) {
        if (log.isDebugEnabled()) {
            log.debug("[PriceUpdateService] findByCriteria() campo='{}' condicao='{}' valor='{}' first={} pageSize={} sortField={} asc={}",
                campo, condicao, valor, first, pageSize, sortField, ascendente);
        }

        if (campo == null || campo.isEmpty() || condicao == null || condicao.isEmpty()) {
            return findAll(first, pageSize, sortField, ascendente);
        }

        String sql = "SELECT * FROM price_update WHERE " + campo;
        sql += condicao.equals("equal") ? " = :valor" : " LIKE :valor";
        if (sortField != null) {
            sql += " ORDER BY " + sortField + (ascendente ? " ASC" : " DESC");
        }

        Query query = entityManager.createNativeQuery(sql, PriceUpdate.class);
        query.setParameter("valor", condicao.equals("equal") ? valor : "%" + valor + "%");
        query.setFirstResult(first);
        query.setMaxResults(pageSize);

        @SuppressWarnings("unchecked")
        List<PriceUpdate> result = query.getResultList();
        if (log.isDebugEnabled()) log.debug("[PriceUpdateService] findByCriteria() -> {} registros", result.size());
        return result;
    }

    @Override
    @Transactional
    public int countByCriteria(String campo, String condicao, String valor) {
        if (log.isDebugEnabled()) {
            log.debug("[PriceUpdateService] countByCriteria() campo='{}' condicao='{}' valor='{}'", campo, condicao, valor);
        }
        if (campo == null || campo.isEmpty() || condicao == null || condicao.isEmpty()) {
            return count();
        }

        String sql = "SELECT COUNT(*) FROM price_update WHERE " + campo;
        sql += condicao.equals("equal") ? " = :valor" : " LIKE :valor";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("valor", condicao.equals("equal") ? valor : "%" + valor + "%");
        int c = ((Number) query.getSingleResult()).intValue();
        if (log.isDebugEnabled()) log.debug("[PriceUpdateService] countByCriteria() -> {}", c);
        return c;
    }

    /* ===========================================================
       Consultas úteis
       =========================================================== */

    @Override
    @Transactional
    public Optional<PriceUpdate> findByLojaAndCodigo(Long lojaId, Integer codigo) {
        if (log.isDebugEnabled()) log.debug("[PriceUpdateService] findByLojaAndCodigo() lojaId={} codigo={}", lojaId, codigo);
        return priceUpdateRepository.findByLoja_LojaIdAndCodigo(lojaId, codigo);
    }

    @Override
    @Transactional
    public List<PriceUpdate> listarPendentesEnvio() {
        if (log.isDebugEnabled()) log.debug("[PriceUpdateService] listarPendentesEnvio()");
        return priceUpdateRepository.findByStatusEnvioVtexFalseOrStatusEnvioVtexIsNull();
    }

    @Override
    @Transactional
    public List<PriceUpdate> listarPendentesEnvioPorLoja(Long lojaId) {
        if (log.isDebugEnabled()) log.debug("[PriceUpdateService] listarPendentesEnvioPorLoja() lojaId={}", lojaId);
        return priceUpdateRepository.findByLoja_LojaIdAndStatusEnvioVtexFalse(lojaId);
    }

    @Override
    @Transactional
    public int countPendentesEnvio() {
        int c = listarPendentesEnvio().size();
        if (log.isDebugEnabled()) log.debug("[PriceUpdateService] countPendentesEnvio() -> {}", c);
        return c;
    }

    @Override
    @Transactional
    public int countPendentesEnvioPorLoja(Long lojaId) {
        int c = listarPendentesEnvioPorLoja(lojaId).size();
        if (log.isDebugEnabled()) log.debug("[PriceUpdateService] countPendentesEnvioPorLoja({}) -> {}", lojaId, c);
        return c;
    }

    /* ===========================================================
       Domínio VTEX — por horário (mantido) + MDC
       =========================================================== */

    @Override
    @Transactional
    public void atualizarPrecosSelectHorario(String horarioUpdate) {
        final Instant t0 = Instant.now();
        log.info("[PriceUpdateService] Iniciando atualização por horário: {}", horarioUpdate);

        List<Loja> lojas = lojaRepository.findByEcommerceAtivoTrueAndHorarioPriceUpdate(horarioUpdate);
        log.info("[PriceUpdateService] Lojas ativas encontradas para {}: {}", horarioUpdate, lojas != null ? lojas.size() : 0);
        if (lojas == null || lojas.isEmpty()) {
            log.warn("[PriceUpdateService] Nenhuma loja ativa no horário: {}", horarioUpdate);
            return;
        }

        int totalSucesso = 0;
        int totalFalha   = 0;
        int totalProcessados = 0;

        for (Loja loja : lojas) {
            Long lojaId = loja.getLojaId();

            if (loja.getPoliticaComercial() == null) {
                log.warn("[PriceUpdateService] Loja {} sem política comercial. Pulando.", lojaId);
                continue;
            }

            Integer tradePolicyId = parsePoliticaComercial(loja.getPoliticaComercial());
            if (tradePolicyId == null) {
                log.warn("[PriceUpdateService] Política inválida p/ loja {}: {}", lojaId, loja.getPoliticaComercial());
                continue;
            }

            String codEconect = loja.getCodLojaEconect();
            String codRmsDg   = loja.getCodLojaRmsDg();

            if (log.isDebugEnabled()) {
                log.debug("[PriceUpdateService] Lendo view (lojaId={}, econect={}, rmsDg={})",
                          lojaId, codEconect, codRmsDg);
            }

            List<ProdutoPrecoDTO> produtos = new ArrayList<>();
            try {
                produtos = viewConsincoPrecoReader.listarProdutosComPrecosAtivos(lojaId, codEconect, codRmsDg);
            } catch (Exception ex) {
                log.error("[PriceUpdateService] Erro view Consinco (loja {}): {}", lojaId, ex.getMessage(), ex);
                continue;
            }

            log.info("[PriceUpdateService] Loja {} - produtos retornados: {}", lojaId, produtos.size());

            int lojaSucesso = 0;
            int lojaFalha   = 0;

            final int total = produtos.size();
            int processed = 0;
            final long t0Ns = System.nanoTime();
            long lastLog = t0Ns;

            for (ProdutoPrecoDTO p : produtos) {
                Integer sku = safeSku(p.getCodigo());
                if (sku == null) {
                    log.debug("[PriceUpdateService] SKU inválido/null: {}", p);
                    continue;
                }

                BigDecimal precoDe  = p.getPrecoDe() != null ? p.getPrecoDe() : BigDecimal.ZERO;
                BigDecimal precoPor = p.getPrecoPor() != null ? p.getPrecoPor() : BigDecimal.ZERO;
                if (precoDe.compareTo(BigDecimal.ZERO) == 0) {
                    precoDe = precoPor;
                }

                if (log.isDebugEnabled()) {
                    log.debug("[PriceUpdateService] Enviando SKU {} policy {} (precoDe={}, precoPor={})",
                              sku, tradePolicyId, precoDe, precoPor);
                }

                PriceUpdate entity = priceUpdateRepository
                        .findByLoja_LojaIdAndCodigo(lojaId, sku)
                        .orElseGet(PriceUpdate::new);

                entity.setLoja(loja);
                entity.setCodigo(sku);
                entity.setDescricao(p.getDescricao());
                entity.setListPrice(precoDe.doubleValue());
                entity.setValue(precoPor.doubleValue());
                entity.setMinQuantity(1);
                entity.setDataUltimoEnvio(LocalDateTime.now());
                entity.setStatusEnvioVtex(false); // assume falha até confirmar sucesso
                entity.setReprocessamento(null);

                // ✅ MDC antes da chamada VTEX
                processed++;
                MDC.put("lojaId", String.valueOf(loja.getLojaId()));
                MDC.put("lojaNome", loja.getNome() != null ? loja.getNome() : "-");
                MDC.put("policy", loja.getPoliticaComercial() != null ? loja.getPoliticaComercial() : "-");
                MDC.put("wh", loja.getWarehouse() != null ? loja.getWarehouse() : "-");
                MDC.put("batchPos", String.valueOf(processed));
                MDC.put("batchTotal", String.valueOf(total));
                MDC.put("okCount", String.valueOf(lojaSucesso));
                MDC.put("failCount", String.valueOf(lojaFalha));

                boolean sucesso = false;
                try {
                    PriceUpdateContext ctx = PriceUpdateContext.builder()
                            .skuId(sku.longValue())
                            .tradePolicyId(tradePolicyId)
                            .listPrice(precoDe)
                            .value(precoPor)
                            .from(LocalDateTime.now())
                            .nomeLoja(loja.getNome())
                            .politicaComercial(loja.getPoliticaComercial())
                            .warehouse(loja.getWarehouse())
                            .build();

                    sucesso = vtexPricingClient.upsertFixedPrice(ctx);
                } catch (VtexPricingException vpe) {
                    log.error("[PriceUpdateService] VTEX erro sku {} loja {}: {}", sku, lojaId, vpe.getMessage());
                    sucesso = false;
                } catch (Exception ex) {
                    log.error("[PriceUpdateService] Erro inesperado VTEX (sku {}, loja {}): {}",
                              sku, lojaId, ex.getMessage(), ex);
                    sucesso = false;
                } finally {
                    MDC.clear();
                }

                entity.setStatusEnvioVtex(sucesso);
                if (sucesso) {
                    lojaSucesso++; totalSucesso++;
                } else {
                    lojaFalha++; totalFalha++;
                    log.debug("[PriceUpdateService] Falha no envio SKU {} (loja {}). Marcado como pendente.", sku, lojaId);
                }
                totalProcessados++;

                priceUpdateRepository.save(entity);

                // 🔔 Progresso
                long now = System.nanoTime();
                boolean passoItens = (processed % PROGRESS_STEP_ITEMS) == 0;
                boolean passoTempo = (now - lastLog) >= PROGRESS_STEP_SECONDS * 1_000_000_000L;
                boolean terminou   = processed == total;

                if (passoItens || passoTempo || terminou) {
                    logProgressoLoja("prices", loja, processed, total, lojaSucesso, lojaFalha, t0Ns, lastLog);
                    lastLog = now;
                }
            }

            log.info("[PriceUpdateService] Loja {} - enviados OK: {} | falhas: {} | total processados: {}",
                     lojaId, lojaSucesso, lojaFalha, (lojaSucesso + lojaFalha));
        }

        log.info("[PriceUpdateService] Ciclo por horário finalizado em {} ms. TOTAL -> OK: {} | Falhas: {} | Processados: {}",
                 Duration.between(t0, Instant.now()).toMillis(), totalSucesso, totalFalha, totalProcessados);

        // Opcional (como no legado): reprocessar pendentes ao final do ciclo
        reprocessarPendentes();
    }

    /* ===========================================================
       Domínio VTEX — por loja (mantido) + NOVO método com relatório + MDC
       =========================================================== */

    @Override
    @Transactional
    public void atualizarPrecosPorLoja(Long lojaId) {
        final Instant t0 = Instant.now();

        final List<Loja> lojas;

        if (lojaId == null) {
            lojas = lojaRepository.findAll().stream()
                    .filter(l -> Boolean.TRUE.equals(l.getEcommerceAtivo()))
                    .collect(Collectors.toList());
            log.info("[PriceUpdateService] Atualização por loja: TODAS (ativas) => {} loja(s)", lojas.size());
            if (lojas.isEmpty()) {
                log.warn("[PriceUpdateService] Nenhuma loja ativa para e-commerce encontrada.");
                return;
            }
        } else {
            Loja l = lojaRepository.findById(lojaId).orElse(null);
            if (l == null) {
                log.warn("[PriceUpdateService] Loja {} não encontrada.", lojaId);
                return;
            }
            if (!Boolean.TRUE.equals(l.getEcommerceAtivo())) {
                log.warn("[PriceUpdateService] Loja {} não está ativa para e-commerce. Abortando.", lojaId);
                return;
            }
            lojas = List.of(l);
            log.info("[PriceUpdateService] Atualização por lojaId={} (ativa).", lojaId);
        }

        int totalSucesso = 0, totalFalha = 0, totalProcessados = 0;

        for (Loja loja : lojas) {
            Long lId = loja.getLojaId();

            if (loja.getPoliticaComercial() == null) {
                log.warn("[PriceUpdateService] Loja {} sem política comercial. Pulando.", lId);
                continue;
            }
            Integer tradePolicyId = parsePoliticaComercial(loja.getPoliticaComercial());
            if (tradePolicyId == null) {
                log.warn("[PriceUpdateService] Política inválida p/ loja {}: {}", lId, loja.getPoliticaComercial());
                continue;
            }

            String codEconect = loja.getCodLojaEconect();
            String codRmsDg   = loja.getCodLojaRmsDg();

            List<ProdutoPrecoDTO> produtos = new ArrayList<>();
            try {
                produtos = viewConsincoPrecoReader.listarProdutosComPrecosAtivos(lId, codEconect, codRmsDg);
            } catch (Exception ex) {
                log.error("[PriceUpdateService] Erro view Consinco (loja {}): {}", lId, ex.getMessage(), ex);
                continue;
            }

            log.info("[PriceUpdateService] Loja {} - produtos retornados: {}", lId, produtos.size());

            int lojaSucesso = 0, lojaFalha = 0;
            final int total = produtos.size();
            int processed = 0;

            final long t0Ns = System.nanoTime();
            long lastLog = t0Ns;

            for (ProdutoPrecoDTO p : produtos) {
                Integer sku = safeSku(p.getCodigo());
                if (sku == null) {
                    log.debug("[PriceUpdateService] SKU inválido/null: {}", p);
                    continue;
                }

                BigDecimal precoDe  = p.getPrecoDe() != null ? p.getPrecoDe() : BigDecimal.ZERO;
                BigDecimal precoPor = p.getPrecoPor() != null ? p.getPrecoPor() : BigDecimal.ZERO;
                if (precoDe.compareTo(BigDecimal.ZERO) == 0) precoDe = precoPor;

                PriceUpdate entity = priceUpdateRepository
                        .findByLoja_LojaIdAndCodigo(lId, sku)
                        .orElseGet(PriceUpdate::new);

                entity.setLoja(loja);
                entity.setCodigo(sku);
                entity.setDescricao(p.getDescricao());
                entity.setListPrice(precoDe.doubleValue());
                entity.setValue(precoPor.doubleValue());
                entity.setMinQuantity(1);
                entity.setDataUltimoEnvio(LocalDateTime.now());
                entity.setStatusEnvioVtex(false);
                entity.setReprocessamento(null);

                // ✅ MDC antes da chamada VTEX
                processed++;
                MDC.put("lojaId", String.valueOf(loja.getLojaId()));
                MDC.put("lojaNome", loja.getNome() != null ? loja.getNome() : "-");
                MDC.put("policy", loja.getPoliticaComercial() != null ? loja.getPoliticaComercial() : "-");
                MDC.put("wh", loja.getWarehouse() != null ? loja.getWarehouse() : "-");
                MDC.put("batchPos", String.valueOf(processed));
                MDC.put("batchTotal", String.valueOf(total));
                MDC.put("okCount", String.valueOf(lojaSucesso));
                MDC.put("failCount", String.valueOf(lojaFalha));

                boolean sucesso = false;
                try {
                    PriceUpdateContext ctx = PriceUpdateContext.builder()
                            .skuId(sku.longValue())
                            .tradePolicyId(tradePolicyId)
                            .listPrice(precoDe)
                            .value(precoPor)
                            .from(LocalDateTime.now())
                            .nomeLoja(loja.getNome())
                            .politicaComercial(loja.getPoliticaComercial())
                            .warehouse(loja.getWarehouse())
                            .build();

                    sucesso = vtexPricingClient.upsertFixedPrice(ctx);
                } catch (Exception ex) {
                    log.error("[PriceUpdateService] Erro VTEX (loja {}, sku {}): {}", lId, sku, ex.getMessage());
                    sucesso = false;
                } finally {
                    MDC.clear();
                }

                entity.setStatusEnvioVtex(sucesso);
                if (sucesso) { lojaSucesso++; totalSucesso++; } else { lojaFalha++; totalFalha++; }
                totalProcessados++;

                priceUpdateRepository.save(entity);

                // 🔔 Progresso
                long now = System.nanoTime();
                boolean passoItens = (processed % PROGRESS_STEP_ITEMS) == 0;
                boolean passoTempo = (now - lastLog) >= PROGRESS_STEP_SECONDS * 1_000_000_000L;
                boolean terminou   = processed == total;

                if (passoItens || passoTempo || terminou) {
                    logProgressoLoja("prices", loja, processed, total, lojaSucesso, lojaFalha, t0Ns, lastLog);
                    lastLog = now;
                }
            }

            log.info("[PriceUpdateService] Loja {} - OK: {} | Falhas: {} | Total: {}", lId, lojaSucesso, lojaFalha, (lojaSucesso + lojaFalha));
        }

        log.info("[PriceUpdateService] Execução por loja finalizada em {} ms. TOTAL -> OK: {} | Falhas: {} | Processados: {}",
                Duration.between(t0, Instant.now()).toMillis(), totalSucesso, totalFalha, totalProcessados);

        // opcional: reprocessar pendentes
        reprocessarPendentes();
    }

    /** ✅ NOVO: executa por loja e devolve um resumo com contadores (para e-mail). */
    @Transactional
    public PriceUpdateRunResult atualizarPrecosPorLojaComRelatorio(Long lojaId) {
        PriceUpdateRunResult resumo = new PriceUpdateRunResult();
        resumo.setInicio(LocalDateTime.now());

        Loja loja = lojaRepository.findById(lojaId).orElse(null);
        if (loja == null) {
            resumo.setObservacoes("Loja não encontrada.");
            resumo.setFim(LocalDateTime.now());
            return resumo;
        }
        resumo.setLojaId(loja.getLojaId());
        resumo.setLojaNome(loja.getNome());
        resumo.setPoliticaComercial(loja.getPoliticaComercial());
        resumo.setWarehouse(loja.getWarehouse());

        if (!Boolean.TRUE.equals(loja.getEcommerceAtivo())) {
            resumo.setObservacoes("Loja inativa no e-commerce.");
            resumo.setFim(LocalDateTime.now());
            return resumo;
        }

        Integer tradePolicyId = parsePoliticaComercial(loja.getPoliticaComercial());
        if (tradePolicyId == null) {
            resumo.setObservacoes("Política comercial inválida.");
            resumo.setFim(LocalDateTime.now());
            return resumo;
        }

        String codEconect = loja.getCodLojaEconect();
        String codRmsDg   = loja.getCodLojaRmsDg();

        List<ProdutoPrecoDTO> produtos;
        try {
            produtos = viewConsincoPrecoReader.listarProdutosComPrecosAtivos(lojaId, codEconect, codRmsDg);
        } catch (Exception ex) {
            log.error("[PriceUpdateService] Erro view Consinco (loja {}): {}", lojaId, ex.getMessage(), ex);
            resumo.setObservacoes("Erro ao consultar view Consinco: " + ex.getMessage());
            resumo.setFim(LocalDateTime.now());
            return resumo;
        }

        resumo.setQtdConsultados(produtos.size());

        int ok = 0, falha = 0, processados = 0;

        final int total = produtos.size();

        for (ProdutoPrecoDTO p : produtos) {
            Integer sku = safeSku(p.getCodigo());
            if (sku == null) continue;

            BigDecimal precoDe  = p.getPrecoDe() != null ? p.getPrecoDe() : BigDecimal.ZERO;
            BigDecimal precoPor = p.getPrecoPor() != null ? p.getPrecoPor() : BigDecimal.ZERO;
            if (precoDe.compareTo(BigDecimal.ZERO) == 0) precoDe = precoPor;

            PriceUpdate entity = priceUpdateRepository
                    .findByLoja_LojaIdAndCodigo(lojaId, sku)
                    .orElseGet(PriceUpdate::new);

            entity.setLoja(loja);
            entity.setCodigo(sku);
            entity.setDescricao(p.getDescricao());
            entity.setListPrice(precoDe.doubleValue());
            entity.setValue(precoPor.doubleValue());
            entity.setMinQuantity(1);
            entity.setDataUltimoEnvio(LocalDateTime.now());
            entity.setStatusEnvioVtex(false);
            entity.setReprocessamento(null);

            // ✅ MDC por item
            processados++;
            MDC.put("lojaId", String.valueOf(loja.getLojaId()));
            MDC.put("lojaNome", loja.getNome() != null ? loja.getNome() : "-");
            MDC.put("policy", loja.getPoliticaComercial() != null ? loja.getPoliticaComercial() : "-");
            MDC.put("wh", loja.getWarehouse() != null ? loja.getWarehouse() : "-");
            MDC.put("batchPos", String.valueOf(processados));
            MDC.put("batchTotal", String.valueOf(total));
            MDC.put("okCount", String.valueOf(ok));
            MDC.put("failCount", String.valueOf(falha));

            boolean sucesso = false;
            try {
                PriceUpdateContext ctx = PriceUpdateContext.builder()
                        .skuId(sku.longValue())
                        .tradePolicyId(tradePolicyId)
                        .listPrice(precoDe)
                        .value(precoPor)
                        .from(LocalDateTime.now())
                        .nomeLoja(loja.getNome())
                        .politicaComercial(loja.getPoliticaComercial())
                        .warehouse(loja.getWarehouse())
                        .build();

                sucesso = vtexPricingClient.upsertFixedPrice(ctx);
            } catch (Exception ex) {
                sucesso = false;
            } finally {
                MDC.clear();
            }

            entity.setStatusEnvioVtex(sucesso);
            if (sucesso) ok++; else falha++;

            priceUpdateRepository.save(entity);
        }

        resumo.setQtdEnviadosOk(ok);
        resumo.setQtdFalhaEnvio(falha);
        resumo.setQtdProcessadosTotal(processados);

        // reprocesso somente dessa loja
        int[] rep = reprocessarPendentesPorLoja(lojaId);
        resumo.setQtdReprocessadosOk(rep[0]);
        resumo.setQtdReprocessadosFalha(rep[1]);

        resumo.setFim(LocalDateTime.now());
        return resumo;
    }

    /* ===========================================================
       Reprocessamento
       =========================================================== */

    @Override
    @Transactional
    public void reprocessarPendentes() {
        final Instant t0 = Instant.now();
        List<PriceUpdate> pendentes = listarPendentesEnvio();
        if (pendentes.isEmpty()) {
            log.info("[PriceUpdateService] Sem pendências para reprocessar.");
            return;
        }

        int ok = 0;
        int falha = 0;

        log.info("[PriceUpdateService] Reprocessando {} pendência(s)...", pendentes.size());

        final int total = pendentes.size();
        int processed = 0;
        final long t0Ns = System.nanoTime();
        long lastLog = t0Ns;

        for (PriceUpdate pu : pendentes) {
            Loja loja = pu.getLoja();
            if (loja == null || loja.getPoliticaComercial() == null) {
                log.debug("[PriceUpdateService] Ignorando pendência sem loja/política: {}", pu.getPriceUpdateId());
                processed++;
                continue;
            }

            Integer tradePolicyId = parsePoliticaComercial(loja.getPoliticaComercial());
            if (tradePolicyId == null) {
                log.debug("[PriceUpdateService] Política inválida (lojaId={}): pendência {}", loja.getLojaId(), pu.getPriceUpdateId());
                processed++;
                continue;
            }

            ProdutoPrecoDTO atual = null;
            try {
                atual = viewConsincoPrecoReader.buscarPrecoAtual(
                            loja.getLojaId(),
                            loja.getCodLojaEconect(),
                            loja.getCodLojaRmsDg(),
                            pu.getCodigo());
            } catch (Exception ex) {
                log.error("[PriceUpdateService] Erro view no reprocesso (loja {}, sku {}): {}",
                          loja.getLojaId(), pu.getCodigo(), ex.getMessage());
            }
            if (atual == null) {
                log.debug("[PriceUpdateService] Sem preço atual para reprocesso (loja {}, sku {}).", loja.getLojaId(), pu.getCodigo());
                processed++;
                continue;
            }

            BigDecimal precoDe  = atual.getPrecoDe() != null ? atual.getPrecoDe() : BigDecimal.ZERO;
            BigDecimal precoPor = atual.getPrecoPor() != null ? atual.getPrecoPor() : BigDecimal.ZERO;
            if (precoDe.compareTo(BigDecimal.ZERO) == 0) {
                precoDe = precoPor;
            }

            // ✅ MDC no reprocesso
            processed++;
            MDC.put("lojaId", String.valueOf(loja.getLojaId()));
            MDC.put("lojaNome", loja.getNome() != null ? loja.getNome() : "-");
            MDC.put("policy", loja.getPoliticaComercial() != null ? loja.getPoliticaComercial() : "-");
            MDC.put("wh", loja.getWarehouse() != null ? loja.getWarehouse() : "-");
            MDC.put("batchPos", String.valueOf(processed));
            MDC.put("batchTotal", String.valueOf(total));
            MDC.put("okCount", String.valueOf(ok));
            MDC.put("failCount", String.valueOf(falha));

            boolean sucesso = false;
            try {
                PriceUpdateContext ctx = PriceUpdateContext.builder()
                        .skuId(pu.getCodigo().longValue())
                        .tradePolicyId(tradePolicyId)
                        .listPrice(precoDe)
                        .value(precoPor)
                        .from(LocalDateTime.now())
                        .nomeLoja(loja.getNome())
                        .politicaComercial(loja.getPoliticaComercial())
                        .warehouse(loja.getWarehouse())
                        .build();

                sucesso = vtexPricingClient.upsertFixedPrice(ctx);
            } catch (Exception ex) {
                log.error("[PriceUpdateService] Erro VTEX no reprocesso (loja {}, sku {}): {}",
                          loja.getLojaId(), pu.getCodigo(), ex.getMessage());
                sucesso = false;
            } finally {
                MDC.clear();
            }

            pu.setListPrice(precoDe.doubleValue());
            pu.setValue(precoPor.doubleValue());
            pu.setDataUltimoEnvio(LocalDateTime.now());
            pu.setStatusEnvioVtex(sucesso);
            pu.setReprocessamento(sucesso);

            if (sucesso) ok++; else falha++;

            priceUpdateRepository.save(pu);

            // 🔔 Progresso do reprocesso
            long now = System.nanoTime();
            boolean passoItens = (processed % PROGRESS_STEP_ITEMS) == 0;
            boolean passoTempo = (now - lastLog) >= PROGRESS_STEP_SECONDS * 1_000_000_000L;
            boolean terminou   = processed == total;

            if (passoItens || passoTempo || terminou) {
                if (loja != null) {
                    logProgressoLoja("reprocess", loja, processed, total, ok, falha, t0Ns, lastLog);
                } else {
                    int percent = total > 0 ? (processed * 100 / total) : 100;
                    log.info("[reprocess][progresso] {}/{} ({}%) ok={} falha={}",
                            processed, total, percent, ok, falha);
                }
                lastLog = now;
            }
        }

        log.info("[PriceUpdateService] Reprocessamento concluído em {} ms. OK: {} | Falhas: {} | Total: {}",
                 Duration.between(t0, Instant.now()).toMillis(), ok, falha, ok + falha);
    }

    /** ✅ NOVO: reprocessa apenas pendências de UMA loja e retorna [ok, falha]. */
    @Transactional
    public int[] reprocessarPendentesPorLoja(Long lojaId) {
        List<PriceUpdate> pendentes = priceUpdateRepository.findByLoja_LojaIdAndStatusEnvioVtexFalse(lojaId);
        if (pendentes.isEmpty()) return new int[]{0,0};

        int ok = 0;
        int falha = 0;

        Loja loja = lojaRepository.findById(lojaId).orElse(null);
        if (loja == null || loja.getPoliticaComercial() == null) return new int[]{0, pendentes.size()};

        Integer tradePolicyId = parsePoliticaComercial(loja.getPoliticaComercial());
        if (tradePolicyId == null) return new int[]{0, pendentes.size()};

        final int total = pendentes.size();
        int processed = 0;

        for (PriceUpdate pu : pendentes) {

            ProdutoPrecoDTO atual = null;
            try {
                atual = viewConsincoPrecoReader.buscarPrecoAtual(
                        loja.getLojaId(),
                        loja.getCodLojaEconect(),
                        loja.getCodLojaRmsDg(),
                        pu.getCodigo());
            } catch (Exception ex) {
                falha++;
                processed++;
                continue;
            }
            if (atual == null) { falha++; processed++; continue; }

            BigDecimal precoDe  = atual.getPrecoDe() != null ? atual.getPrecoDe() : BigDecimal.ZERO;
            BigDecimal precoPor = atual.getPrecoPor() != null ? atual.getPrecoPor() : BigDecimal.ZERO;
            if (precoDe.compareTo(BigDecimal.ZERO) == 0) precoDe = precoPor;

            // ✅ MDC
            processed++;
            MDC.put("lojaId", String.valueOf(loja.getLojaId()));
            MDC.put("lojaNome", loja.getNome() != null ? loja.getNome() : "-");
            MDC.put("policy", loja.getPoliticaComercial() != null ? loja.getPoliticaComercial() : "-");
            MDC.put("wh", loja.getWarehouse() != null ? loja.getWarehouse() : "-");
            MDC.put("batchPos", String.valueOf(processed));
            MDC.put("batchTotal", String.valueOf(total));
            MDC.put("okCount", String.valueOf(ok));
            MDC.put("failCount", String.valueOf(falha));

            boolean sucesso;
            try {
                PriceUpdateContext ctx = PriceUpdateContext.builder()
                        .skuId(pu.getCodigo().longValue())
                        .tradePolicyId(tradePolicyId)
                        .listPrice(precoDe)
                        .value(precoPor)
                        .from(LocalDateTime.now())
                        .nomeLoja(loja.getNome())
                        .politicaComercial(loja.getPoliticaComercial())
                        .warehouse(loja.getWarehouse())
                        .build();

                sucesso = vtexPricingClient.upsertFixedPrice(ctx);
            } catch (Exception ex) {
                sucesso = false;
            } finally {
                MDC.clear();
            }

            pu.setListPrice(precoDe.doubleValue());
            pu.setValue(precoPor.doubleValue());
            pu.setDataUltimoEnvio(LocalDateTime.now());
            pu.setStatusEnvioVtex(sucesso);
            pu.setReprocessamento(sucesso);

            if (sucesso) ok++; else falha++;

            priceUpdateRepository.save(pu);
        }

        return new int[]{ok, falha};
    }

    /* ===========================================================
       Utilitários internos
       =========================================================== */

    private Integer parsePoliticaComercial(String politicaComercial) {
        try {
            return Integer.valueOf(politicaComercial.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private Integer safeSku(Object v) {
        try {
            if (v == null) return null;
            if (v instanceof Integer) return (Integer) v;
            if (v instanceof Long) return ((Long) v).intValue();
            if (v instanceof String) return Integer.valueOf(((String) v).trim());
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    // 🔧 Helper para logs de progresso por loja (percentual, taxa e ETA)
    private void logProgressoLoja(String prefixo, Loja loja,
                                  int processed, int total, int ok, int falha,
                                  long t0Nano, long lastLogNano) {

        long now = System.nanoTime();
        double elapsedSec = Math.max(0.001, (now - t0Nano) / 1_000_000_000.0);
        double rate = processed / elapsedSec; // itens/s
        int percent = total > 0 ? (processed * 100 / total) : 100;

        int remaining = Math.max(0, total - processed);
        double etaSec = remaining / Math.max(0.001, rate);
        String etaStr = String.format("%.0fs", etaSec);

        String nome = loja.getNome() != null ? loja.getNome() : "-";
        String pol  = loja.getPoliticaComercial() != null ? loja.getPoliticaComercial() : "-";
        String wh   = loja.getWarehouse() != null ? loja.getWarehouse() : "-";

        log.info("[{}][progresso] lojaId={} nome='{}' policy={} wh='{}' {}/{} ({}%) ok={} falha={} rate={}/s ETA={}",
                prefixo, loja.getLojaId(), nome, pol, wh,
                processed, total, percent, ok, falha,
                String.format("%.1f", rate), etaStr);
    }
}
