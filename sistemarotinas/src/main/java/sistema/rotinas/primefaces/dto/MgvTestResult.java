package sistema.rotinas.primefaces.dto;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MgvTestResult {

    private boolean sftpOk;
    private boolean downloadOk;

    private boolean fsOk;
    private boolean smbOk;

    // ✅ MGV: múltiplos arquivos
    private List<String> arquivosRemotos = new ArrayList<>();
    private List<Path> arquivosLocais = new ArrayList<>();

    /**
     * ✅ Info por arquivo (nome + lastModified + se é do dia)
     * Isso é o que você precisa mostrar no e-mail.
     */
    private List<ArquivoInfo> arquivosInfo = new ArrayList<>();

    private final List<String> mensagens = new ArrayList<>();

    // ======================
    // Getters/Setters
    // ======================
    public boolean isSftpOk() { return sftpOk; }
    public void setSftpOk(boolean sftpOk) { this.sftpOk = sftpOk; }

    public boolean isDownloadOk() { return downloadOk; }
    public void setDownloadOk(boolean downloadOk) { this.downloadOk = downloadOk; }

    public boolean isFsOk() { return fsOk; }
    public void setFsOk(boolean fsOk) { this.fsOk = fsOk; }

    public boolean isSmbOk() { return smbOk; }
    public void setSmbOk(boolean smbOk) { this.smbOk = smbOk; }

    public List<String> getArquivosRemotos() { return arquivosRemotos; }
    public void setArquivosRemotos(List<String> arquivosRemotos) {
        this.arquivosRemotos = (arquivosRemotos == null ? new ArrayList<>() : arquivosRemotos);
    }

    public List<Path> getArquivosLocais() { return arquivosLocais; }
    public void setArquivosLocais(List<Path> arquivosLocais) {
        this.arquivosLocais = (arquivosLocais == null ? new ArrayList<>() : arquivosLocais);
    }

    public List<ArquivoInfo> getArquivosInfo() { return arquivosInfo; }
    public void setArquivosInfo(List<ArquivoInfo> arquivosInfo) {
        this.arquivosInfo = (arquivosInfo == null ? new ArrayList<>() : arquivosInfo);
    }

    public List<String> getMensagens() { return mensagens; }

    public void addMsg(String msg) {
        if (msg != null && !msg.isBlank()) mensagens.add(msg);
    }

    public void addArquivoInfo(String nomeArquivo, LocalDateTime lastModified, Boolean atualizado) {
        this.arquivosInfo.add(new ArquivoInfo(nomeArquivo, lastModified, atualizado));
    }

    // ======================
    // Inner class
    // ======================
    public static class ArquivoInfo {
        private String nomeArquivo;
        private LocalDateTime lastModified;
        private Boolean atualizado; // true/false/null

        public ArquivoInfo() {}

        public ArquivoInfo(String nomeArquivo, LocalDateTime lastModified, Boolean atualizado) {
            this.nomeArquivo = nomeArquivo;
            this.lastModified = lastModified;
            this.atualizado = atualizado;
        }

        public String getNomeArquivo() { return nomeArquivo; }
        public void setNomeArquivo(String nomeArquivo) { this.nomeArquivo = nomeArquivo; }

        public LocalDateTime getLastModified() { return lastModified; }
        public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }

        public Boolean getAtualizado() { return atualizado; }
        public void setAtualizado(Boolean atualizado) { this.atualizado = atualizado; }
    }
}