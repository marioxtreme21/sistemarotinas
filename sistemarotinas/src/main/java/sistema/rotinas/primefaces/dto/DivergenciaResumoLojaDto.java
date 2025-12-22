package sistema.rotinas.primefaces.dto;

public class DivergenciaResumoLojaDto {

    private final String nomeLoja;
    private final String codLojaRms;
    private final int totalDivergencias;

    public DivergenciaResumoLojaDto(String nomeLoja, String codLojaRms, int totalDivergencias) {
        this.nomeLoja = nomeLoja;
        this.codLojaRms = codLojaRms;
        this.totalDivergencias = totalDivergencias;
    }

    public String getNomeLoja() {
        return nomeLoja;
    }

    public String getCodLojaRms() {
        return codLojaRms;
    }

    public int getTotalDivergencias() {
        return totalDivergencias;
    }
}
