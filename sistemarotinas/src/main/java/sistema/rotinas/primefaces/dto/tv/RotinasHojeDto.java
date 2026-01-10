package sistema.rotinas.primefaces.dto.tv;

public class RotinasHojeDto {

	private String data; // "05/01/2026"
	private String timezone; // "America/Bahia"

	private RotinaHojeItem price;
	private RotinaHojeItem mgv;

	public RotinasHojeDto() {
	}

	public RotinasHojeDto(String data, String timezone, RotinaHojeItem price, RotinaHojeItem mgv) {
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

	public RotinaHojeItem getPrice() {
		return price;
	}

	public void setPrice(RotinaHojeItem price) {
		this.price = price;
	}

	public RotinaHojeItem getMgv() {
		return mgv;
	}

	public void setMgv(RotinaHojeItem mgv) {
		this.mgv = mgv;
	}

	// -------------------------
	public static class RotinaHojeItem {
		private String tipo; // "PRICE" | "MGV"
		private String status; // "SUCESSO" | "FALHA_PARCIAL" | "FALHA" | "EM_ANDAMENTO" | "SEM_EXECUCAO"
		private String ultimaExecucao; // "16:21" (ou "-" se não teve)
		private Long execucaoId; // opcional

		// ✅ NOVO: escopo da execução (quantidade de lojas)
		private Integer lojasTotal;
		private Integer lojasOk;
		private Integer lojasParcial;
		private Integer lojasFalha;

		public RotinaHojeItem() {
		}

		// ✅ Mantido (compatível com seu código atual)
		public RotinaHojeItem(String tipo, String status, String ultimaExecucao, Long execucaoId) {
			this.tipo = tipo;
			this.status = status;
			this.ultimaExecucao = ultimaExecucao;
			this.execucaoId = execucaoId;
		}

		// ✅ Novo construtor opcional
		public RotinaHojeItem(String tipo, String status, String ultimaExecucao, Long execucaoId, Integer lojasTotal,
				Integer lojasOk, Integer lojasParcial, Integer lojasFalha) {
			this.tipo = tipo;
			this.status = status;
			this.ultimaExecucao = ultimaExecucao;
			this.execucaoId = execucaoId;
			this.lojasTotal = lojasTotal;
			this.lojasOk = lojasOk;
			this.lojasParcial = lojasParcial;
			this.lojasFalha = lojasFalha;
		}

		public String getTipo() {
			return tipo;
		}

		public void setTipo(String tipo) {
			this.tipo = tipo;
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

		public Integer getLojasTotal() {
			return lojasTotal;
		}

		public void setLojasTotal(Integer lojasTotal) {
			this.lojasTotal = lojasTotal;
		}

		public Integer getLojasOk() {
			return lojasOk;
		}

		public void setLojasOk(Integer lojasOk) {
			this.lojasOk = lojasOk;
		}

		public Integer getLojasParcial() {
			return lojasParcial;
		}

		public void setLojasParcial(Integer lojasParcial) {
			this.lojasParcial = lojasParcial;
		}

		public Integer getLojasFalha() {
			return lojasFalha;
		}

		public void setLojasFalha(Integer lojasFalha) {
			this.lojasFalha = lojasFalha;
		}
	}
}