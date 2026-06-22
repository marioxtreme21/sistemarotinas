package sistema.rotinas.primefaces.service;

import java.io.BufferedWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import sistema.rotinas.primefaces.model.Loja;

@Service
public class MgvPrecoproClubeService {

    private static final Logger LOG = LoggerFactory.getLogger("ROTINA_MGV");

    private static final String NOME_ARQUIVO = "precopro.txt";

    /**
     * Importante:
     * O MGV/balança espera quebra de linha no padrão Windows.
     * Se usar writer.newLine(), no Linux será gravado apenas \n,
     * e alguns leitores interpretam tudo como uma única linha.
     */
    private static final String QUEBRA_LINHA_MGV = "\r\n";

    private static final String SQL =
            "SELECT EAN, PRECO_CLUBE " +
            "  FROM PRODUTOS_ETIQUETAS_ZKONG " +
            " WHERE loja = ? " +
            "   AND embalagem = 'KG' " +
            "   AND tipcodigo = 'B' " +
            "   AND promocao_clube = 'P' " +
            " ORDER BY EAN";

    private final JdbcTemplate oracleExternoJdbcTemplate;

    public MgvPrecoproClubeService(@Qualifier("oracleExternoJdbcTemplate") JdbcTemplate oracleExternoJdbcTemplate) {
        this.oracleExternoJdbcTemplate = oracleExternoJdbcTemplate;
    }

    /**
     * Gera o arquivo precopro.txt a partir da view PRODUTOS_ETIQUETAS_ZKONG.
     *
     * Layout por linha:
     * - 6 primeiros dígitos: código/EAN do produto
     * - 6 últimos dígitos: preço clube em centavos, sem separador decimal
     *
     * Exemplo:
     * 001112005598 => produto 001112 com preço 55,98
     */
    public Path gerarArquivoPrecopro(Loja loja, Path pastaDestino) throws Exception {
        if (loja == null) {
            throw new IllegalArgumentException("Loja não informada para geração do precopro.txt.");
        }
        if (pastaDestino == null) {
            throw new IllegalArgumentException("Pasta destino não informada para geração do precopro.txt.");
        }

        long codLojaRms = resolverCodLojaRms(loja);

        Files.createDirectories(pastaDestino);

        Path destino = pastaDestino.resolve(NOME_ARQUIVO);
        Path temp = pastaDestino.resolve(NOME_ARQUIVO + ".tmp");

        try {
            List<String> linhas = oracleExternoJdbcTemplate.query(
                    SQL,
                    ps -> ps.setLong(1, codLojaRms),
                    (rs, rowNum) -> montarLinha(rs.getString("EAN"), rs.getBigDecimal("PRECO_CLUBE"))
            );

            try (BufferedWriter writer = Files.newBufferedWriter(
                    temp,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE)) {

                for (String linha : linhas) {
                    writer.write(linha);
                    writer.write(QUEBRA_LINHA_MGV);
                }
            }

            moverSubstituindo(temp, destino);

            int total = linhas == null ? 0 : linhas.size();

            LOG.info("precopro.txt gerado com sucesso | codLojaRms={} arquivo={} linhas={}",
                    codLojaRms, destino, total);

            if (total == 0) {
                LOG.warn("precopro.txt gerado vazio | codLojaRms={} arquivo={}", codLojaRms, destino);
            }

            return destino;

        } catch (Exception e) {
            try {
                Files.deleteIfExists(temp);
            } catch (Exception ignored) { }

            LOG.error("Falha ao gerar precopro.txt | codLojaRms={} pastaDestino={} msg={}",
                    codLojaRms, pastaDestino, e.getMessage(), e);
            throw e;
        }
    }

    private static String montarLinha(String ean, BigDecimal precoClube) {
        String codigo6 = formatarCodigo6(ean);
        String preco6 = formatarPreco6(precoClube);
        return codigo6 + preco6;
    }

    private static String formatarCodigo6(String ean) {
        String digitos = somenteNumeros(ean);

        if (digitos == null || digitos.isBlank()) {
            throw new IllegalArgumentException("EAN/código vazio ao gerar precopro.txt.");
        }

        if (digitos.length() > 6) {
            throw new IllegalArgumentException(
                    "EAN/código com mais de 6 dígitos ao gerar precopro.txt: " + digitos);
        }

        return String.format("%6s", digitos).replace(' ', '0');
    }

    private static String formatarPreco6(BigDecimal precoClube) {
        BigDecimal preco = precoClube == null ? BigDecimal.ZERO : precoClube;

        long centavos = preco
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();

        if (centavos < 0 || centavos > 999999) {
            throw new IllegalArgumentException(
                    "PRECO_CLUBE fora do limite para layout 6 dígitos: " + preco);
        }

        return String.format("%06d", centavos);
    }

    private static long resolverCodLojaRms(Loja loja) {
        String cod = loja.getCodLojaRms();
        String digitos = somenteNumeros(cod);

        if (digitos == null || digitos.isBlank()) {
            throw new IllegalArgumentException("codLojaRms não informado na loja para geração do precopro.txt.");
        }

        try {
            return Long.parseLong(digitos);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("codLojaRms inválido para geração do precopro.txt: " + cod, e);
        }
    }

    private static String somenteNumeros(String valor) {
        if (valor == null) return null;
        return valor.replaceAll("\\D", "");
    }

    private static void moverSubstituindo(Path origem, Path destino) throws Exception {
        try {
            Files.move(origem, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(origem, destino, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}