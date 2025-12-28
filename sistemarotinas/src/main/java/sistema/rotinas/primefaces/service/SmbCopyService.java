package sistema.rotinas.primefaces.service;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msdtyp.FileTime;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2CreateOptions;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.msfscc.fileinformation.FileBasicInformation;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;

@Service
public class SmbCopyService {

    private static final Logger log = LoggerFactory.getLogger(SmbCopyService.class);

    /**
     * Copia um arquivo local para um compartilhamento SMB,
     * preservando a data/hora (LastWriteTime) do arquivo original.
     */
    public void copiarParaSmb(
            Path arquivoLocal,
            String servidor,
            String compartilhamento,
            String subpasta,
            String dominio,
            String usuario,
            String senha
    ) {
        validarInputs(arquivoLocal, servidor, compartilhamento, usuario);

        String dominioNorm = dominio == null ? "" : dominio.trim();
        String usuarioNorm = usuario.trim();
        char[] senhaChars = (senha == null ? "" : senha).toCharArray();

        String subpastaNorm = normalizarSubpasta(subpasta);
        String nomeArquivo = arquivoLocal.getFileName().toString();

        String remoteDir = subpastaNorm; // pode ser ""
        String remoteFilePath = joinRemote(remoteDir, nomeArquivo);

        log.info("SMB copy: servidor={} share={} dir={} arquivo={}", servidor, compartilhamento, remoteDir, nomeArquivo);

        SMBClient client = new SMBClient();

        try (Connection connection = client.connect(servidor)) {

            AuthenticationContext auth = new AuthenticationContext(usuarioNorm, senhaChars, dominioNorm);
            Session session = connection.authenticate(auth);

            try (DiskShare share = (DiskShare) session.connectShare(compartilhamento)) {

                ensureDirectories(share, remoteDir);

                // ⚠️ Inclui FILE_WRITE_ATTRIBUTES para permitir ajustar o timestamp depois
                EnumSet<AccessMask> access = EnumSet.of(
                        AccessMask.GENERIC_WRITE,
                        AccessMask.FILE_WRITE_DATA,
                        AccessMask.FILE_WRITE_ATTRIBUTES
                );

                EnumSet<SMB2ShareAccess> shareAccess = EnumSet.of(
                        SMB2ShareAccess.FILE_SHARE_READ,
                        SMB2ShareAccess.FILE_SHARE_WRITE
                );

                SMB2CreateDisposition disposition = SMB2CreateDisposition.FILE_OVERWRITE_IF;

                EnumSet<SMB2CreateOptions> options = EnumSet.of(
                        SMB2CreateOptions.FILE_NON_DIRECTORY_FILE,
                        SMB2CreateOptions.FILE_SEQUENTIAL_ONLY
                );

                // ✅ IMPORTANTE:
                // DiskShare.openFile internamente usa EnumSet.copyOf(attrs).
                // Se passar Collections.emptySet(), dá "Collection is empty".
                // Usando EnumSet.noneOf(FileAttributes.class) fica OK (mesmo vazio).
                EnumSet<FileAttributes> fileAttrs = EnumSet.noneOf(FileAttributes.class);

                // 1) copia o conteúdo
                try (File remote = share.openFile(remoteFilePath, access, fileAttrs, shareAccess, disposition, options);
                     OutputStream os = remote.getOutputStream();
                     InputStream is = Files.newInputStream(arquivoLocal)) {

                    is.transferTo(os);
                    os.flush();
                }

                // 2) aplica data/hora original (do arquivo local, que já veio preservado do SFTP)
                aplicarTimestampOriginalNoSmb(share, remoteFilePath, arquivoLocal);

                log.info("SMB copy OK: {}", remoteFilePath);

            } finally {
                try { session.close(); } catch (Exception ignore) {}
            }

        } catch (Exception e) {
            log.error("Erro ao copiar para SMB ({}\\{}): {}", servidor, compartilhamento, e.getMessage(), e);
            throw new RuntimeException("Falha ao copiar para SMB: " + e.getMessage(), e);
        } finally {
            try { client.close(); } catch (Exception ignore) {}
        }
    }

    private static void aplicarTimestampOriginalNoSmb(DiskShare share, String remoteFilePath, Path arquivoLocal) {
        try {
            long millis = Files.getLastModifiedTime(arquivoLocal).toMillis();
            FileTime ft = FileTime.ofEpochMillis(millis);

            // Define timestamps (Explorer normalmente mostra LastWriteTime como "Data de modificação")
            // Atributos: usar NORMAL para arquivo comum
            long attrs = FileAttributes.FILE_ATTRIBUTE_NORMAL.getValue();

            FileBasicInformation info = new FileBasicInformation(
                    ft, // creationTime
                    ft, // lastAccessTime
                    ft, // lastWriteTime  <-- este é o "Data de modificação"
                    ft, // changeTime
                    attrs
            );

            share.setFileInformation(remoteFilePath, info);

            log.info("SMB timestamp OK: {} -> {}", remoteFilePath, millis);

        } catch (Exception e) {
            // Se aqui falhar, normalmente é permissão (write attributes) ou política do servidor
            log.error("SMB timestamp FAIL: {} - {}", remoteFilePath, e.getMessage(), e);
            throw new RuntimeException("Falha ao preservar data/hora no SMB: " + e.getMessage(), e);
        }
    }

    private static void validarInputs(Path arquivoLocal, String servidor, String compartilhamento, String usuario) {
        if (arquivoLocal == null || !Files.exists(arquivoLocal)) {
            throw new IllegalArgumentException("Arquivo local não existe para copiar.");
        }
        if (servidor == null || servidor.isBlank()) {
            throw new IllegalArgumentException("Servidor SMB obrigatório.");
        }
        if (compartilhamento == null || compartilhamento.isBlank()) {
            throw new IllegalArgumentException("Compartilhamento SMB obrigatório.");
        }
        if (usuario == null || usuario.isBlank()) {
            throw new IllegalArgumentException("Usuário SMB obrigatório.");
        }
    }

    private static String normalizarSubpasta(String subpasta) {
        if (subpasta == null) return "";
        String s = subpasta.trim();
        if (s.isEmpty()) return "";

        s = s.replace("/", "\\");
        while (s.startsWith("\\")) s = s.substring(1);
        while (s.endsWith("\\")) s = s.substring(0, s.length() - 1);

        return s;
    }

    private static String joinRemote(String dir, String fileName) {
        if (dir == null || dir.isBlank()) return fileName;
        return dir + "\\" + fileName;
    }

    private static void ensureDirectories(DiskShare share, String remoteDir) {
        if (remoteDir == null || remoteDir.isBlank()) return;

        String[] parts = remoteDir.split("\\\\+");
        String current = "";
        for (String p : parts) {
            if (p == null || p.isBlank()) continue;
            current = current.isEmpty() ? p : current + "\\" + p;
            if (!share.folderExists(current)) {
                share.mkdir(current);
            }
        }
    }
}
