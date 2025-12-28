package sistema.rotinas.primefaces.dto;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PriceTestResult {

    private boolean sftpOk;
    private boolean downloadOk;

    private boolean fsOk;
    private boolean smbOk;

    private String arquivoRemoto;
    private Path arquivoLocal;

    private final List<String> mensagens = new ArrayList<>();

    public boolean isSftpOk() { return sftpOk; }
    public void setSftpOk(boolean sftpOk) { this.sftpOk = sftpOk; }

    public boolean isDownloadOk() { return downloadOk; }
    public void setDownloadOk(boolean downloadOk) { this.downloadOk = downloadOk; }

    public boolean isFsOk() { return fsOk; }
    public void setFsOk(boolean fsOk) { this.fsOk = fsOk; }

    public boolean isSmbOk() { return smbOk; }
    public void setSmbOk(boolean smbOk) { this.smbOk = smbOk; }

    public String getArquivoRemoto() { return arquivoRemoto; }
    public void setArquivoRemoto(String arquivoRemoto) { this.arquivoRemoto = arquivoRemoto; }

    public Path getArquivoLocal() { return arquivoLocal; }
    public void setArquivoLocal(Path arquivoLocal) { this.arquivoLocal = arquivoLocal; }

    public List<String> getMensagens() { return mensagens; }

    public void addMsg(String msg) {
        if (msg != null && !msg.isBlank()) mensagens.add(msg);
    }
}
