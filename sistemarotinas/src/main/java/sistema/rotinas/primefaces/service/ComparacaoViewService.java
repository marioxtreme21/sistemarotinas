package sistema.rotinas.primefaces.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import sistema.rotinas.primefaces.dto.ProdutoEcommerceExternoDTO;

@Service
public class ComparacaoViewService {

	private static final Logger log = LoggerFactory.getLogger(ComparacaoViewService.class);

	// Base A = interna (possui DB-Link @rms)
	@Autowired
	@Qualifier("oracleJdbcTemplate")
	private JdbcTemplate oracleJdbcTemplate;

	// Base B = externa (sem DB-Link)
	@Autowired
	@Qualifier("oracleExternoJdbcTemplate")
	private JdbcTemplate oracleExternoJdbcTemplate;

	/** Nomes das views em cada base (ajuste schema se preciso). */
	private static final String OBJ_A = "produtos_ecommerce@rms"; // via DB-Link
	private static final String OBJ_B = "produtos_ecommerce"; // na base externa

	// ---------- SQLs por COD ----------
	private static final String SQL_DISTINCT_COD_ATIVOS_FMT = "SELECT /*+ parallel(4) */ DISTINCT cod FROM %s "
			+ "WHERE situacao = 1 AND cod IS NOT NULL ORDER BY cod";

	/**
	 * Detalhes com deduplicação por COD: - escolhe 1 linha por COD (rn = 1)
	 * priorizando menor DG e depois EAN/Descrição não nulos - agora incluindo
	 * SECAO, GRUPO, SGRUPO
	 */
	private static final String SQL_DETALHES_BY_CODS_FMT = "SELECT cod, dg, ean, descricao, secao, grupo, sgrupo FROM ( "
			+ "  SELECT cod, dg, ean, descricao, secao, grupo, sgrupo, "
			+ "         ROW_NUMBER() OVER (PARTITION BY cod ORDER BY dg, ean NULLS LAST, descricao NULLS LAST) rn "
			+ "  FROM %s " + "  WHERE situacao = 1 AND cod IN (%s) " + ") t WHERE t.rn = 1 " + "ORDER BY cod";

	// ---------- SQLs por LOJA+COD ----------
	private static final String SQL_DISTINCT_LOJA_COD_ATIVOS_FMT = "SELECT /*+ parallel(4) */ DISTINCT loja, cod FROM %s "
			+ "WHERE situacao = 1 AND cod IS NOT NULL ORDER BY loja, cod";

	/**
	 * Detalhes com deduplicação por (LOJA, COD): 1 linha por par, incluindo
	 * SECAO/GRUPO/SGRUPO.
	 */
	private static final String SQL_DETALHES_BY_LOJA_CODS_FMT = "SELECT loja, cod, dg, ean, descricao, secao, grupo, sgrupo FROM ( "
			+ "  SELECT loja, cod, dg, ean, descricao, secao, grupo, sgrupo, "
			+ "         ROW_NUMBER() OVER (PARTITION BY loja, cod ORDER BY dg, ean NULLS LAST, descricao NULLS LAST) rn "
			+ "  FROM %s " + "  WHERE situacao = 1 AND (%s) " + // (%s) será (loja=? AND cod=?) OR (loja=? AND cod=?)
																// ...
			") t WHERE t.rn = 1 " + "ORDER BY loja, cod";

	private static final int IN_CHUNK = 900; // segurança para limite do Oracle

	// ---------- helpers ----------
	private Integer getInt(java.sql.ResultSet rs, String col) throws java.sql.SQLException {
		BigDecimal n = rs.getBigDecimal(col);
		return n != null ? n.intValue() : null;
	}

	// ---------- helpers por COD ----------
	private List<Long> queryDistinctCods(JdbcTemplate jdbc, String objName) {
		final String sql = String.format(SQL_DISTINCT_COD_ATIVOS_FMT, objName);
		return jdbc.query(sql, (rs, i) -> {
			BigDecimal n = rs.getBigDecimal(1);
			return n != null ? n.longValue() : null;
		});
	}

	private List<Long> listarCodsBaseA() {
		log.debug("Carregando DISTINCT cod (situacao=1) da Base A: {}", OBJ_A);
		return queryDistinctCods(oracleJdbcTemplate, OBJ_A);
	}

	private List<Long> listarCodsBaseB() {
		log.debug("Carregando DISTINCT cod (situacao=1) da Base B: {}", OBJ_B);
		return queryDistinctCods(oracleExternoJdbcTemplate, OBJ_B);
	}

