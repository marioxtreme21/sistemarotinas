package sistema.rotinas.primefaces.consinco;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import sistema.rotinas.primefaces.dto.ProdutoPrecoDTO;

@Repository
public class PrecoViewEcommerceConsincoReader implements ViewEcommercePrecoReader {

    private final JdbcTemplate oracleJdbc;

    public PrecoViewEcommerceConsincoReader(@Qualifier("oracleExternoJdbcTemplate") JdbcTemplate oracleJdbc) {
        this.oracleJdbc = oracleJdbc;
    }

    private static final String SQL_LISTAR =
        "SELECT loja, cod, descricao, estq AS qtd, preco_de, preco_por, situacao " +
        "  FROM produtos_ecommerce " +
        " WHERE situacao = 1 AND loja = ? " +
        " ORDER BY loja, cod";

    private static final String SQL_BUSCAR =
        "SELECT loja, cod, descricao, estq AS qtd, preco_de, preco_por, situacao " +
        "  FROM produtos_ecommerce " +
        " WHERE situacao = 1 AND loja = ? AND cod = ?";

    @Override
    public List<ProdutoPrecoDTO> listarProdutosComPrecosAtivos(Long lojaId, String codLojaEconect, String codLojaRmsDg) {
    	System.out.println("Acessou metodo listarProdutosComPrecosAtivos");
        int loja = parseIntSafe(codLojaEconect);
        return oracleJdbc.query(SQL_LISTAR, rowMapper(), loja);
    }

    @Override
    public ProdutoPrecoDTO buscarPrecoAtual(Long lojaId, String codLojaEconect, String codLojaRmsDg, Integer sku) {
        int loja = parseIntSafe(codLojaEconect);
        List<ProdutoPrecoDTO> list = oracleJdbc.query(SQL_BUSCAR, rowMapper(), loja, sku);
        return list.isEmpty() ? null : list.get(0);
    }

    /* helpers */
    private RowMapper<ProdutoPrecoDTO> rowMapper() {
        return (rs, i) -> {
            ProdutoPrecoDTO dto = new ProdutoPrecoDTO();
            dto.setCodigo(rs.getInt("cod"));
            dto.setDescricao(rs.getString("descricao"));
            dto.setPrecoDe(rs.getBigDecimal("preco_de"));
            dto.setPrecoPor(rs.getBigDecimal("preco_por"));
            dto.setAtivo(rs.getInt("situacao") == 1);
            return dto;
        };
    }

    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }
}
