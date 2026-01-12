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

	// ✅ NOVO: status geral estruturado (OK | FALHA_PARCIAL | FALHOU | INDEFINIDO)
	private String statusGeral;
	private String detalheGeral;

	// ✅ MGV: múltiplos arquivos
	private List<String> arquivosRemotos = new ArrayList<>();
	private List<Path> arquivosLocais = new ArrayList<>();

	/**
	 * ✅ Info por arquivo (nome + lastModified + se é do dia) + NOVO: origem/destino
	 * + resultado FS/SMB por arquivo
	 */
	private List<ArquivoInfo> arquivosInfo = new ArrayList<>();

	private final List<String> mensagens = new ArrayList<>();

	// ======================
	// Getters/Setters
	// ======================
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

	public String getStatusGeral() {
		return statusGeral;
	}

	public void setStatusGeral(String statusGeral) {
		this.statusGeral = statusGeral;
	}

	public String getDetalheGeral() {
		return detalheGeral;
	}

	public void setDetalheGeral(String detalheGeral) {
		this.detalheGeral = detalheGeral;
	}

	public List<String> getArquivosRemotos() {
		return arquivosRemotos;
	}

	public void setArquivosRemotos(List<String> arquivosRemotos) {
		this.arquivosRemotos = (arquivosRemotos == null ? new ArrayList<>() : arquivosRemotos);
	}

	public List<Path> getArquivosLocais() {
		return arquivosLocais;
	}

	public void setArquivosLocais(List<Path> arquivosLocais) {
		this.arquivosLocais = (arquivosLocais == null ? new ArrayList<>() : arquivosLocais);
	}

	public List<ArquivoInfo> getArquivosInfo() {
		return arquivosInfo;
	}

	public void setArquivosInfo(List<ArquivoInfo> arquivosInfo) {
		this.arquivosInfo = (arquivosInfo == null ? new ArrayList<>() : arquivosInfo);
	}

	public List<String> getMensagens() {
		return mensagens;
	}

	public void addMsg(String msg) {
		if (msg != null && !msg.isBlank())
			mensagens.add(msg);
	}

	// ======================
	// Helpers de arquivo
	// ======================

	public void addArquivoInfo(String nomeArquivo, LocalDateTime lastModified, Boolean atualizado) {
		this.arquivosInfo.add(new ArquivoInfo(nomeArquivo, lastModified, atualizado));
	}

	public ArquivoInfo getOrCreateArquivoInfo(String nomeArquivo) {
		if (nomeArquivo == null)
			return null;
		for (ArquivoInfo a : arquivosInfo) {
			if (a != null && nomeArquivo.equalsIgnoreCase(a.getNomeArquivo()))
				return a;
		}
		ArquivoInfo novo = new ArquivoInfo();
		novo.setNomeArquivo(nomeArquivo);
		arquivosInfo.add(novo);
		return novo;
	}

	// ======================
	// Inner class
	// ======================
	public static class ArquivoInfo {
		private String nomeArquivo;
		private LocalDateTime lastModified;
		private Boolean atualizado; // true/false/null

		// ✅ NOVO (estruturado, sem quebrar compatibilidade)
		private String origemRemota; // ex: /dir/arquivo.txt
		private String destinoLocal; // ex: /uploads/.../arquivo.txt
		private String fsStatus; // OK | FALHOU | PULADO | INDEFINIDO
		private String smbStatus; // OK | FALHOU | PULADO | INDEFINIDO
		private String detalhe; // erro curto / motivo

		public ArquivoInfo() {
		}

		public ArquivoInfo(String nomeArquivo, LocalDateTime lastModified, Boolean atualizado) {
			this.nomeArquivo = nomeArquivo;
			this.lastModified = lastModified;
			this.atualizado = atualizado;
		}

		public String getNomeArquivo() {
			return nomeArquivo;
		}

		public void setNomeArquivo(String nomeArquivo) {
			this.nomeArquivo = nomeArquivo;
		}

		public LocalDateTime getLastModified() {
			return lastModified;
		}

		public void setLastModified(LocalDateTime lastModified) {
			this.lastModified = lastModified;
		}

		public Boolean getAtualizado() {
			return atualizado;
		}

		public void setAtualizado(Boolean atualizado) {
			this.atualizado = atualizado;
		}

		public String getOrigemRemota() {
			return origemRemota;
		}

		public void setOrigemRemota(String origemRemota) {
			this.origemRemota = origemRemota;
		}

		public String getDestinoLocal() {
			return destinoLocal;
		}

		public void setDestinoLocal(String destinoLocal) {
			this.destinoLocal = destinoLocal;
		}

		public String getFsStatus() {
			return fsStatus;
		}

		public void setFsStatus(String fsStatus) {
			this.fsStatus = fsStatus;
		}

		public String getSmbStatus() {
			return smbStatus;
		}

		public void setSmbStatus(String smbStatus) {
			this.smbStatus = smbStatus;
		}

		public String getDetalhe() {
			return detalhe;
		}

		public void setDetalhe(String detalhe) {
			this.detalhe = detalhe;
		}
	}
}