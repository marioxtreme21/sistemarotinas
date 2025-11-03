// src/main/java/sistema/ecommerce/primefaces/dto/InventoryUpdateRunResult.java
package sistema.rotinas.primefaces.dto;

import java.time.LocalDateTime;

public class InventoryUpdateRunResult {

    private Long lojaId;
    private String lojaNome;
    private String warehouse;
    private String politicaComercial; // ✅ novo campo

    private LocalDateTime inicio;
    private LocalDateTime fim;

    private int qtdConsultados;
    private int qtdEnviadosOk;
    private int qtdFalhaEnvio;
    private int qtdReprocessadosOk;
    private int qtdReprocessadosFalha;
    private int qtdProcessadosTotal;

    private String observacoes;

    // Getters e Setters
    public Long getLojaId() {
        return lojaId;
    }

    public void setLojaId(Long lojaId) {
        this.lojaId = lojaId;
    }

    public String getLojaNome() {
        return lojaNome;
    }

    public void setLojaNome(String lojaNome) {
        this.lojaNome = lojaNome;
    }

    public String getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(String warehouse) {
        this.warehouse = warehouse;
    }

    public String getPoliticaComercial() {
        return politicaComercial;
    }

    public void setPoliticaComercial(String politicaComercial) {
        this.politicaComercial = politicaComercial;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public void setInicio(LocalDateTime inicio) {
        this.inicio = inicio;
    }

    public LocalDateTime getFim() {
        return fim;
    }

    public void setFim(LocalDateTime fim) {
        this.fim = fim;
    }

    public int getQtdConsultados() {
        return qtdConsultados;
    }

    public void setQtdConsultados(int qtdConsultados) {
        this.qtdConsultados = qtdConsultados;
    }

    public int getQtdEnviadosOk() {
        return qtdEnviadosOk;
    }

    public void setQtdEnviadosOk(int qtdEnviadosOk) {
        this.qtdEnviadosOk = qtdEnviadosOk;
    }

    public int getQtdFalhaEnvio() {
        return qtdFalhaEnvio;
    }

    public void setQtdFalhaEnvio(int qtdFalhaEnvio) {
        this.qtdFalhaEnvio = qtdFalhaEnvio;
    }

    public int getQtdReprocessadosOk() {
        return qtdReprocessadosOk;
    }

    public void setQtdReprocessadosOk(int qtdReprocessadosOk) {
        this.qtdReprocessadosOk = qtdReprocessadosOk;
    }

    public int getQtdReprocessadosFalha() {
        return qtdReprocessadosFalha;
    }

    public void setQtdReprocessadosFalha(int qtdReprocessadosFalha) {
        this.qtdReprocessadosFalha = qtdReprocessadosFalha;
    }

    public int getQtdProcessadosTotal() {
        return qtdProcessadosTotal;
    }

    public void setQtdProcessadosTotal(int qtdProcessadosTotal) {
        this.qtdProcessadosTotal = qtdProcessadosTotal;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}
