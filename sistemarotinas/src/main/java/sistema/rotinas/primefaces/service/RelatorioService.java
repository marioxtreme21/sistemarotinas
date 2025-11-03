package sistema.rotinas.primefaces.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RelatorioService {

    @Autowired
    @Qualifier("mysqlJdbcTemplate") // <-- garante que é o MySQL
    private JdbcTemplate mysqlExternalJdbcTemplate; // Conexão com banco externo

    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("R$ #,##0.00");
    private static final DecimalFormat PERCENTAGE_FORMAT = new DecimalFormat("#0.00'%'");

    /**
     * Consulta promoções vigentes no MySQL externo com base no código da loja e busca o preço vigente do produto.
     * @param codLojaEconect Código da loja (Loja.codLojaEconect)
     * @param codLojaRms Código da loja no RMS (Loja.codLojaRms)
     * @return Lista de mapas contendo os resultados do banco.
     */
    public List<Map<String, Object>> buscarPromocoesPorLoja(String codLojaEconect, String codLojaRms) {
        List<Map<String, Object>> resultados = new ArrayList<>();

        // Obtém a data do sistema no horário de execução, fixando o horário para 00:00:00
        String dataAtual = LocalDate.now().plusDays(5).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 00:00:00";

        // Query SQL para buscar promoções no banco externo
        String sqlPromocoes = """
                SELECT prm.cod_loj as loja, prm.cod_mix, prm.des as desc_mix,
                       pr.codigo_produto as c_produto, pr.descricao as descricao_produto,
                       dsc.qtd as quantidade, dsc.dsc as Valor_Desconto, dsc.dsc_vlr as Desconto_Valor,
                       dsc.dsc_per as Desconto_Percentual, dsc.cod_prd as Produto_C_desconto,
                       prm.dat_ini as data_inicial, prm.dat_fim as data_final
                FROM prm_mix_lev_pag prm
                INNER JOIN prd_mix_lev_pag prd ON (prm.cod_loj = prd.cod_loj) AND (prm.cod_mix = prd.cod_mix)
                INNER JOIN produto pr ON (prd.prd = pr.codigo_produto)
                INNER JOIN dsc_mix_lev_pag dsc ON (dsc.cod_loj = prd.cod_loj) AND (dsc.cod_mix = prd.cod_mix)
                WHERE prm.dat_fim >= ? 
                AND prm.dat_ini <= ? 
                AND prm.cod_loj = ?
                GROUP BY prm.cod_loj, prm.cod_mix, prd.prd
                """;

        try {
            List<Map<String, Object>> promocoes = mysqlExternalJdbcTemplate.queryForList(sqlPromocoes, dataAtual, dataAtual, codLojaEconect);

            for (Map<String, Object> promocao : promocoes) {
                String codigoProduto = String.valueOf(promocao.get("c_produto"));
                String codMix = String.valueOf(promocao.get("cod_mix"));

                // Determinar a origem da promoção
                String origemPromocao = codMix.length() >= 8 ? "Scantech" : "Lançamento Manual";
                promocao.put("Origem_Promocao", origemPromocao);

                // Ajuste para preço vigente
                Object precoVigente = buscarPrecoVigente(codigoProduto, codLojaRms, dataAtual);

                // Obtendo os valores corretos dos campos
                double descontoValor = promocao.get("Desconto_Valor") != null ?
                        Double.parseDouble(promocao.get("Desconto_Valor").toString()) : 0.0;

                boolean descontoPercentual = promocao.get("Desconto_Percentual") != null &&
                        promocao.get("Desconto_Percentual").toString().equalsIgnoreCase("true");

                // Ajustando os valores das colunas conforme as regras
                promocao.put("Desconto_Valor", (descontoValor == 1) ? "Ativo" : "Desativado");
                promocao.put("Desconto_Percentual", descontoPercentual ? "Ativo" : "Desativado");

                // Se "Desconto_Valor" estiver como "Ativo", formatamos "Valor_Desconto" como moeda
                if (descontoValor == 1) {
                    promocao.put("Valor_Desconto", CURRENCY_FORMAT.format(promocao.get("Valor_Desconto")));
                }

                // Se "Desconto_Percentual" estiver como "Ativo", formatamos "Valor_Desconto" como percentual
                if (descontoPercentual) {
                    promocao.put("Valor_Desconto", PERCENTAGE_FORMAT.format(promocao.get("Valor_Desconto")));
                }

                // Criando um novo mapa ordenado para garantir a posição correta das colunas
                Map<String, Object> promocaoOrdenada = new LinkedHashMap<>();
                for (Map.Entry<String, Object> entry : promocao.entrySet()) {
                    promocaoOrdenada.put(entry.getKey(), entry.getValue());
                    if (entry.getKey().equals("descricao_produto")) {
                        promocaoOrdenada.put("preco_vigente", precoVigente); // Inserindo após "descricao_produto"
                    }
                }

                resultados.add(promocaoOrdenada);
            }
        } catch (Exception e) {
            System.err.println("❌ Erro ao consultar promoções para a loja " + codLojaEconect + ": " + e.getMessage());
        }

        return resultados;
    }

    /**
     * Busca o preço vigente do produto considerando promoções e preços normais.
     */
    private Object buscarPrecoVigente(String codigoProduto, String codLojaRms, String dataAtual) {
        Object precoVigente = null;

        // Primeiro SELECT para verificar se há preço de promoção
        String sqlPrecoPromocional = """
                SELECT pro.preco
                FROM ean e 
                INNER JOIN promocao pro ON (e.codigo_ean = pro.codigo_produto)
                WHERE e.codigo_produto = ? 
                AND pro.data_final >= ? 
                AND pro.data_inicial <= ? 
                AND pro.codigo_loja = ?
                AND e.embalagem='UN'         
                LIMIT 1
                """;

        try {
            Map<String, Object> resultadoPromocao = mysqlExternalJdbcTemplate.queryForMap(sqlPrecoPromocional, codigoProduto, dataAtual, dataAtual, codLojaRms);
            if (resultadoPromocao != null && resultadoPromocao.containsKey("preco")) {
                return resultadoPromocao.get("preco");
            }
        } catch (Exception e) {
            System.err.println("⚠️ Erro ao buscar preço promocional para o produto " + codigoProduto + ": " + e.getMessage());
        }

        // Se não houver promoção, busca o preço normal no segundo SELECT
        String sqlPrecoNormal = """
                SELECT nv.preco 
                FROM ean e 
                INNER JOIN nivel_preco_produto nv ON (e.codigo_ean = nv.codigo_produto)
                WHERE e.codigo_produto = ? 
                AND nv.codigo_nivel = ?
                AND e.embalagem='UN' 
                LIMIT 1
                """;

        try {
            Map<String, Object> resultadoPrecoNormal = mysqlExternalJdbcTemplate.queryForMap(sqlPrecoNormal, codigoProduto, codLojaRms);
            if (resultadoPrecoNormal != null && resultadoPrecoNormal.containsKey("preco")) {
                return resultadoPrecoNormal.get("preco");
            }
        } catch (Exception e) {
            System.err.println("⚠️ Erro ao buscar preço normal para o produto " + codigoProduto + ": " + e.getMessage());
        }

        return "Sem Preco";
    }
}