	private List<ProdutoEcommerceExternoDTO> buscarDetalhesPorCods(JdbcTemplate jdbc, String objName, List<Long> cods) {
		List<ProdutoEcommerceExternoDTO> out = new ArrayList<>();
		if (cods == null || cods.isEmpty())
			return out;

		int i = 0;
		while (i < cods.size()) {
			int end = Math.min(i + IN_CHUNK, cods.size());
			List<Long> slice = cods.subList(i, end);

			String placeholders = String.join(",", java.util.Collections.nCopies(slice.size(), "?"));
			String sql = String.format(SQL_DETALHES_BY_CODS_FMT, objName, placeholders);

			out.addAll(jdbc.query(sql, slice.toArray(), (rs, rowNum) -> {
				ProdutoEcommerceExternoDTO p = new ProdutoEcommerceExternoDTO();
				BigDecimal cod = rs.getBigDecimal("COD");
				BigDecimal dg = rs.getBigDecimal("DG");
				BigDecimal ean = rs.getBigDecimal("EAN");

				p.setCod(cod != null ? cod.intValue() : null);
				p.setDg(dg != null ? dg.intValue() : null);
				p.setEan(ean != null ? ean.toPlainString() : null);
				p.setDescricao(rs.getString("DESCRICAO"));

				// NOVO: popular classificação
				p.setSecao(getInt(rs, "SECAO"));
				p.setGrupo(getInt(rs, "GRUPO"));
				p.setSgrupo(getInt(rs, "SGRUPO"));
				return p;
			}));
			i = end;
		}
		return out;
	}

	// ---------- helpers por LOJA+COD ----------
	private static final class LojaCod {
		final Integer loja;
		final Long cod;

		LojaCod(Integer loja, Long cod) {
			this.loja = loja;
			this.cod = cod;
		}

