// src/main/java/sistema/ecommerce/primefaces/service/InventoryUpdateService.java
package sistema.rotinas.primefaces.service;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.persistence.*;
import jakarta.transaction.Transactional;

import org.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sistema.rotinas.primefaces.consinco.ViewEcommerceEstoqueReader;
import sistema.rotinas.primefaces.dto.InventoryUpdateRunResult;
import sistema.rotinas.primefaces.dto.ProdutoEstoqueDTO;
import sistema.rotinas.primefaces.model.InventoryUpdate;
import sistema.rotinas.primefaces.model.Loja;
import sistema.rotinas.primefaces.repository.InventoryUpdateRepository;
import sistema.rotinas.primefaces.repository.LojaRepository;
import sistema.rotinas.primefaces.service.interfaces.IInventoryUpdateService;
import sistema.rotinas.primefaces.vtex.*;

@Service
public class InventoryUpdateService implements IInventoryUpdateService {
	private static final Logger log = LoggerFactory.getLogger(InventoryUpdateService.class);

	private static final int PROGRESS_STEP_ITEMS = 200;
	private static final long PROGRESS_STEP_SECONDS = 15L;

	@Autowired
	private InventoryUpdateRepository repository;
	@Autowired
	private LojaRepository lojaRepository;
	@Autowired
	private ViewEcommerceEstoqueReader estoqueReader;
	@Autowired
	private VtexInventoryClient vtexInventoryClient;

	@PersistenceContext
	private EntityManager em;

	/* CRUD */
	@Override
	@Transactional
	public List<InventoryUpdate> getAll() {
		return repository.findAll();
	}

	@Override
	@Transactional
	public InventoryUpdate save(InventoryUpdate e) {
		if (e.getLoja() != null && e.getCodigo() != null) {
			Optional<InventoryUpdate> exist = repository.findByLoja_LojaIdAndCodigo(e.getLoja().getLojaId(),
					e.getCodigo());
			if (exist.isPresent()) {
				InventoryUpdate x = exist.get();
				x.setDescricao(e.getDescricao());
				x.setQuantidade(e.getQuantidade());
				x.setDataUltimoEnvio(e.getDataUltimoEnvio());
				x.setStatusEnvioVtex(e.getStatusEnvioVtex());
				x.setReprocessamento(e.getReprocessamento());
				return repository.save(x);
			}
		}
		return repository.save(e);
	}

	@Override
	@Transactional
	public InventoryUpdate findById(Long id) {
		return repository.findById(id).orElse(null);
	}

	@Override
	@Transactional
	public void deleteById(Long id) {
		repository.deleteById(id);
	}

	@Override
	@Transactional
	public InventoryUpdate update(InventoryUpdate e) {
		if (e.getInventoryUpdateId() == null)
			throw new IllegalArgumentException("ID obrigatório");
		if (!repository.existsById(e.getInventoryUpdateId()))
			throw new IllegalArgumentException("Registro não encontrado");
		return repository.save(e);
	}

	/* Paginação/critério */
	@Override
	@Transactional
	public List<InventoryUpdate> findAll(int first, int pageSize, String sortField, boolean ascendente) {
		String sql = "SELECT * FROM inventory_update";
		if (sortField != null)
			sql += " ORDER BY " + sortField + (ascendente ? " ASC" : " DESC");
		Query q = em.createNativeQuery(sql, InventoryUpdate.class);
		q.setFirstResult(first);
		q.setMaxResults(pageSize);
		@SuppressWarnings("unchecked")
		List<InventoryUpdate> r = q.getResultList();
		return r;
	}

	@Override
	@Transactional
	public int count() {
		Number n = (Number) em.createNativeQuery("SELECT COUNT(*) FROM inventory_update").getSingleResult();
		return n.intValue();
	}

