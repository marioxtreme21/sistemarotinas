package sistema.rotinas.primefaces.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Component
public class PastaUploadUtil {

    public static String PASTA_BASE;
    public static String PASTA_ANEXOS;
    public static String PASTA_COMPROVANTES;
    public static String PASTA_RELATORIOS;
    public static String PASTA_EMAIL_IMAGES;

    // ✅ Rotinas alteradas
    public static String PASTA_ROTINAALTERADOS;

    // ✅ PRICE
    public static String PASTA_PRICE;

    // ✅ MGV
    public static String PASTA_MGV;

    public static final String URL_BASE_WEB = "/uploads";

    private static final DateTimeFormatter DIA_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter HORA_FMT = DateTimeFormatter.ofPattern("HHmmss");

    public PastaUploadUtil(@Value("${app.upload.base-dir}") String baseDir) {
        // 🔧 Garante caminho absoluto e multiplataforma
        PASTA_BASE = new File(baseDir).getAbsolutePath();
        PASTA_ANEXOS = PASTA_BASE + File.separator + "anexos";
        PASTA_COMPROVANTES = PASTA_BASE + File.separator + "comprovantes";
        PASTA_RELATORIOS = PASTA_BASE + File.separator + "relatorios";
        PASTA_EMAIL_IMAGES = PASTA_BASE + File.separator + "email_images";

        // ✅ Rotinas
        PASTA_ROTINAALTERADOS = PASTA_BASE + File.separator + "rotinaalterados";

        // ✅ PRICE e MGV
        PASTA_PRICE = PASTA_ROTINAALTERADOS + File.separator + "price";
        PASTA_MGV   = PASTA_ROTINAALTERADOS + File.separator + "mgv";

        // 🔒 Verifica permissão de escrita
        if (!Files.isWritable(Paths.get(PASTA_BASE))) {
            throw new RuntimeException("❌ Diretório sem permissão de escrita: " + PASTA_BASE);
        }

        criarPastaSeNaoExistir(PASTA_ANEXOS);
        criarPastaSeNaoExistir(PASTA_COMPROVANTES);
        criarPastaSeNaoExistir(PASTA_RELATORIOS);
        criarPastaSeNaoExistir(PASTA_EMAIL_IMAGES);

        // ✅ Rotinas
        criarPastaSeNaoExistir(PASTA_ROTINAALTERADOS);

        // ✅ PRICE e MGV
        criarPastaSeNaoExistir(PASTA_PRICE);
        criarPastaSeNaoExistir(PASTA_MGV);
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

    // =========================================================
    // ✅ PRICE (LJ{codLojaRms}/YYYY-MM-DD/)
    // =========================================================

    public static String prefixoLojaPrice(String codLojaRms) {
        if (codLojaRms == null || codLojaRms.trim().isEmpty()) return "LJ";
        return "LJ" + codLojaRms.trim();
    }

    public static Path pastaPriceBase() {
        return Paths.get(PASTA_PRICE);
    }

    public static Path pastaPriceLojaBase(String codLojaRms) {
        Path p = pastaPriceBase().resolve(prefixoLojaPrice(codLojaRms));
        try {
            Files.createDirectories(p);
            return p;
        } catch (IOException e) {
            throw new RuntimeException("Erro ao criar pasta da loja PRICE: " + p, e);
        }
    }

    public static Path pastaPriceLojaDia(String codLojaRms, LocalDate dia) {
        String d = (dia != null ? dia.format(DIA_FMT) : LocalDate.now().format(DIA_FMT));
        Path p = pastaPriceLojaBase(codLojaRms).resolve(d);
        try {
            Files.createDirectories(p);
            return p;
        } catch (IOException e) {
            throw new RuntimeException("Erro ao criar pasta do dia PRICE: " + p, e);
        }
    }

    /**
     * ✅ Se já existir o arquivo no dia, renomeia o existente com _HHmmss e devolve o path do "novo".
     */
    public static Path prepararArquivoNoDia(Path pastaDia, String nomeArquivo) {
        if (pastaDia == null) throw new IllegalArgumentException("pastaDia é obrigatória");
        if (nomeArquivo == null || nomeArquivo.trim().isEmpty()) throw new IllegalArgumentException("nomeArquivo é obrigatório");

        try {
            Files.createDirectories(pastaDia);

            Path destino = pastaDia.resolve(nomeArquivo.trim());
            if (!Files.exists(destino)) return destino;

            String base = nomeArquivo.trim();
            String ext = "";
            int idx = base.lastIndexOf('.');
            if (idx > 0 && idx < base.length() - 1) {
                ext = base.substring(idx);
                base = base.substring(0, idx);
            }

            String hora = LocalTime.now().format(HORA_FMT);
            Path backup = pastaDia.resolve(base + "_" + hora + ext);

            Files.move(destino, backup, StandardCopyOption.REPLACE_EXISTING);
            return destino;

        } catch (Exception e) {
            throw new RuntimeException("Erro ao preparar arquivo no dia para gravação: " + nomeArquivo, e);
        }
    }

    /**
     * ✅ Retenção por loja: apaga subpastas no padrão YYYY-MM-DD mais antigas que "diasRetencao".
     */
    public static void limparPriceLojaPorRetencao(String codLojaRms, int diasRetencao) {
        if (diasRetencao <= 0) return;

        Path baseLoja = pastaPriceLojaBase(codLojaRms);
        LocalDate limite = LocalDate.now().minusDays(diasRetencao);

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(baseLoja)) {
            for (Path p : ds) {
                if (!Files.isDirectory(p)) continue;

                String nome = p.getFileName().toString();
                LocalDate data;
                try {
                    data = LocalDate.parse(nome, DIA_FMT);
                } catch (Exception ignore) {
                    continue;
                }

                if (data.isBefore(limite)) {
                    deleteRecursively(p);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao aplicar retenção na pasta PRICE da loja " + codLojaRms, e);
        }
    }

    // =========================================================
    // ✅ MGV (LJ{codLojaRms}/YYYY-MM-DD/)
    // (mesmo padrão do PRICE, apenas base diferente)
    // =========================================================

    public static String prefixoLojaMgv(String codLojaRms) {
        if (codLojaRms == null || codLojaRms.trim().isEmpty()) return "LJ";
        return "LJ" + codLojaRms.trim();
    }

    public static Path pastaMgvBase() {
        return Paths.get(PASTA_MGV);
    }

    public static Path pastaMgvLojaBase(String codLojaRms) {
        Path p = pastaMgvBase().resolve(prefixoLojaMgv(codLojaRms));
        try {
            Files.createDirectories(p);
            return p;
        } catch (IOException e) {
            throw new RuntimeException("Erro ao criar pasta da loja MGV: " + p, e);
        }
    }

    public static Path pastaMgvLojaDia(String codLojaRms, LocalDate dia) {
        String d = (dia != null ? dia.format(DIA_FMT) : LocalDate.now().format(DIA_FMT));
        Path p = pastaMgvLojaBase(codLojaRms).resolve(d);
        try {
            Files.createDirectories(p);
            return p;
        } catch (IOException e) {
            throw new RuntimeException("Erro ao criar pasta do dia MGV: " + p, e);
        }
    }

    /**
     * ✅ Retenção por loja MGV: apaga subpastas no padrão YYYY-MM-DD mais antigas que "diasRetencao".
     */
    public static void limparMgvLojaPorRetencao(String codLojaRms, int diasRetencao) {
        if (diasRetencao <= 0) return;

        Path baseLoja = pastaMgvLojaBase(codLojaRms);
        LocalDate limite = LocalDate.now().minusDays(diasRetencao);

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(baseLoja)) {
            for (Path p : ds) {
                if (!Files.isDirectory(p)) continue;

                String nome = p.getFileName().toString();
                LocalDate data;
                try {
                    data = LocalDate.parse(nome, DIA_FMT);
                } catch (Exception ignore) {
                    continue;
                }

                if (data.isBefore(limite)) {
                    deleteRecursively(p);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao aplicar retenção na pasta MGV da loja " + codLojaRms, e);
        }
    }

    // =========================================================
    // Helpers internos
    // =========================================================

    private static void deleteRecursively(Path root) throws IOException {
        if (root == null || !Files.exists(root)) return;

        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    // =========================================================
    // (código antigo permanece)
    // =========================================================

    public static String gerarCaminhoAnexo(LocalDate data, Long chamadoId) {
        return gerarCaminhoGenerico(PASTA_ANEXOS, data, chamadoId);
    }

    public static String gerarCaminhoImagemEmail(LocalDate data, Long chamadoId) {
        return gerarCaminhoGenerico(PASTA_EMAIL_IMAGES, data, chamadoId);
    }

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

    public static String gerarUrlPublicaAnexo(LocalDate data, Long chamadoId, String nomeArquivo) {
        validarNomeArquivo(nomeArquivo);
        return URL_BASE_WEB + "/anexos/" +
                data.getYear() + "/" +
                String.format("%02d", data.getMonthValue()) + "/" +
                String.format("%02d", data.getDayOfMonth()) + "/" +
                chamadoId + "/" + nomeArquivo;
    }

    public static String gerarUrlPublicaImagemEmail(LocalDate data, Long chamadoId, String nomeArquivo) {
        validarNomeArquivo(nomeArquivo);
        return URL_BASE_WEB + "/email_images/" +
                data.getYear() + "/" +
                String.format("%02d", data.getMonthValue()) + "/" +
                String.format("%02d", data.getDayOfMonth()) + "/" +
                chamadoId + "/" + nomeArquivo;
    }

    public static LocalDate dataHoje() {
        return LocalDate.now();
    }

    private static void validarNomeArquivo(String nomeArquivo) {
        if (nomeArquivo == null || nomeArquivo.isBlank()) {
            throw new IllegalArgumentException("Nome do arquivo não pode ser nulo ou vazio.");
        }
    }
}