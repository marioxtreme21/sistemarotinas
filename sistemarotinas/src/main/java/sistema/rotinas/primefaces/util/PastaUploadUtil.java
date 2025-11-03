package sistema.rotinas.primefaces.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

@Component
public class PastaUploadUtil {

    public static String PASTA_BASE;
    public static String PASTA_ANEXOS;
    public static String PASTA_COMPROVANTES;
    public static String PASTA_RELATORIOS;
    public static String PASTA_EMAIL_IMAGES;

    public static final String URL_BASE_WEB = "/uploads";

    public PastaUploadUtil(@Value("${app.upload.base-dir}") String baseDir) {
        // 🔧 Garante caminho absoluto e multiplataforma
        PASTA_BASE = new File(baseDir).getAbsolutePath();
        PASTA_ANEXOS = PASTA_BASE + File.separator + "anexos";
        PASTA_COMPROVANTES = PASTA_BASE + File.separator + "comprovantes";
        PASTA_RELATORIOS = PASTA_BASE + File.separator + "relatorios";
        PASTA_EMAIL_IMAGES = PASTA_BASE + File.separator + "email_images";

        // 🔒 Verifica permissão de escrita
        if (!Files.isWritable(Paths.get(PASTA_BASE))) {
            throw new RuntimeException("❌ Diretório sem permissão de escrita: " + PASTA_BASE);
        }

        criarPastaSeNaoExistir(PASTA_ANEXOS);
        criarPastaSeNaoExistir(PASTA_COMPROVANTES);
        criarPastaSeNaoExistir(PASTA_RELATORIOS);
        criarPastaSeNaoExistir(PASTA_EMAIL_IMAGES);
    }

    /**
     * 🔧 Cria a pasta se não existir, usando NIO
     */
    private static void criarPastaSeNaoExistir(String caminho) {
        try {
            Files.createDirectories(Paths.get(caminho));
            System.out.println("📁 Pasta garantida: " + caminho);
        } catch (Exception e) {
            System.err.println("❌ Erro ao criar diretório: " + caminho);
            throw new RuntimeException("Erro ao criar diretório: " + caminho, e);
        }
    }

    /**
     * 🔥 Gera caminho físico para anexos.
     */
    public static String gerarCaminhoAnexo(LocalDate data, Long chamadoId) {
        return gerarCaminhoGenerico(PASTA_ANEXOS, data, chamadoId);
    }

    /**
     * 🔥 Gera caminho físico para imagens inline.
     */
    public static String gerarCaminhoImagemEmail(LocalDate data, Long chamadoId) {
        return gerarCaminhoGenerico(PASTA_EMAIL_IMAGES, data, chamadoId);
    }

    /**
     * 🔥 Método genérico para geração de caminho físico.
     */
    private static String gerarCaminhoGenerico(String base, LocalDate data, Long chamadoId) {
        String path = String.join(File.separator,
                base,
                String.valueOf(data.getYear()),
                String.format("%02d", data.getMonthValue()),
                String.format("%02d", data.getDayOfMonth()),
                String.valueOf(chamadoId));
        criarPastaSeNaoExistir(path);
        return path;
    }

    /**
     * 🔗 Gera URL pública para anexos.
     */
    public static String gerarUrlPublicaAnexo(LocalDate data, Long chamadoId, String nomeArquivo) {
        validarNomeArquivo(nomeArquivo);
        return URL_BASE_WEB + "/anexos/" +
                data.getYear() + "/" +
                String.format("%02d", data.getMonthValue()) + "/" +
                String.format("%02d", data.getDayOfMonth()) + "/" +
                chamadoId + "/" + nomeArquivo;
    }

    /**
     * 🔗 Gera URL pública para imagens inline.
     */
    public static String gerarUrlPublicaImagemEmail(LocalDate data, Long chamadoId, String nomeArquivo) {
        validarNomeArquivo(nomeArquivo);
        return URL_BASE_WEB + "/email_images/" +
                data.getYear() + "/" +
                String.format("%02d", data.getMonthValue()) + "/" +
                String.format("%02d", data.getDayOfMonth()) + "/" +
                chamadoId + "/" + nomeArquivo;
    }

    /**
     * 🗓️ Retorna a data atual.
     */
    public static LocalDate dataHoje() {
        return LocalDate.now();
    }

    /**
     * 🔐 Validação para nome de arquivo.
     */
    private static void validarNomeArquivo(String nomeArquivo) {
        if (nomeArquivo == null || nomeArquivo.isBlank()) {
            throw new IllegalArgumentException("Nome do arquivo não pode ser nulo ou vazio.");
        }
    }
}
