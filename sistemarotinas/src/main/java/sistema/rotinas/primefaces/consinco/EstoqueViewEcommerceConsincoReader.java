// src/main/java/sistema/ecommerce/primefaces/consinco/EstoqueViewEcommerceConsincoReader.java
package sistema.rotinas.primefaces.consinco;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import sistema.rotinas.primefaces.dto.ProdutoEstoqueDTO;

@Repository
public class EstoqueViewEcommerceConsincoReader implements ViewEcommerceEstoqueReader {

	private final JdbcTemplate oracleJdbc;

	public EstoqueViewEcommerceConsincoReader(@Qualifier("oracleExternoJdbcTemplate") JdbcTemplate oracleJdbc) {
		this.oracleJdbc = oracleJdbc;
	}

	private static final String SQL_LISTAR = "SELECT loja, cod, descricao, estq AS qtd, situacao "
			+ "  FROM produtos_ecommerce " + " WHERE situacao = 1 AND loja = ? " + " ORDER BY loja, cod";

	private static final String SQL_BUSCAR = "SELECT loja, cod, descricao, estq AS qtd, situacao "
			+ "  FROM produtos_ecommerce " + " WHERE situacao = 1 AND loja = ? AND cod = ?";

	@Override
	public List<ProdutoEstoqueDTO> listarProdutosComEstoque(Long lojaId, String codLojaEconect, String codLojaRmsDg) {
		int loja = parseIntSafe(codLojaEconect);
		return oracleJdbc.query(SQL_LISTAR, rowMapper(), loja);
	}

	@Override
	public ProdutoEstoqueDTO buscarEstoqueAtual(Long lojaId, String codLojaEconect, String codLojaRmsDg,
			Integer codigo) {
		int loja = parseIntSafe(codLojaEconect);
		List<ProdutoEstoqueDTO> list = oracleJdbc.query(SQL_BUSCAR, rowMapper(), loja, codigo);
		return list.isEmpty() ? null : list.get(0);
	}

	/* helpers */
	private RowMapper<ProdutoEstoqueDTO> rowMapper() {
		return (rs, i) -> {
			ProdutoEstoqueDTO dto = new ProdutoEstoqueDTO();
			dto.setCodigo(rs.getInt("cod"));
			dto.setDescricao(rs.getString("descricao"));
			// estq pode vir decimal na view; mantemos BigDecimal no DTO e convertimos para
			// int no service
			BigDecimal qtd = rs.getBigDecimal("qtd");
			dto.setQuantidade(qtd != null ? qtd : BigDecimal.ZERO);
			return dto;
		};
	}

	private int parseIntSafe(String s) {
		try {
			return Integer.parseInt(s);
		} catch (Exception e) {
			return 0;
		}
	}
}