		@Override
		public int hashCode() {
			return (loja == null ? 0 : loja.hashCode()) * 31 + (cod == null ? 0 : cod.hashCode());
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof LojaCod))
				return false;
			LojaCod other = (LojaCod) o;
			return java.util.Objects.equals(this.loja, other.loja) && java.util.Objects.equals(this.cod, other.cod);
		}

		@Override
		public String toString() {
			return "LojaCod{loja=" + loja + ", cod=" + cod + "}";
		}
	}

	private List<LojaCod> queryDistinctLojaCods(JdbcTemplate jdbc, String objName) {
		final String sql = String.format(SQL_DISTINCT_LOJA_COD_ATIVOS_FMT, objName);
		return jdbc.query(sql, (rs, i) -> {
			Integer loja = rs.getBigDecimal(1) != null ? rs.getBigDecimal(1).intValue() : null;
			Long cod = rs.getBigDecimal(2) != null ? rs.getBigDecimal(2).longValue() : null;
			return new LojaCod(loja, cod);
		});
	}

	private List<LojaCod> listarLojaCodBaseA() {
		log.debug("Carregando DISTINCT (loja,cod) da Base A: {}", OBJ_A);
		return queryDistinctLojaCods(oracleJdbcTemplate, OBJ_A);
	}

	private List<LojaCod> listarLojaCodBaseB() {
		log.debug("Carregando DISTINCT (loja,cod) da Base B: {}", OBJ_B);
		return queryDistinctLojaCods(oracleExternoJdbcTemplate, OBJ_B);
	}

	private List<ProdutoEcommerceExternoDTO> buscarDetalhesPorLojaCods(JdbcTemplate jdbc, String objName,
			List<LojaCod> pares) {
		List<ProdutoEcommerceExternoDTO> out = new ArrayList<>();
		if (pares == null || pares.isEmpty())
			return out;

		// cada par gera 2 binds (loja, cod); limitar para não estourar ~1000 binds
		final int CHUNK_PAIRS = 400;
		int i = 0;
		while (i < pares.size()) {
			int end = Math.min(i + CHUNK_PAIRS, pares.size());
			List<LojaCod> slice = pares.subList(i, end);

			StringBuilder where = new StringBuilder();
			List<Object> params = new ArrayList<>(slice.size() * 2);
			for (LojaCod lc : slice) {
				if (where.length() > 0)
					where.append(" OR ");
				where.append("(loja = ? AND cod = ?)");
				params.add(lc.loja);
				params.add(lc.cod);
			}

			String sql = String.format(SQL_DETALHES_BY_LOJA_CODS_FMT, objName, where.toString());

			out.addAll(jdbc.query(sql, params.toArray(), (rs, rowNum) -> {
				ProdutoEcommerceExternoDTO p = new ProdutoEcommerceExternoDTO();
				p.setLoja(getInt(rs, "LOJA"));
				p.setCod(getInt(rs, "COD"));
				p.setDg(getInt(rs, "DG"));
				BigDecimal ean = rs.getBigDecimal("EAN");
				p.setEan(ean != null ? ean.toPlainString() : null);
				p.setDescricao(rs.getString("DESCRICAO"));

				// NOVO: popular classificação
				p.setSecao(getInt(rs, "SECAO"));
				p.setGrupo(getInt(rs, "GRUPO"));
				p.setSgrupo(getInt(rs, "SGRUPO"));
				return p;
			}));

			i = end;
		}
		return out;
	}

	// ---------- B \ A (EXTERNO menos @rms) ----------
	public int countDiffExternoMenosRms() {
		Set<Long> setA = new HashSet<>(listarCodsBaseA());
		int count = 0;
		for (Long codB : listarCodsBaseB()) {
			if (codB != null && !setA.contains(codB))
				count++;
		}
		log.info("Diferenças B\\A (EXTERNO - @rms): {}", count);
		return count;
	}

	public List<Long> diffExternoMenosRmsSample(int limite) {
		Set<Long> setA = new HashSet<>(listarCodsBaseA());
		List<Long> sample = new ArrayList<>(Math.max(16, limite));
		for (Long codB : listarCodsBaseB()) {
			if (codB != null && !setA.contains(codB)) {
				sample.add(codB);
				if (sample.size() >= limite)
					break;
			}
		}
		log.info("Sample B\\A gerado ({} solicitados): {}", limite, sample.size());
		return sample;
	}

	/**
	 * Detalhes para Excel: produtos na Base B e NÃO na Base A (1 linha por COD).
	 */
	public List<ProdutoEcommerceExternoDTO> detalhesExternoMenosRms() {
		Set<Long> setA = new HashSet<>(listarCodsBaseA());
		List<Long> diff = new ArrayList<>();
		for (Long codB : listarCodsBaseB()) {
			if (codB != null && !setA.contains(codB))
				diff.add(codB);
		}
		log.info("Carregando detalhes de {} códigos (B\\A) a partir da Base B [{}]...", diff.size(), OBJ_B);
		return buscarDetalhesPorCods(oracleExternoJdbcTemplate, OBJ_B, diff);
	}

	// ---------- A \ B (@rms menos EXTERNO) ----------
	public int countDiffRmsMenosExterno() {
		Set<Long> setB = new HashSet<>(listarCodsBaseB());
		int count = 0;
		for (Long codA : listarCodsBaseA()) {
			if (codA != null && !setB.contains(codA))
				count++;
		}
		log.info("Diferenças A\\B (@rms - EXTERNO): {}", count);
		return count;
	}

	public List<Long> diffRmsMenosExternoSample(int limite) {
		Set<Long> setB = new HashSet<>(listarCodsBaseB());
		List<Long> sample = new ArrayList<>(Math.max(16, limite));
		for (Long codA : listarCodsBaseA()) {
			if (codA != null && !setB.contains(codA)) {
				sample.add(codA);
				if (sample.size() >= limite)
					break;
			}
		}
		log.info("Sample A\\B gerado ({} solicitados): {}", limite, sample.size());
		return sample;
	}

	/**
	 * Detalhes para Excel: produtos na Base A e NÃO na Base B (1 linha por COD).
	 */
	public List<ProdutoEcommerceExternoDTO> detalhesRmsMenosExterno() {
		Set<Long> setB = new HashSet<>(listarCodsBaseB());
		List<Long> diff = new ArrayList<>();
		for (Long codA : listarCodsBaseA()) {
			if (codA != null && !setB.contains(codA))
				diff.add(codA);
		}
		log.info("Carregando detalhes de {} códigos (A\\B) a partir da Base A [{}]...", diff.size(), OBJ_A);
		return buscarDetalhesPorCods(oracleJdbcTemplate, OBJ_A, diff);
	}

	// ---------- Relatórios por LOJA+COD ----------
	/** (B \ A) Pares (loja, cod) presentes na Base B e ausentes na Base A. */
	public List<ProdutoEcommerceExternoDTO> detalhesExternoMenosRmsPorLoja() {
		Set<LojaCod> setA = new HashSet<>(listarLojaCodBaseA());
		List<LojaCod> diff = new ArrayList<>();
		for (LojaCod lcB : listarLojaCodBaseB()) {
			if (lcB.loja != null && lcB.cod != null && !setA.contains(lcB))
				diff.add(lcB);
		}
		log.info("Diferenças por LOJA+COD (B\\A): {} pares", diff.size());
		return buscarDetalhesPorLojaCods(oracleExternoJdbcTemplate, OBJ_B, diff);
	}

	/** (A \ B) Pares (loja, cod) presentes na Base A e ausentes na Base B. */
	public List<ProdutoEcommerceExternoDTO> detalhesRmsMenosExternoPorLoja() {
		Set<LojaCod> setB = new HashSet<>(listarLojaCodBaseB());
		List<LojaCod> diff = new ArrayList<>();
		for (LojaCod lcA : listarLojaCodBaseA()) {
			if (lcA.loja != null && lcA.cod != null && !setB.contains(lcA))
				diff.add(lcA);
		}
		log.info("Diferenças por LOJA+COD (A\\B): {} pares", diff.size());
		return buscarDetalhesPorLojaCods(oracleJdbcTemplate, OBJ_A, diff);
	}
}
