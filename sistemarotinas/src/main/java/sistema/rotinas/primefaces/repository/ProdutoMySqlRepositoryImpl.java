package sistema.rotinas.primefaces.repository;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import sistema.rotinas.primefaces.dto.SkuMigracaoDTO;

@Repository
public class ProdutoMySqlRepositoryImpl implements ProdutoMySqlRepository {

	private final JdbcTemplate mysql144;

	public ProdutoMySqlRepositoryImpl(@Qualifier("mysqlExternalJdbcTemplate144") JdbcTemplate mysql144) {
		this.mysql144 = mysql144;
	}

	@Override
	public List<SkuMigracaoDTO> listarSkuDescricao() {
		final String sql = "SELECT codigo_produto AS SKU, descricao FROM produto ORDER BY codigo_produto";
		return mysql144.query(sql, (rs, i) -> {
			Integer sku = rs.getInt("SKU");
			if (rs.wasNull())
				sku = null;
			String desc = rs.getString("descricao");
			return new SkuMigracaoDTO(sku, null, desc);
		});
	}
}
