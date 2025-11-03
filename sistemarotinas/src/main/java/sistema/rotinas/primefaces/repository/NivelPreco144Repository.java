package sistema.rotinas.primefaces.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import sistema.rotinas.primefaces.dto.ProdutoNivelPrecoDTO;

@Repository
public class NivelPreco144Repository {

	private final JdbcTemplate jdbc144; // 10.1.1.144

	public NivelPreco144Repository(@Qualifier("mysqlExternalJdbcTemplate144") JdbcTemplate jdbc144) {
		this.jdbc144 = jdbc144;
	}

	private static final RowMapper<ProdutoNivelPrecoDTO> MAPPER = new RowMapper<ProdutoNivelPrecoDTO>() {
		@Override
		public ProdutoNivelPrecoDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
			ProdutoNivelPrecoDTO dto = new ProdutoNivelPrecoDTO();
			// mapeia campos do SELECT
			dto.setCodigoNivel(rs.getInt("loja"));
			dto.setCodigoProduto(rs.getInt("codigo_produto"));
			dto.setCodigoEan(rs.getString("codigo_ean"));
			dto.setDescricao(rs.getString("descricao"));
			dto.setPreco(rs.getBigDecimal("preco"));
			// usamos o campo 'embalagem' do DTO para carregar e.tipo_embalagem como String
			dto.setEmbalagem(rs.getString("tipo_embalagem"));
			return dto;
		}
	};

	private static final String QUERY_NIVEL_SEM_PROMO = "SELECT " + "  nv.codigo_nivel      AS loja, "
			+ "  e.codigo_produto     AS codigo_produto, " + "  nv.codigo_produto    AS codigo_ean, "
			+ "  pro.descricao        AS descricao, " + "  nv.preco             AS preco, "
			+ "  e.tipo_embalagem     AS tipo_embalagem " + "FROM nivel_preco_produto nv "
			+ "JOIN ean     e   ON e.codigo_ean       = nv.codigo_produto "
			+ "JOIN produto pro ON pro.codigo_produto = e.codigo_produto " + "LEFT JOIN promocao p "
			+ "       ON p.codigo_loja    = nv.codigo_nivel " + "      AND p.codigo_produto = nv.codigo_produto "
			+ "      AND p.data_inicial  <= ? " + "      AND (p.data_final IS NULL OR p.data_final >= ?) "
			+ "WHERE nv.codigo_nivel = ? " + "  AND e.embalagem NOT IN ('CJ','CX','EV') "
			+ "  AND e.tipo_embalagem <> 9 " + "  AND p.codigo_produto IS NULL " 
			+ "  AND nv.data_preco >= '2024-01-01 00:00:00' and nv.data_preco >= '2024-01-01 00:00:00'  " 
			+ "ORDER BY nv.codigo_produto";

	/**
	 * Lista o último preço por produto no 144 (nível) que NÃO está em promoção
	 * vigente na data informada (início <= data 23:59:59 e fim >= data 00:00:00).
	 */
	public List<ProdutoNivelPrecoDTO> listarSemPromocaoVigente(int loja, LocalDate dataReferencia) {
		LocalDateTime fimDia = LocalDateTime.of(dataReferencia, LocalTime.of(23, 59, 59));
		LocalDateTime iniDia = LocalDateTime.of(dataReferencia, LocalTime.of(0, 0, 0));
		return jdbc144.query(QUERY_NIVEL_SEM_PROMO, MAPPER, Timestamp.valueOf(fimDia), Timestamp.valueOf(iniDia), loja);
	}
}
