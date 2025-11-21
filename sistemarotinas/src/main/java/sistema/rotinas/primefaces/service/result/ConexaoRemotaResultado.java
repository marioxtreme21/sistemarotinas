package sistema.rotinas.primefaces.service.result;

import java.io.Serializable;

public class ConexaoRemotaResultado implements Serializable {
	private static final long serialVersionUID = 1L;

	private boolean sucesso;
	private String mensagem;

	public ConexaoRemotaResultado() {
	}

	public ConexaoRemotaResultado(boolean sucesso, String mensagem) {
		this.sucesso = sucesso;
		this.mensagem = mensagem;
	}

	public static ConexaoRemotaResultado ok(String msg) {
		return new ConexaoRemotaResultado(true, msg);
	}

	public static ConexaoRemotaResultado erro(String msg) {
		return new ConexaoRemotaResultado(false, msg);
	}

	public boolean isSucesso() {
		return sucesso;
	}

	public void setSucesso(boolean sucesso) {
		this.sucesso = sucesso;
	}

	public String getMensagem() {
		return mensagem;
	}

	public void setMensagem(String mensagem) {
		this.mensagem = mensagem;
	}
}