	@Override
	@Transactional
	public List<InventoryUpdate> findByCriteria(String campo, String condicao, String valor, int first, int pageSize,
			String sortField, boolean ascendente) {
		if (campo == null || campo.isBlank() || condicao == null || condicao.isBlank())
			return findAll(first, pageSize, sortField, ascendente);

		String sql = "SELECT * FROM inventory_update WHERE " + campo
				+ (condicao.equals("equal") ? " = :v" : " LIKE :v");
		if (sortField != null)
			sql += " ORDER BY " + sortField + (ascendente ? " ASC" : " DESC");
		Query q = em.createNativeQuery(sql, InventoryUpdate.class);
		q.setParameter("v", condicao.equals("equal") ? valor : "%" + valor + "%");
		q.setFirstResult(first);
		q.setMaxResults(pageSize);
		@SuppressWarnings("unchecked")
		List<InventoryUpdate> r = q.getResultList();
		return r;
	}

	@Override
	@Transactional
	public int countByCriteria(String campo, String condicao, String valor) {
		if (campo == null || campo.isBlank() || condicao == null || condicao.isBlank())
			return count();
		String sql = "SELECT COUNT(*) FROM inventory_update WHERE " + campo
				+ (condicao.equals("equal") ? " = :v" : " LIKE :v");
		Query q = em.createNativeQuery(sql);
		q.setParameter("v", condicao.equals("equal") ? valor : "%" + valor + "%");
		Number n = (Number) q.getSingleResult();
		return n.intValue();
	}

	/* Consultas úteis */
	@Override
	@Transactional
	public Optional<InventoryUpdate> findByLojaAndCodigo(Long lojaId, Integer codigo) {
		return repository.findByLoja_LojaIdAndCodigo(lojaId, codigo);
	}

	@Override
	@Transactional
	public List<InventoryUpdate> listarPendentesEnvio() {
		return repository.findByStatusEnvioVtexFalseOrStatusEnvioVtexIsNull();
	}

	@Override
	@Transactional
	public List<InventoryUpdate> listarPendentesEnvioPorLoja(Long lojaId) {
		return repository.findByLoja_LojaIdAndStatusEnvioVtexFalse(lojaId);
	}

	@Override
	@Transactional
	public int countPendentesEnvio() {
		return listarPendentesEnvio().size();
	}

	@Override
	@Transactional
	public int countPendentesEnvioPorLoja(Long lojaId) {
		return listarPendentesEnvioPorLoja(lojaId).size();
	}

