package sistema.rotinas.primefaces.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import sistema.rotinas.primefaces.dto.ProdutoNivelPrecoDTO;

@Repository
public class ProdutoNivelPrecoRepository {

    private final JdbcTemplate jdbc144; // 10.1.1.144
    private final JdbcTemplate jdbc50;  // 10.1.1.50

    public ProdutoNivelPrecoRepository(
            @Qualifier("mysqlExternalJdbcTemplate144") JdbcTemplate jdbc144,
            @Qualifier("mysqlExternalJdbcTemplate") JdbcTemplate jdbc50) {
        this.jdbc144 = jdbc144;
        this.jdbc50 = jdbc50;
    }

    private static final String BASE_QUERY =
            "select n.codigo_nivel   as codigo_nivel, " +
            "       n.codigo_produto as codigo_ean, " +
            "       p.codigo_produto as codigo_produto, " +
            "       p.descricao      as descricao, " +
            "       p.pesavel        as pesavel, " +
            "       p.embalagem      as embalagem, " +
            "       n.preco          as preco " +
            "from nivel_preco_produto n " +
            "inner join ean e on (n.codigo_produto = e.codigo_ean) " +
            "inner join produto p on (p.codigo_produto = e.codigo_produto) " +
            "where n.codigo_nivel = ? and e.tipo_embalagem<>9 " +
            "order by n.codigo_produto";

    private static final RowMapper<ProdutoNivelPrecoDTO> MAPPER = new RowMapper<ProdutoNivelPrecoDTO>() {
        @Override
        public ProdutoNivelPrecoDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            ProdutoNivelPrecoDTO dto = new ProdutoNivelPrecoDTO();
            dto.setCodigoNivel(rs.getInt("codigo_nivel"));
            dto.setCodigoEan(rs.getString("codigo_ean"));
            dto.setCodigoProduto(rs.getInt("codigo_produto"));
            dto.setDescricao(rs.getString("descricao"));
            dto.setPesavel(rs.getInt("pesavel"));
            dto.setEmbalagem(rs.getString("embalagem"));
            dto.setPreco(rs.getBigDecimal("preco"));
            return dto;
        }
    };

    public List<ProdutoNivelPrecoDTO> buscarNo144(int codigoNivel) {
        return jdbc144.query(BASE_QUERY, MAPPER, codigoNivel);
    }

    public List<ProdutoNivelPrecoDTO> buscarNo50(int codigoNivel) {
        return jdbc50.query(BASE_QUERY, MAPPER, codigoNivel);
    }
}
