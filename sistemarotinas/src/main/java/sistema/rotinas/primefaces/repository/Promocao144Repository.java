package sistema.rotinas.primefaces.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import sistema.rotinas.primefaces.dto.ProdutoNivelPrecoDTO;

@Repository
public class Promocao144Repository {

    private final JdbcTemplate jdbc144; // 10.1.1.144

    public Promocao144Repository(@Qualifier("mysqlExternalJdbcTemplate144") JdbcTemplate jdbc144) {
        this.jdbc144 = jdbc144;
    }

    private static final RowMapper<ProdutoNivelPrecoDTO> MAPPER = new RowMapper<ProdutoNivelPrecoDTO>() {
        @Override
        public ProdutoNivelPrecoDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            ProdutoNivelPrecoDTO dto = new ProdutoNivelPrecoDTO();
            dto.setCodigoNivel(rs.getInt("codigo_nivel"));      // aqui é a LOJA do 144
            dto.setCodigoProduto(rs.getInt("codigo_produto"));
            dto.setCodigoEan(rs.getString("codigo_ean"));
            dto.setDescricao(rs.getString("descricao"));
            dto.setPesavel(rs.getInt("pesavel"));
            dto.setEmbalagem(rs.getString("embalagem"));
            dto.setPreco(rs.getBigDecimal("preco"));
            Timestamp di = rs.getTimestamp("data_inicial");
            Timestamp df = rs.getTimestamp("data_final");
            dto.setDataInicial(di == null ? null : di.toLocalDateTime());
            dto.setDataFinal(df == null ? null : df.toLocalDateTime());
            
            return dto;
        }
    };

 // Promoções vigentes na data de referência (prints do usuário: filtros de embalagem C, CX, EV)
    private static final String QUERY_PROMOCOES_VIGENTES =
        "select p.codigo_loja    as codigo_nivel, " +
        "       e.codigo_produto as codigo_produto, " +
        "       p.codigo_produto as codigo_ean, " +
        "       pro.descricao     as descricao, " +
        "       pro.pesavel       as pesavel, " +
        "       e.tipo_embalagem  as embalagem, " +
        "       p.preco           as preco " +
        "       p.data_inicial     as data_inicial, " +
        "       p.data_final       as data_final " +
        "from promocao p " +
        "join ean e on (e.codigo_ean = p.codigo_produto) " +
        "join produto pro on (pro.codigo_produto = e.codigo_produto) " +
        "where p.data_inicial <= ? " +
        "  and (p.data_final is null or p.data_final >= ?) " +          // vigente na data de referência
        "  and p.data_final is not null and p.data_final <= ? " +       // NOVO: data_final <= 01/12/2025 00:00:00
        "  and e.embalagem <> 'C' and e.embalagem <> 'CX' and e.embalagem <> 'EV' " +
        "  and p.codigo_loja = ? " +
        "order by e.codigo_produto";

    public List<ProdutoNivelPrecoDTO> listarPromocoesVigentes(int loja, LocalDateTime referencia) {
        Timestamp ref = Timestamp.valueOf(referencia);
        Timestamp limite = Timestamp.valueOf(LocalDateTime.of(2025, 12, 1, 0, 0, 0));
        return jdbc144.query(QUERY_PROMOCOES_VIGENTES, MAPPER, ref, ref, limite, loja);
    }
}