	/* Execução principal — por loja */
	@Override
	@Transactional
	public void atualizarEstoquePorLoja(Long lojaId) {
		final Instant t0 = Instant.now();

		final List<Loja> lojas;
		if (lojaId == null) {
			lojas = lojaRepository.findAll().stream().filter(l -> Boolean.TRUE.equals(l.getEcommerceAtivo()))
					.collect(Collectors.toList());
			if (lojas.isEmpty()) {
				log.warn("[Inventory] Nenhuma loja ativa.");
				return;
			}
		} else {
			Loja l = lojaRepository.findById(lojaId).orElse(null);
			if (l == null || !Boolean.TRUE.equals(l.getEcommerceAtivo())) {
				log.warn("[Inventory] Loja inválida/inativa.");
				return;
			}
			lojas = List.of(l);
		}

		int totOK = 0, totFail = 0, totProc = 0;

		for (Loja loja : lojas) {
			if (loja.getWarehouse() == null || loja.getWarehouse().isBlank()) {
				log.warn("[Inventory] Loja {} sem warehouse. Pulando.", loja.getLojaId());
				continue;
			}

			List<ProdutoEstoqueDTO> itens;
			try {
				itens = estoqueReader.listarProdutosComEstoque(loja.getLojaId(), loja.getCodLojaEconect(),
						loja.getCodLojaRmsDg());
			} catch (Exception ex) {
				log.error("[Inventory] Erro view estoque (loja {}): {}", loja.getLojaId(), ex.getMessage(), ex);
				continue;
			}

			int ok = 0, fail = 0, processed = 0, total = itens.size();
			long t0Ns = System.nanoTime(), lastLog = t0Ns;

			for (ProdutoEstoqueDTO p : itens) {
				Integer sku = safeSku(p.getCodigo());
				if (sku == null)
					continue;
				int qty = (p.getQuantidade() != null ? p.getQuantidade().intValue() : 0);

				InventoryUpdate entity = repository.findByLoja_LojaIdAndCodigo(loja.getLojaId(), sku)
						.orElseGet(InventoryUpdate::new);
				entity.setLoja(loja);
				entity.setCodigo(sku);
				entity.setDescricao(p.getDescricao());
				entity.setQuantidade(qty);
				entity.setDataUltimoEnvio(LocalDateTime.now());
				entity.setStatusEnvioVtex(false);
				entity.setReprocessamento(null);

				processed++;
				MDC.put("lojaId", String.valueOf(loja.getLojaId()));
				MDC.put("lojaNome", nz(loja.getNome()));
				MDC.put("wh", nz(loja.getWarehouse()));
				MDC.put("batchPos", String.valueOf(processed));
				MDC.put("batchTotal", String.valueOf(total));
				MDC.put("okCount", String.valueOf(ok));
				MDC.put("failCount", String.valueOf(fail));

				boolean sucesso;
				try {
					InventoryUpdateContext ctx = InventoryUpdateContext.builder().skuId(sku.longValue())
							.warehouse(loja.getWarehouse()).quantity(qty).nomeLoja(loja.getNome()).build();
					sucesso = vtexInventoryClient.updateStock(ctx);
				} catch (Exception ex) {
					log.error("[Inventory] VTEX erro loja {} sku {}: {}", loja.getLojaId(), sku, ex.getMessage());
					sucesso = false;
				} finally {
					MDC.clear();
				}

				entity.setStatusEnvioVtex(sucesso);
				if (sucesso) {
					ok++;
					totOK++;
				} else {
					fail++;
					totFail++;
				}
				totProc++;

				repository.save(entity);

				long now = System.nanoTime();
				boolean passoItens = (processed % PROGRESS_STEP_ITEMS) == 0;
				boolean passoTempo = (now - lastLog) >= PROGRESS_STEP_SECONDS * 1_000_000_000L;
				boolean terminou = processed == total;
				if (passoItens || passoTempo || terminou) {
					logProgressoLoja("inventory", loja, processed, total, ok, fail, t0Ns);
					lastLog = now;
				}
			}
		}

		log.info("[Inventory] Finalizado em {} ms. OK={} Falha={} Total={}",
				Duration.between(t0, Instant.now()).toMillis(), totOK, totFail, totProc);

		// opcional
		reprocessarPendentes();
	}

