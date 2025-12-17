// src/main/java/sistema/rotinas/primefaces/service/AjusteBaixaVoucherService.java
package sistema.rotinas.primefaces.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sistema.rotinas.primefaces.dto.ResultadoRotinaVoucherDTO;
import sistema.rotinas.primefaces.service.interfaces.IAjusteBaixaVoucherService;

@Service
public class AjusteBaixaVoucherService implements IAjusteBaixaVoucherService {

    private static final Logger log = LoggerFactory.getLogger(AjusteBaixaVoucherService.class);

    private final JdbcTemplate mysqlExternalJdbcTemplate;      // ECONECT
    private final JdbcTemplate oracleExternoJdbcTemplate;      // CONSINCO / BDC

    public AjusteBaixaVoucherService(
            @Qualifier("mysqlExternalJdbcTemplate") JdbcTemplate mysqlExternalJdbcTemplate,
            @Qualifier("oracleExternoJdbcTemplate") JdbcTemplate oracleExternoJdbcTemplate) {
        this.mysqlExternalJdbcTemplate = mysqlExternalJdbcTemplate;
        this.oracleExternoJdbcTemplate = oracleExternoJdbcTemplate;
    }

    /**
     * Busca os usos de voucher no ECONECT (MySQL), base capa_cupom_venda / movimento_finalizadora.
     *
     * D/para (ECONECT → MOV):
     *  - CPF_CNPJ        = ca.numero_cpf_cnpj
     *  - DATA_MOVIMENTO  = ca.hora_venda
     *  - NUMERO_LOJA     = ca.numero_loja
     *  - NUMERO_PDV      = ca.numero_pdv
     *  - NUMERO_CUPOM    = ca.numero_cupom
     *  - SEQUENCIA       = mv.sequencia
     *  - VALOR_TRANSACAO = mv.valor
     *  - CPF_CNP_TIT     = ca.numero_cpf_cnpj
     *  - TIP_TRN         = 0
     */
    private static final String SQL_BUSCAR_USOS_ECONECT =
            "SELECT " +
            "  ca.data_venda, " +
            "  ca.hora_venda, " +
            "  ca.numero_loja, " +
            "  ca.numero_pdv, " +
            "  ca.numero_cupom, " +
            "  mv.valor AS valor_transacao, " +
            "  mv.sequencia, " +
            "  ca.numero_cpf_cnpj, " +
            "  ca.cnpj_cpf_consumidor " +
            "FROM capa_cupom_venda ca " +
            "JOIN movimento_finalizadora mv " +
            "  ON ca.data_venda   = mv.data_movimento " +
            " AND ca.numero_loja  = mv.numero_loja " +
            " AND ca.numero_pdv   = mv.numero_pdv " +
            " AND ca.numero_cupom = mv.numero_cupom " +
            "WHERE ca.data_venda >= ? " +
            "  AND ca.data_venda <  ? " +
            "  AND mv.codigo_finalizadora = 31 " +
            "  AND ca.situacao_capa = 7 " +
            "ORDER BY ca.data_venda, ca.numero_loja, ca.numero_pdv, ca.numero_cupom, mv.sequencia";

    /** Busca cliente no controle por CPF. */
    private static final String SQL_BUSCAR_CLIENTE_POR_CPF =
            "SELECT CODIGO_CLIENTE " +
            "  FROM tb_bdc_controle_clientes " +
            " WHERE CPF_CNPJ = ?";

    /** Verifica se já existe movimento idêntico em tb_bdc_controle_clientes_mov. */
    private static final String SQL_EXISTE_MOVIMENTO =
            "SELECT COUNT(1) " +
            "  FROM tb_bdc_controle_clientes_mov " +
            " WHERE CPF_CNPJ = ? " +
            "   AND DATA_MOVIMENTO = ? " +
            "   AND NUMERO_LOJA = ? " +
            "   AND NUMERO_PDV = ? " +
            "   AND NUMERO_CUPOM = ? " +
            "   AND SEQUENCIA = ? " +
            "   AND CODIGO_CLIENTE = ?";

    /** Insert na tb_bdc_controle_clientes_mov (TIP_TRN = 0). */
    private static final String SQL_INSERIR_MOVIMENTO =
            "INSERT INTO tb_bdc_controle_clientes_mov (" +
            "  CPF_CNPJ, DATA_MOVIMENTO, NUMERO_LOJA, NUMERO_PDV, NUMERO_CUPOM, SEQUENCIA, " +
            "  VALOR_TRANSACAO, CODIGO_CLIENTE, CPF_CNP_TIT, TIP_TRN" +
            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    /** Update de situação do cliente (99) por CPF. */
    private static final String SQL_MARCAR_CLIENTE_99 =
            "UPDATE tb_bdc_controle_clientes " +
            "   SET situacao = 99 " +
            " WHERE CPF_CNPJ = ? " +
            "   AND (situacao IS NULL OR situacao <> 99)";

