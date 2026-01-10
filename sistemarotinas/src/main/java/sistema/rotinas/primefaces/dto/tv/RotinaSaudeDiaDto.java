package sistema.rotinas.primefaces.dto.tv;

import java.util.ArrayList;
import java.util.List;

public class RotinaSaudeDiaDto {

	private String data; // "07/01/2026"
	private String timezone; // "America/Bahia"

	private SaudeRotinaItem price;
	private SaudeRotinaItem mgv;

	public RotinaSaudeDiaDto() {
	}

	public RotinaSaudeDiaDto(String data, String timezone, SaudeRotinaItem price, SaudeRotinaItem mgv) {
		this.data = data;
		this.timezone = timezone;
		this.price = price;
		this.mgv = mgv;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public String getTimezone() {
		return timezone;
	}

	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}

	public SaudeRotinaItem getPrice() {
		return price;
	}

	public void setPrice(SaudeRotinaItem price) {
		this.price = price;
	}

	public SaudeRotinaItem getMgv() {
		return mgv;
	}

	public void setMgv(SaudeRotinaItem mgv) {
		this.mgv = mgv;
	}

	// =========================================================
	public static class SaudeRotinaItem {

		private String tipo; // "PRICE" | "MGV"

		private int totalLojasComConfig;
		private int ok;
		private int parcial;
		private int falha;
		private int semExecucaoHoje;

		private List<PendenciaLoja> pendencias = new ArrayList<>();

		public SaudeRotinaItem() {
		}

		public SaudeRotinaItem(String tipo) {
			this.tipo = tipo;
		}

		public String getTipo() {
			return tipo;
		}

		public void setTipo(String tipo) {
			this.tipo = tipo;
		}

		public int getTotalLojasComConfig() {
			return totalLojasComConfig;
		}

		public void setTotalLojasComConfig(int totalLojasComConfig) {
			this.totalLojasComConfig = totalLojasComConfig;
		}

		public int getOk() {
			return ok;
		}

		public void setOk(int ok) {
			this.ok = ok;
		}

		public int getParcial() {
			return parcial;
		}

		public void setParcial(int parcial) {
			this.parcial = parcial;
		}

		public int getFalha() {
			return falha;
		}

		public void setFalha(int falha) {
			this.falha = falha;
		}

		public int getSemExecucaoHoje() {
			return semExecucaoHoje;
		}

		public void setSemExecucaoHoje(int semExecucaoHoje) {
			this.semExecucaoHoje = semExecucaoHoje;
		}

		public List<PendenciaLoja> getPendencias() {
			return pendencias;
		}

		public void setPendencias(List<PendenciaLoja> pendencias) {
			this.pendencias = (pendencias == null ? new ArrayList<>() : pendencias);
		}
	}

	// =========================================================
	public static class PendenciaLoja {

		private String codLojaConsinco; // Loja.codLojaRms
		private String loja; // nome
		private String status; // FALHA | FALHA_PARCIAL | SEM_EXECUCAO | ...
		private String ultimaExecucao; // "06/01 16:21" ou "-"
		private Long execucaoId; // opcional

		public PendenciaLoja() {
		}

		public PendenciaLoja(String codLojaConsinco, String loja, String status, String ultimaExecucao,
				Long execucaoId) {
			this.codLojaConsinco = codLojaConsinco;
			this.loja = loja;
			this.status = status;
			this.ultimaExecucao = ultimaExecucao;
			this.execucaoId = execucaoId;
		}

		public String getCodLojaConsinco() {
			return codLojaConsinco;
		}

		public void setCodLojaConsinco(String codLojaConsinco) {
			this.codLojaConsinco = codLojaConsinco;
		}

		public String getLoja() {
			return loja;
		}

		public void setLoja(String loja) {
			this.loja = loja;
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}

		public String getUltimaExecucao() {
			return ultimaExecucao;
		}

		public void setUltimaExecucao(String ultimaExecucao) {
			this.ultimaExecucao = ultimaExecucao;
		}

		public Long getExecucaoId() {
			return execucaoId;
		}

		public void setExecucaoId(Long execucaoId) {
			this.execucaoId = execucaoId;
		}
	}
}