	@Override
	@Transactional
	public InventoryUpdateRunResult atualizarEstoquePorLojaComRelatorio(Long lojaId) {
		InventoryUpdateRunResult r = new InventoryUpdateRunResult();
		r.setInicio(LocalDateTime.now());

		Loja loja = lojaRepository.findById(lojaId).orElse(null);
		if (loja == null) {
			r.setObservacoes("Loja não encontrada.");
			r.setFim(LocalDateTime.now());
			return r;
		}
		if (!Boolean.TRUE.equals(loja.getEcommerceAtivo())) {
			r.setObservacoes("Loja inativa.");
			r.setFim(LocalDateTime.now());
			return r;
		}
		if (loja.getWarehouse() == null || loja.getWarehouse().isBlank()) {
			r.setObservacoes("Warehouse não configurado.");
			r.setFim(LocalDateTime.now());
			return r;
		}

		r.setLojaId(loja.getLojaId());
		r.setLojaNome(loja.getNome());
		r.setWarehouse(loja.getWarehouse());

		List<ProdutoEstoqueDTO> itens;
		try {
			itens = estoqueReader.listarProdutosComEstoque(loja.getLojaId(), loja.getCodLojaEconect(),
					loja.getCodLojaRmsDg());
		} catch (Exception ex) {
			r.setObservacoes("Erro view estoque: " + ex.getMessage());
			r.setFim(LocalDateTime.now());
			return r;
		}

		r.setQtdConsultados(itens.size());
		int ok = 0, fail = 0, proc = 0, total = itens.size();

		for (ProdutoEstoqueDTO p : itens) {
			Integer sku = safeSku(p.getCodigo());
			if (sku == null)
				continue;
			int qty = (p.getQuantidade() != null ? p.getQuantidade().intValue() : 0);

			InventoryUpdate entity = repository.findByLoja_LojaIdAndCodigo(lojaId, sku).orElseGet(InventoryUpdate::new);
			entity.setLoja(loja);
			entity.setCodigo(sku);
			entity.setDescricao(p.getDescricao());
			entity.setQuantidade(qty);
			entity.setDataUltimoEnvio(LocalDateTime.now());
			entity.setStatusEnvioVtex(false);
			entity.setReprocessamento(null);

			proc++;
			MDC.put("lojaId", String.valueOf(loja.getLojaId()));
			MDC.put("lojaNome", nz(loja.getNome()));
			MDC.put("wh", nz(loja.getWarehouse()));
			MDC.put("batchPos", String.valueOf(proc));
			MDC.put("batchTotal", String.valueOf(total));
			MDC.put("okCount", String.valueOf(ok));
			MDC.put("failCount", String.valueOf(fail));

			boolean sucesso;
			try {
				InventoryUpdateContext ctx = InventoryUpdateContext.builder().skuId(sku.longValue())
						.warehouse(loja.getWarehouse()).quantity(qty).nomeLoja(loja.getNome()).build();
				sucesso = vtexInventoryClient.updateStock(ctx);
			} catch (Exception ex) {
				sucesso = false;
			} finally {
				MDC.clear();
			}

			entity.setStatusEnvioVtex(sucesso);
			if (sucesso)
				ok++;
			else
				fail++;

			repository.save(entity);
		}

		r.setQtdEnviadosOk(ok);
		r.setQtdFalhaEnvio(fail);
		r.setQtdProcessadosTotal(proc);

		int[] rep = reprocessarPendentesPorLoja(lojaId);
		r.setQtdReprocessadosOk(rep[0]);
		r.setQtdReprocessadosFalha(rep[1]);

		r.setFim(LocalDateTime.now());
		return r;
	}

	/* Reprocesso */
	@Override
	@Transactional
	public void reprocessarPendentes() {
		List<InventoryUpdate> pend = listarPendentesEnvio();
		if (pend.isEmpty()) {
			log.info("[Inventory] Sem pendências.");
			return;
		}

		int ok = 0, fail = 0, total = pend.size(), proc = 0;
		for (InventoryUpdate iu : pend) {
			Loja loja = iu.getLoja();
			if (loja == null || loja.getWarehouse() == null || loja.getWarehouse().isBlank()) {
				proc++;
				continue;
			}

			ProdutoEstoqueDTO atual = null;
			try {
				atual = estoqueReader.buscarEstoqueAtual(loja.getLojaId(), loja.getCodLojaEconect(),
						loja.getCodLojaRmsDg(), iu.getCodigo());
			} catch (Exception ex) {
				proc++;
				continue;
			}
			if (atual == null) {
				proc++;
				continue;
			}

			int qty = atual.getQuantidade() != null ? atual.getQuantidade().intValue() : 0;

			proc++;
			MDC.put("lojaId", String.valueOf(loja.getLojaId()));
			MDC.put("lojaNome", nz(loja.getNome()));
			MDC.put("wh", nz(loja.getWarehouse()));
			MDC.put("batchPos", String.valueOf(proc));
			MDC.put("batchTotal", String.valueOf(total));
			MDC.put("okCount", String.valueOf(ok));
			MDC.put("failCount", String.valueOf(fail));

			boolean sucesso;
			try {
				InventoryUpdateContext ctx = InventoryUpdateContext.builder().skuId(iu.getCodigo().longValue())
						.warehouse(loja.getWarehouse()).quantity(qty).nomeLoja(loja.getNome()).build();
				sucesso = vtexInventoryClient.updateStock(ctx);
			} catch (Exception ex) {
				sucesso = false;
			} finally {
				MDC.clear();
			}

			iu.setQuantidade(qty);
			iu.setDataUltimoEnvio(LocalDateTime.now());
			iu.setStatusEnvioVtex(sucesso);
			iu.setReprocessamento(sucesso);

			if (sucesso)
				ok++;
			else
				fail++;

			repository.save(iu);
		}

		log.info("[Inventory] Reprocesso concluído: ok={} falha={} total={}", ok, fail, ok + fail);
	}

