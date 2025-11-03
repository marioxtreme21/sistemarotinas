package sistema.rotinas.primefaces.dto;

public class ResultadoProcessamentoRelatorio {

    private int inseridos;
    private int duplicados;

    public ResultadoProcessamentoRelatorio() {
    }

    public ResultadoProcessamentoRelatorio(int inseridos, int duplicados) {
        this.inseridos = inseridos;
        this.duplicados = duplicados;
    }

    public int getInseridos() {
        return inseridos;
    }

    public void setInseridos(int inseridos) {
        this.inseridos = inseridos;
    }

    public int getDuplicados() {
        return duplicados;
    }

    public void setDuplicados(int duplicados) {
        this.duplicados = duplicados;
    }
}
