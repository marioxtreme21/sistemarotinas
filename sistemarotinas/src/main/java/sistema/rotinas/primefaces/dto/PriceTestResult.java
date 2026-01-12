package sistema.rotinas.primefaces.dto;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PriceTestResult {

	private boolean sftpOk;
	private boolean downloadOk;

	private boolean fsOk;
	private boolean smbOk;

	private String arquivoRemoto;
	private Path arquivoLocal;

	// ✅ NOVO: para validação de “arquivo atualizado”
	private LocalDateTime lastModifiedRemoto;
	private Boolean arquivoAtualizado; // true/false/null (não foi possível validar)

	// =========================
	// ✅ MSG (m1) - resultado estruturado (NÃO quebra compatibilidade)
	// =========================
	// Valores sugeridos: OK | FALHOU | PULADO | DESATIVADO | INDEFINIDO
	private String msgStatus;
	private String msgOrigem;  // caminho local do .m1
	private String msgDestino; // \\server\share\subpasta
	private String msgDetalhe; // motivo/erro curto

	private final List<String> mensagens = new ArrayList<>();

	public boolean isSftpOk() {
		return sftpOk;
	}

	public void setSftpOk(boolean sftpOk) {
		this.sftpOk = sftpOk;
	}

	public boolean isDownloadOk() {
		return downloadOk;
	}

	public void setDownloadOk(boolean downloadOk) {
		this.downloadOk = downloadOk;
	}

	public boolean isFsOk() {
		return fsOk;
	}

	public void setFsOk(boolean fsOk) {
		this.fsOk = fsOk;
	}

	public boolean isSmbOk() {
		return smbOk;
	}

	public void setSmbOk(boolean smbOk) {
		this.smbOk = smbOk;
	}

	public String getArquivoRemoto() {
		return arquivoRemoto;
	}

	public void setArquivoRemoto(String arquivoRemoto) {
		this.arquivoRemoto = arquivoRemoto;
	}

	public Path getArquivoLocal() {
		return arquivoLocal;
	}

	public void setArquivoLocal(Path arquivoLocal) {
		this.arquivoLocal = arquivoLocal;
	}

	public LocalDateTime getLastModifiedRemoto() {
		return lastModifiedRemoto;
	}

	public void setLastModifiedRemoto(LocalDateTime lastModifiedRemoto) {
		this.lastModifiedRemoto = lastModifiedRemoto;
	}

	public Boolean getArquivoAtualizado() {
		return arquivoAtualizado;
	}

	public void setArquivoAtualizado(Boolean arquivoAtualizado) {
		this.arquivoAtualizado = arquivoAtualizado;
	}

	public List<String> getMensagens() {
		return mensagens;
	}

	public void addMsg(String msg) {
		if (msg != null && !msg.isBlank())
			mensagens.add(msg);
	}

	// =========================
	// ✅ MSG getters/setters
	// =========================
	public String getMsgStatus() {
		return msgStatus;
	}

	public void setMsgStatus(String msgStatus) {
		this.msgStatus = msgStatus;
	}

	public String getMsgOrigem() {
		return msgOrigem;
	}

	public void setMsgOrigem(String msgOrigem) {
		this.msgOrigem = msgOrigem;
	}

	public String getMsgDestino() {
		return msgDestino;
	}

	public void setMsgDestino(String msgDestino) {
		this.msgDestino = msgDestino;
	}

	public String getMsgDetalhe() {
		return msgDetalhe;
	}

	public void setMsgDetalhe(String msgDetalhe) {
		this.msgDetalhe = msgDetalhe;
	}

	// conveniência (não atrapalha ninguém)
	public boolean isMsgOk() {
		return "OK".equalsIgnoreCase(msgStatus) || "DESATIVADO".equalsIgnoreCase(msgStatus);
	}
}