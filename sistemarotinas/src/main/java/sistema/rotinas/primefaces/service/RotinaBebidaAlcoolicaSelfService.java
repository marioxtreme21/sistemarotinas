package sistema.rotinas.primefaces.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sistema.rotinas.primefaces.dto.ProdutoBebidaAlcoolicaDTO;
import sistema.rotinas.primefaces.service.interfaces.IRotinaBebidaAlcoolicaSelfService;

@Service
public class RotinaBebidaAlcoolicaSelfService implements IRotinaBebidaAlcoolicaSelfService {

    private static final Logger log = LoggerFactory.getLogger(RotinaBebidaAlcoolicaSelfService.class);

    private final JdbcTemplate mysqlExternalJdbcTemplate;

    public RotinaBebidaAlcoolicaSelfService(
            @Qualifier("mysqlExternalJdbcTemplate") JdbcTemplate mysqlExternalJdbcTemplate) {
        this.mysqlExternalJdbcTemplate = mysqlExternalJdbcTemplate;
    }

    /**
     * Método interno que executa apenas os dois UPDATEs.
     * Fica reaproveitado pelo método da tela e pelo scheduler.
     */
    protected void executarUpdatesBebidaAlcoolica() {
        // 1) Marca bebidas alcoólicas = 1 para seção/grupos desejados
        String sqlUpdateOn = """
                UPDATE produto
                   SET bebida_alcoolica = 1
                 WHERE codigo_secao IN (6808)
                   AND codigo_grupo  IN (7067, 7052)
                """;

        // 2) Garante 0 para quem está fora desses critérios
        String sqlUpdateOff = """
                UPDATE produto
                   SET bebida_alcoolica = 0
                 WHERE (codigo_secao NOT IN (6808)
                        OR codigo_grupo NOT IN (7067, 7052))
                """;

        int afetadosOn = mysqlExternalJdbcTemplate.update(sqlUpdateOn);
        int afetadosOff = mysqlExternalJdbcTemplate.update(sqlUpdateOff);

        log.info("[RotinaBebidaAlcoolicaSelf] UPDATE ON afetou {} linhas.", afetadosOn);
        log.info("[RotinaBebidaAlcoolicaSelf] UPDATE OFF afetou {} linhas.", afetadosOff);
    }

    @Override
    @Transactional(transactionManager = "mysqlExternalTransactionManager")
    public List<ProdutoBebidaAlcoolicaDTO> executarRotina() {

        log.info("[RotinaBebidaAlcoolicaSelf] Iniciando rotina de bebidas alcoólicas (SELF) - TELA.");

        // Executa apenas os UPDATEs
        executarUpdatesBebidaAlcoolica();

        // 3) Seleciona os produtos marcados como bebida_alcoolica = 1
        String sqlSelect = """
                SELECT codigo_produto,
                       codigo_secao,
                       codigo_grupo,
                       codigo_sub_grupo,
                       descricao,
                       bebida_alcoolica
                  FROM produto
                 WHERE bebida_alcoolica = 1
                 ORDER BY descricao
                """;

        List<ProdutoBebidaAlcoolicaDTO> resultado = mysqlExternalJdbcTemplate.query(sqlSelect, (rs, rowNum) -> {
            ProdutoBebidaAlcoolicaDTO dto = new ProdutoBebidaAlcoolicaDTO();
            dto.setCodigoProduto(rs.getLong("codigo_produto"));
            dto.setCodigoSecao(rs.getInt("codigo_secao"));
            dto.setCodigoGrupo(rs.getInt("codigo_grupo"));
            dto.setCodigoSubGrupo(rs.getInt("codigo_sub_grupo"));
            dto.setDescricao(rs.getString("descricao"));
            dto.setBebidaAlcoolica(rs.getInt("bebida_alcoolica"));
            return dto;
        });

        log.info("[RotinaBebidaAlcoolicaSelf] Rotina concluída (TELA). Total produtos bebida_alcoolica=1: {}",
                resultado.size());

        return resultado;
    }

    @Override
    @Transactional(transactionManager = "mysqlExternalTransactionManager")
    public void executarRotinaSemSelect() {
        log.info("[RotinaBebidaAlcoolicaSelf] Iniciando rotina de bebidas alcoólicas (SELF) - SCHEDULER.");
        executarUpdatesBebidaAlcoolica();
        log.info("[RotinaBebidaAlcoolicaSelf] Rotina concluída (SCHEDULER) - apenas UPDATEs.");
    }
}
