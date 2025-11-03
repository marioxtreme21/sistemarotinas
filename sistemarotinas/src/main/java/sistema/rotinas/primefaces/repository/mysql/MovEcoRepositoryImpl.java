package sistema.rotinas.primefaces.repository.mysql;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import sistema.rotinas.primefaces.dto.PedidoFreteDTO;

@Repository
public class MovEcoRepositoryImpl implements MovEcoRepository {

    private final JdbcTemplate mysql;

    public MovEcoRepositoryImpl(@Qualifier("mysqlExternalJdbcTemplate") JdbcTemplate mysql) {
        this.mysql = mysql;
    }

    @Override
    public List<PedidoFreteDTO> listarPedidosComFrete(LocalDate dataInicial, BigDecimal freteMinimo) {
    	System.out.println("Acessou metodo listarPedidosComFrete ");
        String sql = """
            select 
              me.dat        as data_venda,
              me.cod_loj    as loja,
              me.num_cup    as numero_cupom,
              me.cod_pdv	as numero_pdv,
              me.num_ped    as numero_pedido,
              me.nom_cli    as nome_cliente,
              me.vlr_vda    as valor_pedido,
              me.vlr_fte    as valor_frete,
              nfe.num_lot   as numero_nfce,
              nfe.qrcode    as qrcodenfce,
              nfe.chv_acs   as chave_acesso
            from mov_eco me
            inner join mov_nfc nfe 
                on me.cod_loj = nfe.num_loj 
               and me.cod_pdv = nfe.sre_nfc 
               and me.num_cup = nfe.num_nfc
            where me.dat = ? 
              and me.vlr_fte > ? 
            order by me.cod_loj, me.cod_pdv
        """;

        return mysql.query(sql, new Object[]{dataInicial, freteMinimo}, new RowMapper<PedidoFreteDTO>() {
            @Override
            public PedidoFreteDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
                PedidoFreteDTO d = new PedidoFreteDTO();
                java.sql.Date dv = rs.getDate("data_venda");
                d.setDataVenda(dv != null ? dv.toLocalDate() : null);
                d.setCodLoja(rs.getInt("loja"));
                d.setNumeroPdv(rs.getInt("numero_pdv"));
                d.setNumCupom(rs.getInt("numero_cupom"));
                d.setNumPedido(rs.getInt("numero_pedido"));
                d.setNomeCliente(rs.getString("nome_cliente"));
                d.setValorPedido(rs.getBigDecimal("valor_pedido"));
                d.setValorFrete(rs.getBigDecimal("valor_frete"));
                d.setNumeroNfce(rs.getLong("numero_nfce"));
                d.setQrcode(rs.getString("qrcodenfce"));
                d.setChaveAcesso(rs.getString("chave_acesso"));
                return d;
            }
        });
    }
}
