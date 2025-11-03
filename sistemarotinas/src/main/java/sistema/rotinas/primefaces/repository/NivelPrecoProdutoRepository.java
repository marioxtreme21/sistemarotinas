package sistema.rotinas.primefaces.repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import sistema.rotinas.primefaces.dto.NivelPrecoProdutoDTO;

@Repository
public class NivelPrecoProdutoRepository {

	private static final Logger log = LoggerFactory.getLogger(NivelPrecoProdutoRepository.class);

	@Autowired
	@Qualifier("mysqlExternalJdbcTemplate")
	private JdbcTemplate jdbc;

	/** SELECT base (todos os produtos do nível) com aliases novos. */
	private static final String BASE_SQL = "select n.codigo_nivel   as codigo_nivel, "
			+ "       e.codigo_produto as codigo_produto, " + // código interno (produto)
			"       n.codigo_produto as codigo_ean, " + // EAN/PLU
			"       p.descricao      as descricao, " + "       p.pesavel        as pesavel, "
			+ "       p.embalagem      as embalagem, " + "       n.preco          as preco "
			+ "from nivel_preco_produto n " + "inner join ean e on (n.codigo_produto = e.codigo_ean) "
			+ "inner join produto p on (p.codigo_produto = e.codigo_produto) " + "where n.codigo_nivel = ? "
			+ "  and e.embalagem <> 'CJ' and e.embalagem <> 'EV' and e.codigo_ean > 0 ";

	/** RowMapper compatível com o DTO. */
	private static final RowMapper<NivelPrecoProdutoDTO> ROW_MAPPER = new RowMapper<NivelPrecoProdutoDTO>() {
		@Override
		public NivelPrecoProdutoDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
			Integer codigoNivel = rs.getInt("codigo_nivel");
			String codigoProd = rs.getString("codigo_produto"); // e.codigo_produto
			String codigoEan = rs.getString("codigo_ean"); // n.codigo_produto
			String descricao = rs.getString("descricao");
			Boolean pesavel = toBoolean(rs.getObject("pesavel"));
			String embalagem = rs.getString("embalagem");
			BigDecimal preco = rs.getBigDecimal("preco");

			return new NivelPrecoProdutoDTO(codigoNivel, codigoProd, codigoEan, descricao, pesavel, embalagem, preco);
		}

