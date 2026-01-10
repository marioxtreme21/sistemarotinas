package sistema.rotinas.primefaces.dto;

/**
 * ✅ Resultado de download via SFTP com o mtime remoto (epochSeconds).
 * - nomeArquivo: nome do arquivo escolhido/baixado
 * - mtimeEpochSeconds: lastModified remoto (segundos). Pode ser null.
 */
public record SftpDownloadInfo(String nomeArquivo, Long mtimeEpochSeconds) { }