    @Override
    @Transactional
    public ResultadoRotinaVoucherDTO executar(LocalDate dataInicial, LocalDate dataFinal) {

        LocalDate ini = dataInicial;
        LocalDate fim = dataFinal;

        // fallback (caso alguém chame com null)
        if (ini == null || fim == null) {
            LocalDate hoje = LocalDate.now();
            fim = hoje;
            ini = hoje.minusDays(2);
        }

        log.info("▶ Iniciando rotina de baixa voucher. Intervalo ECONECT: {} até {} (inclusive)",
                ini, fim);

        ResultadoRotinaVoucherDTO resultado = new ResultadoRotinaVoucherDTO();

        // Busca usos no ECONECT
        List<UsoVoucherEconect> usos = buscarUsosEconect(ini, fim);
        resultado.setTotalLidosEconect(usos.size());

        // Para rastrear CPFs não encontrados ou vazios
        Set<String> cpfsProblema = new HashSet<>();

        for (UsoVoucherEconect uso : usos) {
            try {
                processarUso(uso, resultado, cpfsProblema);
            } catch (Exception e) {
                log.error("❌ Erro processando uso: {}", uso, e);
            }
        }

        if (!cpfsProblema.isEmpty()) {
            String listaCpfs = String.join(" | ", cpfsProblema);
            log.warn("⚠ CPFs com problema (vazio ou sem cliente em tb_bdc_controle_clientes) ({}): {}",
                    cpfsProblema.size(),
                    listaCpfs);
        }

        log.info(
            "✅ Rotina concluída. Lidos={}, Inseridos={}, Atualizados={}, JáExistentes={}, JáExistentesSemAlt={}, CPF não encontrado={}, Clientes marcados 99={}",
            resultado.getTotalLidosEconect(),
            resultado.getTotalMovInseridos(),
            resultado.getTotalMovAtualizados(),
            resultado.getTotalMovJaExistentes(),
            resultado.getTotalMovJaExistentesSemAlteracao(),
            resultado.getTotalClientesNaoEncontrados(),
            resultado.getTotalClientesMarcadosSituacao99()
        );

        return resultado;
    }

    /**
     * Busca os usos de voucher no ECONECT (MySQL) no intervalo de data_venda.
     */
    private List<UsoVoucherEconect> buscarUsosEconect(LocalDate dataInicial, LocalDate dataFinal) {

        LocalDate fimExclusive = dataFinal.plusDays(1);

        return mysqlExternalJdbcTemplate.query(
                SQL_BUSCAR_USOS_ECONECT,
                ps -> {
                    ps.setObject(1, dataInicial.atStartOfDay());
                    ps.setObject(2, fimExclusive.atStartOfDay());
                },
                (rs, rowNum) -> {
                    UsoVoucherEconect uso = new UsoVoucherEconect();

                    Timestamp tsHora = rs.getTimestamp("hora_venda");
                    if (tsHora != null) {
                        uso.setDataMovimento(tsHora.toLocalDateTime());
                    } else {
                        java.sql.Date dv = rs.getDate("data_venda");
                        if (dv != null) {
                            uso.setDataMovimento(dv.toLocalDate().atStartOfDay());
                        }
                    }

                    uso.setNumeroLoja(rs.getInt("numero_loja"));
                    uso.setNumeroPdv(rs.getInt("numero_pdv"));
                    uso.setNumeroCupom(rs.getLong("numero_cupom"));
                    uso.setSequencia(rs.getInt("sequencia"));

                    BigDecimal valor = rs.getBigDecimal("valor_transacao");
                    uso.setValor(valor != null ? valor : BigDecimal.ZERO);

                    String cpfNumero     = trimToNull(rs.getString("numero_cpf_cnpj"));
                    String cpfConsumidor = trimToNull(rs.getString("cnpj_cpf_consumidor"));

                    uso.setCpfOrigNumero(cpfNumero);
                    uso.setCpfOrigConsumidor(cpfConsumidor);

                    // CPF "final" usado para busca na tb_bdc_controle_clientes
                    String cpf = cpfNumero != null ? cpfNumero : cpfConsumidor;
                    uso.setCpfCnpj(cpf);

                    return uso;
                }
        );
    }