		private Boolean toBoolean(Object raw) {
			if (raw == null)
				return null;
			if (raw instanceof Boolean)
				return (Boolean) raw;
			if (raw instanceof Number)
				return ((Number) raw).intValue() != 0;
			String s = String.valueOf(raw).trim();
			if (s.equalsIgnoreCase("S") || s.equalsIgnoreCase("Y"))
				return true;
			if (s.equalsIgnoreCase("N"))
				return false;
			if (s.equals("1"))
				return true;
			if (s.equals("0"))
				return false;
			return Boolean.parseBoolean(s);
		}
	};

	/** Lista completa (sem paginação). */
	public List<NivelPrecoProdutoDTO> listarPorNivel(int codigoNivel) {
		String sql = BASE_SQL + " order by n.codigo_produto";

		// LOG: SQL expandido
		if (log.isInfoEnabled()) {
			log.info("SQL para copiar e testar (TODOS):\n{}\n", renderSql(sql, new Object[] { codigoNivel }));
		}

		return jdbc.query(sql, ROW_MAPPER, codigoNivel);
	}

	/** Lista paginada/ordenada. */
	public List<NivelPrecoProdutoDTO> listarPorNivel(int codigoNivel, int first, int pageSize, String sortField,
			boolean ascendente) {

		String order = (sortField == null || sortField.isBlank()) ? "n.codigo_produto" : sortField;

		String sql = BASE_SQL + " order by " + order + (ascendente ? " asc " : " desc ") + " limit ? offset ?";

		// LOG: SQL expandido
		if (log.isInfoEnabled()) {
			log.info("SQL para copiar e testar (paginado):\n{}\n",
					renderSql(sql, new Object[] { codigoNivel, pageSize, first }));
		}

		return jdbc.query(sql, ps -> {
			ps.setInt(1, codigoNivel);
			ps.setInt(2, pageSize);
			ps.setInt(3, first);
		}, ROW_MAPPER);
	}

	/** Total de registros para o nível. */
	public int countPorNivel(int codigoNivel) {
		String sql = "select count(*) from ( " + BASE_SQL + " ) t";

		// LOG: SQL expandido
		if (log.isInfoEnabled()) {
			log.info("SQL para copiar e testar (count):\n{}\n", renderSql(sql, new Object[] { codigoNivel }));
		}

		Integer count = jdbc.queryForObject(sql, Integer.class, codigoNivel);
		return count == null ? 0 : count;
	}

	/** Apenas EAN-13 válidos e distintos, para rotação de barcodes. */
	public List<String> listarCodigosEan13PorNivel(int codigoNivel) {
		String sql = "select distinct n.codigo_produto " + "from nivel_preco_produto n "
				+ "inner join ean e on (n.codigo_produto = e.codigo_ean) "
				+ "inner join produto p on (p.codigo_produto = e.codigo_produto) " + "where n.codigo_nivel = ? "
				+ "  and e.embalagem <> 'CJ' " + "  and n.codigo_produto regexp '^[0-9]{13}$' "
				+ "order by n.codigo_produto";

		// LOG: SQL expandido
		if (log.isInfoEnabled()) {
			log.info("SQL para copiar e testar (EAN13):\n{}\n", renderSql(sql, new Object[] { codigoNivel }));
		}

		return jdbc.query(sql, (rs, i) -> rs.getString(1), codigoNivel);
	}

	/**
	 * SELECT com IN (...) em e.codigo_produto e ORDER BY p.descricao, idêntico ao
	 * que você executou manualmente no banco. Faz o binding dos códigos como
	 * Integer. Loga o SQL expandido para copiar/colar.
	 */
	public List<NivelPrecoProdutoDTO> listarPorNivelComProdutosOrdenadoPorDescricao(int codigoNivel,
			List<Long> codigosProduto) {

		if (codigosProduto == null || codigosProduto.isEmpty()) {
			if (log.isInfoEnabled())
				log.info("listarPorNivelComProdutosOrdenadoPorDescricao: lista vazia");
			return List.of();
		}

		StringJoiner sj = new StringJoiner(",", "(", ")");
		for (int i = 0; i < codigosProduto.size(); i++)
			sj.add("?");

		String sql = "select n.codigo_nivel   as codigo_nivel, " + "       e.codigo_produto as codigo_produto, "
				+ "       n.codigo_produto as codigo_ean, " + "       p.descricao      as descricao, "
				+ "       p.pesavel        as pesavel, " + "       p.embalagem      as embalagem, "
				+ "       n.preco          as preco " + "from nivel_preco_produto n "
				+ "inner join ean e on (n.codigo_produto = e.codigo_ean) "
				+ "inner join produto p on (p.codigo_produto = e.codigo_produto) " + "where n.codigo_nivel = ? "
				+ "  and e.embalagem <> 'CJ' and e.embalagem <> 'EV' " + "  and e.codigo_ean > 0 "
				+ "  and e.codigo_produto in " + sj.toString() + " " + "order by p.descricao";

		// Monta parâmetros: nível + lista (bind como Integer)
		Object[] params = new Object[1 + codigosProduto.size()];
		params[0] = Integer.valueOf(codigoNivel);
		for (int i = 0; i < codigosProduto.size(); i++) {
			Long v = codigosProduto.get(i);
			params[i + 1] = (v == null ? null : Integer.valueOf(v.intValue()));
		}

		if (log.isInfoEnabled()) {
			log.info("listarPorNivelComProdutosOrdenadoPorDescricao: nivel={}, qtdCodigos={}, primeiros={}",
					codigoNivel, codigosProduto.size(), codigosProduto.stream().limit(10).collect(Collectors.toList()));

			// LOG: SQL expandido para copiar/colar
			log.info("SQL para copiar e testar (LISTA):\n{}\n", renderSql(sql, params));
		}

		return jdbc.query(sql, ROW_MAPPER, params);
	}

	// =========================
	// Helpers de LOG do SQL
	// =========================

	private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private static String renderSql(String sql, Object[] params) {
		if (params == null || params.length == 0)
			return sql;
		StringBuilder out = new StringBuilder(sql.length() + params.length * 10);
		int p = 0;
		for (int i = 0; i < sql.length(); i++) {
			char c = sql.charAt(i);
			if (c == '?' && p < params.length) {
				out.append(formatParam(params[p++]));
			} else {
				out.append(c);
			}
		}
		return out.toString();
	}

	private static String formatParam(Object val) {
		if (val == null)
			return "NULL";
		if (val instanceof Number)
			return val.toString();
		if (val instanceof Boolean)
			return ((Boolean) val) ? "1" : "0";
		if (val instanceof Date) {
			String s = ((Date) val).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(DT_FMT);
			return "'" + s + "'";
		}
		// Strings / outros objetos => quoted + escape de aspas simples
		String s = String.valueOf(val);
		s = s.replace("'", "''");
		return "'" + s + "'";
	}
}
