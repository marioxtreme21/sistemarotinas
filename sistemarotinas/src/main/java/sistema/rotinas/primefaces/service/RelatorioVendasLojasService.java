package sistema.rotinas.primefaces.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import sistema.rotinas.primefaces.dto.VendaLojaResumo;
import sistema.rotinas.primefaces.model.Loja;
import sistema.rotinas.primefaces.repository.LojaRepository;
import sistema.rotinas.primefaces.service.interfaces.IRelatorioVendasLojasService;

@Service
public class RelatorioVendasLojasService implements IRelatorioVendasLojasService {

    private static final Logger log = LoggerFactory.getLogger(RelatorioVendasLojasService.class);

    @Autowired
    @Qualifier("mysqlExternalJdbcTemplate") // banco 10.1.1.144 (ECONECT)
    private JdbcTemplate mysqlExternalJdbcTemplate;

    @Autowired
    private LojaRepository lojaRepository;

    @Autowired
    private NotificacaoService notificacaoService;

    /**
     * Gera o resumo de vendas por loja no período.
     * Se codLojaEconect for nulo/vazio, busca TODAS as lojas.
     */
    @Override
    @Transactional
    public List<VendaLojaResumo> gerarRelatorio(LocalDate dataInicial,
                                                LocalDate dataFinal,
                                                String codLojaEconect) {

        if (dataInicial == null || dataFinal == null) {
            throw new IllegalArgumentException("Datas não podem ser nulas.");
        }

        if (dataFinal.isBefore(dataInicial)) {
            throw new IllegalArgumentException("Data final não pode ser menor que a data inicial.");
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT numero_loja, COUNT(numero_cupom) AS clientes, SUM(total_liquido) AS total_venda ")
           .append("FROM capa_cupom_venda ")
           .append("WHERE data_venda BETWEEN ? AND ? ")
           .append("  AND situacao_capa = 7 ")
           .append("  AND tipo_recebimento <> 4 ")
           .append("  AND tipo_capa = 0 ");

        List<Object> params = new ArrayList<>();
        params.add(Date.valueOf(dataInicial));
        params.add(Date.valueOf(dataFinal));

        // Filtro opcional por loja (campo da tela, se for usar)
        if (codLojaEconect != null && !codLojaEconect.isBlank()) {
            sql.append("  AND numero_loja = ? ");
            params.add(Integer.valueOf(codLojaEconect));
        }

        sql.append("GROUP BY numero_loja ORDER BY numero_loja");

        log.debug("Executando relatório de vendas: sql={} params={}", sql, params);

        return mysqlExternalJdbcTemplate.query(
                sql.toString(),
                params.toArray(),
                (rs, rowNum) -> {
                    int numeroLoja = rs.getInt("numero_loja");
                    long clientes = rs.getLong("clientes");
                    BigDecimal totalVenda = rs.getBigDecimal("total_venda");
                    if (totalVenda == null) {
                        totalVenda = BigDecimal.ZERO;
                    }
                    BigDecimal ticketMedio = clientes > 0
                            ? totalVenda.divide(BigDecimal.valueOf(clientes), 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    VendaLojaResumo resumo = new VendaLojaResumo();
                    resumo.setCodLojaEconect(String.valueOf(numeroLoja));
                    resumo.setDescricaoLoja(montarDescricaoLoja(numeroLoja));
                    resumo.setNumeroClientes(clientes);
                    resumo.setTotalVenda(totalVenda);
                    resumo.setTicketMedio(ticketMedio);

                    return resumo;
                }
        );
    }

    /**
     * Usado pela tela. Hoje NÃO aplica mais filtro de "lojas permitidas".
     * O boolean é ignorado e delega para gerarRelatorio sem filtrar por loja.
     */
    @Override
    @Transactional
    public List<VendaLojaResumo> buscarVendasPeriodo(LocalDate dataInicial,
                                                     LocalDate dataFinal,
                                                     boolean apenasLojasPermitidas) {
        // Ignora o parâmetro apenasLojasPermitidas (não há mais restrição por usuário)
        return gerarRelatorio(dataInicial, dataFinal, null);
    }

    /**
     * Gera o relatório e envia por e-mail.
     */
    @Override
    @Transactional
    public void enviarRelatorioPorEmail(LocalDate dataInicial,
                                        LocalDate dataFinal,
                                        String codLojaEconect) {

        List<VendaLojaResumo> dados = gerarRelatorio(dataInicial, dataFinal, codLojaEconect);

        if (dados == null || dados.isEmpty()) {
            log.info("Relatório de vendas por loja sem dados para o período {} a {}. Nenhum e-mail será enviado.",
                     dataInicial, dataFinal);
            return;
        }

        notificacaoService.notificarRelatorioVendasLojas(dados, dataInicial, dataFinal, codLojaEconect);
    }

    /**
     * Lista todas as lojas para popular o combo de filtro da tela.
     */
    @Override
    @Transactional
    public List<Loja> listarLojasParaFiltro() {
        return lojaRepository.findAll();
    }

    // ===================== Helpers =====================

    private String montarDescricaoLoja(int numeroLoja) {
        String codigo = String.valueOf(numeroLoja);

        return lojaRepository.findByCodLojaEconect(codigo)
                .map(loja -> {
                    String codigoExibicao = loja.getCodLojaEconect() != null
                            ? loja.getCodLojaEconect()
                            : codigo;
                    return codigoExibicao + " - " + loja.getNome();
                })
                .orElse("Loja " + codigo);
    }
}
