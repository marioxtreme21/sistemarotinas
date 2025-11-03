package sistema.rotinas.primefaces.util;

import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RelatorioUtil {

    /**
     * Gera um relatório PDF a partir de um .jasper e dados, garantindo compatibilidade com WAR/Tomcat + Windows/Linux.
     */
    public static String gerarRelatorioPDF(String nomeRelatorio, List<?> dados,
                                           String numeroFilial, String pathArquivoSaida) throws Exception {
        try {
            // 1️⃣ Carrega .jasper via classpath (ex: /resources/relatorios/...)
            InputStream jasperStream = Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream("relatorios/" + nomeRelatorio);
            if (jasperStream == null) {
                throw new RuntimeException("❌ Arquivo Jasper não encontrado: relatorios/" + nomeRelatorio);
            }

            // 2️⃣ Localiza imagens no classpath
            URL urlImagens = Thread.currentThread()
                    .getContextClassLoader()
                    .getResource("images");
            if (urlImagens == null) {
                throw new RuntimeException("❌ Pasta de imagens não encontrada no classpath: resources/images");
            }
            String pathImagens = Paths.get(urlImagens.toURI()).toAbsolutePath().toString();

            // 3️⃣ Prepara parâmetros Jasper
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(dados);
            Map<String, Object> parametros = new HashMap<>();
            parametros.put("REPORT_PARAMETERS_IMG", pathImagens + File.separator);
            parametros.put("NUMERO_FILIAL", numeroFilial);

            // 4️⃣ Preenche relatório
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperStream, parametros, dataSource);

            // 5️⃣ Verifica e cria diretório de saída
            File arquivoPDF = new File(pathArquivoSaida);
            File dirSaida = arquivoPDF.getParentFile();
            if (dirSaida != null && !dirSaida.exists()) {
                boolean criado = dirSaida.mkdirs();
                if (!criado) {
                    throw new RuntimeException("❌ Falha ao criar diretório: " + dirSaida.getAbsolutePath());
                }
            }
            if (!Files.isWritable(dirSaida.toPath())) {
                throw new RuntimeException("❌ Sem permissão para escrever em: " + dirSaida.getAbsolutePath());
            }

            // 6️⃣ Exporta para PDF
            JasperExportManager.exportReportToPdfFile(jasperPrint, pathArquivoSaida);
            System.out.println("✅ Relatório gerado com sucesso: " + pathArquivoSaida);
            return pathArquivoSaida;

        } catch (Exception e) {
            System.err.println("❌ Erro ao gerar PDF: " + e.getMessage());
            throw e;
        }
    }
}
