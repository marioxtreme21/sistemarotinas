package sistema.rotinas.primefaces.repository.loyalty;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import sistema.rotinas.primefaces.dto.loyalty.LoyaltyCupomOrigemDTO;

@Repository
public class LoyaltyVenda144Repository {

    private final JdbcTemplate jdbcTemplate144;

    public LoyaltyVenda144Repository(@Qualifier("mysqlExternalJdbcTemplate144") JdbcTemplate jdbcTemplate144) {
        this.jdbcTemplate144 = jdbcTemplate144;
    }

    public List<LoyaltyCupomOrigemDTO> buscarVendasPorDataELoja(LocalDate dataVenda, Integer numeroLoja) {
        String sql = """
                select
                    c.data_venda as dt_movimento,
                    c.numero_loja as id_loja,
                    l.nom_fan_loj as nome_loja,
                    c.numero_pdv as id_pdv,
                    nf.num_lot as num_cupom,
                    'Identificado' as categoria,
                    case
                        when cbk.chv_idt is null
                          or cbk.chv_idt = ''
                          or cbk.chv_idt = '0'
                        then null
                        else cbk.chv_idt
                    end as id_cliente,
                    'PDV' as canal_venda,
                    c.total_liquido as vlr_venda,
                    c.codigo_operador as id_operador,
                    coalesce(itens.qtd_produto, 0) as qtd_produto
                from (
                    select
                        data_venda,
                        numero_loja,
                        numero_pdv,
                        numero_cupom,
                        total_liquido,
                        codigo_operador
                    from capa_cupom_venda
                    where data_venda = ?
                      and numero_loja = ?
                      and situacao_capa = 7
                      and tipo_capa = 0
                ) c
                inner join configuracao_loja l
                    on l.codigo_loja = c.numero_loja
                inner join (
                    select
                        num_loj,
                        sre_nfc,
                        num_nfc,
                        date(dat_hor_ems) as data_emissao,
                        max(num_lot) as num_lot
                    from mov_nfc
                    where num_loj = ?
                      and dat_hor_ems >= ?
                      and dat_hor_ems < date_add(?, interval 1 day)
                      and tip_ems in (1,9)
                    group by
                        num_loj,
                        sre_nfc,
                        num_nfc,
                        date(dat_hor_ems)
                ) nf
                    on nf.num_loj = c.numero_loja
                   and nf.sre_nfc = c.numero_pdv
                   and nf.num_nfc = c.numero_cupom
                   and nf.data_emissao = c.data_venda
                left join (
                    select
                        dat_mov,
                        cod_loj,
                        cod_pdv,
                        num_cup,
                        max(
                            case
                                when chv_idt is not null
                                 and chv_idt <> ''
                                 and chv_idt <> '0'
                                then chv_idt
                                else null
                            end
                        ) as chv_idt
                    from mov_idt_cbk
                    where dat_mov = ?
                      and cod_loj = ?
                    group by
                        dat_mov,
                        cod_loj,
                        cod_pdv,
                        num_cup
                ) cbk
                    on cbk.dat_mov = c.data_venda
                   and cbk.cod_loj = c.numero_loja
                   and cbk.cod_pdv = c.numero_pdv
                   and cbk.num_cup = c.numero_cupom
                left join (
                    select
                        data_venda,
                        numero_loja,
                        numero_pdv,
                        numero_cupom,
                        round(sum(quantidade), 0) as qtd_produto
                    from detalhe_cupom_venda
                    where data_venda = ?
                      and numero_loja = ?
                      and situacao_detalhe = 1
                    group by
                        data_venda,
                        numero_loja,
                        numero_pdv,
                        numero_cupom
                ) itens
                    on itens.data_venda = c.data_venda
                   and itens.numero_loja = c.numero_loja
                   and itens.numero_pdv = c.numero_pdv
                   and itens.numero_cupom = c.numero_cupom
                """;

        Date data = Date.valueOf(dataVenda);

        return jdbcTemplate144.query(
                sql,
                this::mapRow,
                data, numeroLoja,
                numeroLoja, data, data,
                data, numeroLoja,
                data, numeroLoja
        );
    }

    private LoyaltyCupomOrigemDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        Date dt = rs.getDate("dt_movimento");
        return new LoyaltyCupomOrigemDTO(
                dt != null ? dt.toLocalDate() : null,
                rs.getInt("id_loja"),
                rs.getString("nome_loja"),
                rs.getInt("id_pdv"),
                rs.getLong("num_cupom"),
                rs.getString("categoria"),
                rs.getString("id_cliente"),
                rs.getString("canal_venda"),
                rs.getBigDecimal("vlr_venda"),
                rs.getString("id_operador"),
                rs.getInt("qtd_produto")
        );
    }
}