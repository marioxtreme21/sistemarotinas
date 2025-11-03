package sistema.rotinas.primefaces.repository;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Usa o bean @Qualifier("mysqlExternalJdbcTemplate") (10.1.1.50)
 * definido em MySQLEconectConsincoDataSourceConfig.
 */
@Repository
public class CtrIntVdaRepositoryImpl implements CtrIntVdaRepository {

    private final JdbcTemplate mysql;

    public CtrIntVdaRepositoryImpl(@Qualifier("mysqlExternalJdbcTemplate") JdbcTemplate mysql) {
        this.mysql = mysql;
    }

    @Override
    public List<LinkedHashMap<String, Object>> listarTipInt16() {
        // MySQL 5.6: sem funções JSON; filtra apenas o básico + LIKE para reduzir tráfego
        final String sql = ""
            + "SELECT * "
            + "FROM ctr_int_vda "
            + "WHERE tip_int = 16 "
            + "  AND jso_env IS NOT NULL "
            + "  AND TRIM(jso_env) <> '' "
            + "  AND jso_env LIKE '%\"transactionId\"%'";
            // ^^^ se quiser trazer tudo e filtrar 100% no service, remova esta última linha

        return mysql.query(sql, (ResultSet rs) -> {
            List<LinkedHashMap<String, Object>> lista = new ArrayList<>();
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();

            while (rs.next()) {
                LinkedHashMap<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= cols; i++) {
                    String colName = md.getColumnLabel(i);
                    if (colName == null || colName.isBlank()) {
                        colName = md.getColumnName(i);
                    }
                    // Normaliza para UPPER para consistência visual
                    row.put(colName != null ? colName.toUpperCase() : ("COL_" + i), rs.getObject(i));
                }
                lista.add(row);
            }
            return lista;
        });
    }
}