	@Override
	@Transactional
	public int[] reprocessarPendentesPorLoja(Long lojaId) {
		List<InventoryUpdate> pend = repository.findByLoja_LojaIdAndStatusEnvioVtexFalse(lojaId);
		if (pend.isEmpty())
			return new int[] { 0, 0 };

		Loja loja = lojaRepository.findById(lojaId).orElse(null);
		if (loja == null || loja.getWarehouse() == null || loja.getWarehouse().isBlank())
			return new int[] { 0, pend.size() };

		int ok = 0, fail = 0, total = pend.size(), proc = 0;

		for (InventoryUpdate iu : pend) {
			ProdutoEstoqueDTO atual = null;
			try {
				atual = estoqueReader.buscarEstoqueAtual(loja.getLojaId(), loja.getCodLojaEconect(),
						loja.getCodLojaRmsDg(), iu.getCodigo());
			} catch (Exception ex) {
				fail++;
				continue;
			}
			if (atual == null) {
				fail++;
				continue;
			}

			int qty = atual.getQuantidade() != null ? atual.getQuantidade().intValue() : 0;

			proc++;
			MDC.put("lojaId", String.valueOf(loja.getLojaId()));
			MDC.put("lojaNome", nz(loja.getNome()));
			MDC.put("wh", nz(loja.getWarehouse()));
			MDC.put("batchPos", String.valueOf(proc));
			MDC.put("batchTotal", String.valueOf(total));
			MDC.put("okCount", String.valueOf(ok));
			MDC.put("failCount", String.valueOf(fail));

			boolean sucesso;
			try {
				InventoryUpdateContext ctx = InventoryUpdateContext.builder().skuId(iu.getCodigo().longValue())
						.warehouse(loja.getWarehouse()).quantity(qty).nomeLoja(loja.getNome()).build();
				sucesso = vtexInventoryClient.updateStock(ctx);
			} catch (Exception ex) {
				sucesso = false;
			} finally {
				MDC.clear();
			}

			iu.setQuantidade(qty);
			iu.setDataUltimoEnvio(LocalDateTime.now());
			iu.setStatusEnvioVtex(sucesso);
			iu.setReprocessamento(sucesso);

			if (sucesso)
				ok++;
			else
				fail++;

			repository.save(iu);
		}
		return new int[] { ok, fail };
	}

	/* Utils */
	private Integer safeSku(Object v) {
		try {
			if (v == null)
				return null;
			if (v instanceof Integer i)
				return i;
			if (v instanceof Long l)
				return l.intValue();
			if (v instanceof String s)
				return Integer.valueOf(s.trim());
			return null;
		} catch (Exception e) {
			return null;
		}
	}

	private String nz(String v) {
		return (v == null || v.isBlank()) ? "-" : v;
	}

	private void logProgressoLoja(String prefixo, Loja loja, int processed, int total, int ok, int falha, long t0Nano) {
		long now = System.nanoTime();
		double elapsedSec = Math.max(0.001, (now - t0Nano) / 1_000_000_000.0);
		double rate = processed / elapsedSec;
		int percent = total > 0 ? (processed * 100 / total) : 100;
		log.info("[{}][progresso] lojaId={} nome='{}' wh='{}' {}/{} ({}%) ok={} falha={} rate={}/s", prefixo,
				loja.getLojaId(), nz(loja.getNome()), nz(loja.getWarehouse()), processed, total, percent, ok, falha,
				String.format("%.1f", rate));
	}
}