    /**
     * Processa um uso individual:
     *  1) Localiza cliente por CPF em tb_bdc_controle_clientes
     *  2) Verifica se já existe movimento em tb_bdc_controle_clientes_mov
     *  3) Se não existir, insere
     *  4) Marca situação 99 no cliente (tb_bdc_controle_clientes)
     *  5) Quando não achar cliente ou CPF vier vazio, incrementa contador e LOGA detalhes
     */
    private void processarUso(UsoVoucherEconect uso,
                              ResultadoRotinaVoucherDTO resultado,
                              Set<String> cpfsProblema) {

        String cpf = uso.getCpfCnpj();

        // Caso 1: CPF não veio em nenhuma das colunas do ECONECT
        if (cpf == null || cpf.isBlank()) {
            resultado.setTotalClientesNaoEncontrados(
                    resultado.getTotalClientesNaoEncontrados() + 1
            );

            String detalheCpf = String.format(
                    "CPF_FINAL=<vazio>, numero_cpf_cnpj='%s', cnpj_cpf_consumidor='%s', loja=%d, pdv=%d, cupom=%d, seq=%d",
                    safe(uso.getCpfOrigNumero()),
                    safe(uso.getCpfOrigConsumidor()),
                    uso.getNumeroLoja(),
                    uso.getNumeroPdv(),
                    uso.getNumeroCupom(),
                    uso.getSequencia()
            );

            cpfsProblema.add(detalheCpf);

            log.warn("⚠ CPF NÃO INFORMADO no ECONECT para uso: {}", detalheCpf);
            return;
        }

        String cpfLimpo = cpf.trim();

        // Caso 2: CPF informado, mas não encontrou cliente na tb_bdc_controle_clientes
        ClienteControleVoucher cliente = buscarClienteNoOraclePorCpf(cpfLimpo);

        if (cliente == null || cliente.getCodigoCliente() == null || cliente.getCodigoCliente().isBlank()) {

            resultado.setTotalClientesNaoEncontrados(
                    resultado.getTotalClientesNaoEncontrados() + 1
            );

            String detalheCpf = String.format(
                    "CPF_FINAL='%s', numero_cpf_cnpj='%s', cnpj_cpf_consumidor='%s', loja=%d, pdv=%d, cupom=%d, seq=%d",
                    cpfLimpo,
                    safe(uso.getCpfOrigNumero()),
                    safe(uso.getCpfOrigConsumidor()),
                    uso.getNumeroLoja(),
                    uso.getNumeroPdv(),
                    uso.getNumeroCupom(),
                    uso.getSequencia()
            );

            cpfsProblema.add(detalheCpf);

            log.warn(
                "⚠ CPF INFORMADO, MAS NÃO LOCALIZADO em tb_bdc_controle_clientes: {}",
                detalheCpf
            );

            return;
        }

        String codigoCliente = cliente.getCodigoCliente().trim();

        // Verifica se já existe movimento
        if (existeMovimento(uso, codigoCliente)) {
            resultado.setTotalMovJaExistentes(
                    resultado.getTotalMovJaExistentes() + 1
            );
            resultado.setTotalMovJaExistentesSemAlteracao(
                    resultado.getTotalMovJaExistentesSemAlteracao() + 1
            );
            return;
        }

        // Insere movimento
        inserirMovimento(uso, codigoCliente);
        resultado.setTotalMovInseridos(resultado.getTotalMovInseridos() + 1);

        // Marca situação 99 no cliente
        marcarClienteSituacao99(cpfLimpo, resultado);
    }

    private ClienteControleVoucher buscarClienteNoOraclePorCpf(String cpf) {
        try {
            List<ClienteControleVoucher> lista = oracleExternoJdbcTemplate.query(
                    SQL_BUSCAR_CLIENTE_POR_CPF,
                    ps -> ps.setString(1, cpf),
                    (rs, rowNum) -> {
                        ClienteControleVoucher c = new ClienteControleVoucher();
                        c.setCodigoCliente(rs.getString("CODIGO_CLIENTE"));
                        return c;
                    }
            );
            return lista.isEmpty() ? null : lista.get(0);
        } catch (Exception e) {
            log.error("❌ Erro ao buscar cliente por CPF {} em tb_bdc_controle_clientes", cpf, e);
            return null;
        }
    }

    private boolean existeMovimento(UsoVoucherEconect uso, String codigoCliente) {
        Timestamp ts = uso.getDataMovimento() != null
                ? Timestamp.valueOf(uso.getDataMovimento())
                : null;

        Integer count = oracleExternoJdbcTemplate.queryForObject(
                SQL_EXISTE_MOVIMENTO,
                new Object[]{
                        uso.getCpfCnpj(),
                        ts,
                        uso.getNumeroLoja(),
                        uso.getNumeroPdv(),
                        uso.getNumeroCupom(),
                        uso.getSequencia(),
                        codigoCliente
                },
                Integer.class
        );

        return count != null && count > 0;
    }

