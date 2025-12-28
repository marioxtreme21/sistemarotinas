package sistema.rotinas.primefaces.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FsCopyService {

    private static final Logger log = LoggerFactory.getLogger(FsCopyService.class);

    /**
     * Copia um arquivo local para um destino em FS montado.
     *
     * @param arquivoLocal        arquivo já baixado localmente
     * @param caminhoFsDestino    caminho do diretório montado (ex: /mnt/datafiles102)
     */
    public void copiarParaFs(Path arquivoLocal, String caminhoFsDestino) {

        if (arquivoLocal == null || !Files.exists(arquivoLocal)) {
            throw new IllegalArgumentException("Arquivo local não existe para copiar para FS.");
        }

        if (caminhoFsDestino == null || caminhoFsDestino.isBlank()) {
            throw new IllegalArgumentException("Caminho FS destino é obrigatório.");
        }

        try {
            Path dirDestino = Paths.get(caminhoFsDestino).normalize();

            // garante diretório
            Files.createDirectories(dirDestino);

            // destino final: diretório + nome do arquivo
            Path destinoFinal = dirDestino.resolve(arquivoLocal.getFileName().toString());

            log.info("FS copy: local={} -> destino={}", arquivoLocal, destinoFinal);

            Files.copy(
                arquivoLocal,
                destinoFinal,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.COPY_ATTRIBUTES
            );

            log.info("FS copy OK: {}", destinoFinal);

        } catch (Exception e) {
            log.error("Erro ao copiar para FS ({}): {}", caminhoFsDestino, e.getMessage(), e);
            throw new RuntimeException("Falha ao copiar para FS: " + e.getMessage(), e);
        }
    }
}
