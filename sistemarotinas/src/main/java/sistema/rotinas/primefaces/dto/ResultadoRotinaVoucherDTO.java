package sistema.rotinas.primefaces.dto;

import java.io.Serializable;

public class ResultadoRotinaVoucherDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private int totalLidosEconect;
    private int totalClientesNaoEncontrados;
    private int totalClientesMarcadosSituacao99;

    private int totalMovInseridos;
    private int totalMovAtualizados;

    // ✅ usado na tela
    private int totalMovJaExistentes;

    // opcional (já apareceu em log)
    private int totalMovJaExistentesSemAlteracao;

    public int getTotalLidosEconect() {
        return totalLidosEconect;
    }

    public void setTotalLidosEconect(int totalLidosEconect) {
        this.totalLidosEconect = totalLidosEconect;
    }

    public int getTotalClientesNaoEncontrados() {
        return totalClientesNaoEncontrados;
    }

    public void setTotalClientesNaoEncontrados(int totalClientesNaoEncontrados) {
        this.totalClientesNaoEncontrados = totalClientesNaoEncontrados;
    }

    public int getTotalClientesMarcadosSituacao99() {
        return totalClientesMarcadosSituacao99;
    }

    public void setTotalClientesMarcadosSituacao99(int totalClientesMarcadosSituacao99) {
        this.totalClientesMarcadosSituacao99 = totalClientesMarcadosSituacao99;
    }

    public int getTotalMovInseridos() {
        return totalMovInseridos;
    }

    public void setTotalMovInseridos(int totalMovInseridos) {
        this.totalMovInseridos = totalMovInseridos;
    }

    public int getTotalMovAtualizados() {
        return totalMovAtualizados;
    }

    public void setTotalMovAtualizados(int totalMovAtualizados) {
        this.totalMovAtualizados = totalMovAtualizados;
    }

    public int getTotalMovJaExistentes() {
        return totalMovJaExistentes;
    }

    public void setTotalMovJaExistentes(int totalMovJaExistentes) {
        this.totalMovJaExistentes = totalMovJaExistentes;
    }

    public int getTotalMovJaExistentesSemAlteracao() {
        return totalMovJaExistentesSemAlteracao;
    }

    public void setTotalMovJaExistentesSemAlteracao(int totalMovJaExistentesSemAlteracao) {
        this.totalMovJaExistentesSemAlteracao = totalMovJaExistentesSemAlteracao;
    }
}
