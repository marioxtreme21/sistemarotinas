package sistema.rotinas.primefaces.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import sistema.rotinas.primefaces.dto.ProdutoEcommerceExternoDTO;
import sistema.rotinas.primefaces.service.interfaces.IProdutoEcommerceExternoService;

@Service
public class ProdutoEcommerceExternoService implements IProdutoEcommerceExternoService {

    @Autowired
    @Qualifier("oracleExternoJdbcTemplate")
    private JdbcTemplate jdbc;

    @Override
    public List<ProdutoEcommerceExternoDTO> getPrimeiros(int limit) {
        String sql = "SELECT * FROM produtos_ecommerce FETCH FIRST ? ROWS ONLY";
        List<ProdutoEcommerceExternoDTO> lista = jdbc.query(sql, new Object[]{limit}, (rs, rowNum) -> {
            ProdutoEcommerceExternoDTO p = new ProdutoEcommerceExternoDTO();

            p.setLoja(getInt(rs, "LOJA"));
            p.setCod(getInt(rs, "COD"));
            p.setDg(getInt(rs, "DG"));

            BigDecimal eanNum = rs.getBigDecimal("EAN");
            p.setEan(eanNum != null ? eanNum.toPlainString() : null);

            p.setDescricao(rs.getString("DESCRICAO"));
            p.setSecao(getInt(rs, "SECAO"));
            p.setGrupo(getInt(rs, "GRUPO"));
            p.setSgrupo(getInt(rs, "SGRUPO"));
            p.setEstq(rs.getBigDecimal("ESTQ"));
            p.setMarca(rs.getString("MARCA"));

            p.setPrecoPor(rs.getBigDecimal("PRECO_POR"));
            p.setPrecoDe(rs.getBigDecimal("PRECO_DE"));
            p.setSituacao(rs.getString("SITUACAO"));

            return p;
        });

        // Imprime no console os registros lidos
        lista.forEach(p -> System.out.println(p));

        return lista;
    }

    private Integer getInt(java.sql.ResultSet rs, String col) throws java.sql.SQLException {
        BigDecimal n = rs.getBigDecimal(col);
        return n != null ? n.intValue() : null;
    }

    // Métodos ainda não implementados (iremos fazer depois)
    @Override
    public List<ProdutoEcommerceExternoDTO> findAllProdutosEcommerce(int first, int pageSize, String sortField, boolean ascendente) {
        throw new UnsupportedOperationException("Método não implementado ainda.");
    }

    @Override
    public int countProdutosEcommerce() {
        throw new UnsupportedOperationException("Método não implementado ainda.");
    }

    @Override
    public List<ProdutoEcommerceExternoDTO> findProdutosEcommerceByCriteria(String campo, String condicao, String valor,
            int first, int pageSize, String sortField, boolean ascendente) {
        throw new UnsupportedOperationException("Método não implementado ainda.");
    }

    @Override
    public int countProdutosEcommerceByCriteria(String campo, String condicao, String valor) {
        throw new UnsupportedOperationException("Método não implementado ainda.");
    }

    @Override
    public ProdutoEcommerceExternoDTO findByChave(Integer loja, Integer cod, Integer dg) {
        throw new UnsupportedOperationException("Método não implementado ainda.");
    }

    @Override
    public List<ProdutoEcommerceExternoDTO> findByEan(String ean) {
        throw new UnsupportedOperationException("Método não implementado ainda.");
    }
}