    private void inserirMovimento(UsoVoucherEconect uso, String codigoCliente) {
        Timestamp ts = uso.getDataMovimento() != null
                ? Timestamp.valueOf(uso.getDataMovimento())
                : null;

        oracleExternoJdbcTemplate.update(SQL_INSERIR_MOVIMENTO, ps -> {
            ps.setString(1, uso.getCpfCnpj());
            ps.setTimestamp(2, ts);
            ps.setInt(3, uso.getNumeroLoja());
            ps.setInt(4, uso.getNumeroPdv());
            ps.setLong(5, uso.getNumeroCupom());
            ps.setInt(6, uso.getSequencia());
            ps.setBigDecimal(7, uso.getValor());
            ps.setString(8, codigoCliente);
            ps.setString(9, uso.getCpfCnpj()); // CPF_CNP_TIT = CPF do uso
            ps.setInt(10, 0); // TIP_TRN = 0
        });
    }

    private void marcarClienteSituacao99(String cpf, ResultadoRotinaVoucherDTO resultado) {
        int linhas = oracleExternoJdbcTemplate.update(SQL_MARCAR_CLIENTE_99, ps -> ps.setString(1, cpf));
        if (linhas > 0) {
            resultado.setTotalClientesMarcadosSituacao99(
                    resultado.getTotalClientesMarcadosSituacao99() + linhas
            );
        }
    }

    /* ===================== HELPERS ===================== */

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String safe(String s) {
        return s == null ? "<null>" : s;
    }

    /* ===================== CLASSES INTERNAS SIMPLES ===================== */

    private static class UsoVoucherEconect {
        private LocalDateTime dataMovimento;
        private String cpfCnpj;          // CPF final usado na busca
        private String cpfOrigNumero;    // numero_cpf_cnpj original
        private String cpfOrigConsumidor;// cnpj_cpf_consumidor original
        private int numeroLoja;
        private int numeroPdv;
        private long numeroCupom;
        private int sequencia;
        private BigDecimal valor;

        public LocalDateTime getDataMovimento() {
            return dataMovimento;
        }

        public void setDataMovimento(LocalDateTime dataMovimento) {
            this.dataMovimento = dataMovimento;
        }

        public String getCpfCnpj() {
            return cpfCnpj;
        }

        public void setCpfCnpj(String cpfCnpj) {
            this.cpfCnpj = cpfCnpj;
        }

        public String getCpfOrigNumero() {
            return cpfOrigNumero;
        }

        public void setCpfOrigNumero(String cpfOrigNumero) {
            this.cpfOrigNumero = cpfOrigNumero;
        }

        public String getCpfOrigConsumidor() {
            return cpfOrigConsumidor;
        }

        public void setCpfOrigConsumidor(String cpfOrigConsumidor) {
            this.cpfOrigConsumidor = cpfOrigConsumidor;
        }

        public int getNumeroLoja() {
            return numeroLoja;
        }

        public void setNumeroLoja(int numeroLoja) {
            this.numeroLoja = numeroLoja;
        }

        public int getNumeroPdv() {
            return numeroPdv;
        }

        public void setNumeroPdv(int numeroPdv) {
            this.numeroPdv = numeroPdv;
        }

        public long getNumeroCupom() {
            return numeroCupom;
        }

        public void setNumeroCupom(long numeroCupom) {
            this.numeroCupom = numeroCupom;
        }

        public int getSequencia() {
            return sequencia;
        }

        public void setSequencia(int sequencia) {
            this.sequencia = sequencia;
        }

        public BigDecimal getValor() {
            return valor;
        }

        public void setValor(BigDecimal valor) {
            this.valor = valor;
        }

        @Override
        public String toString() {
            return "UsoVoucherEconect{" +
                    "dataMovimento=" + dataMovimento +
                    ", cpfCnpj='" + cpfCnpj + '\'' +
                    ", cpfOrigNumero='" + cpfOrigNumero + '\'' +
                    ", cpfOrigConsumidor='" + cpfOrigConsumidor + '\'' +
                    ", numeroLoja=" + numeroLoja +
                    ", numeroPdv=" + numeroPdv +
                    ", numeroCupom=" + numeroCupom +
                    ", sequencia=" + sequencia +
                    ", valor=" + valor +
                    '}';
        }
    }

    private static class ClienteControleVoucher {
        private String codigoCliente;

        public String getCodigoCliente() {
            return codigoCliente;
        }

        public void setCodigoCliente(String codigoCliente) {
            this.codigoCliente = codigoCliente;
        }

        @Override
        public String toString() {
            return "ClienteControleVoucher{" +
                    "codigoCliente='" + codigoCliente + '\'' +
                    '}';
        }
    }
}
