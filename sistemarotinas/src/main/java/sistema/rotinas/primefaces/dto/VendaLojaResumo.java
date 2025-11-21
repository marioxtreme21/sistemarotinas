package sistema.rotinas.primefaces.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class VendaLojaResumo implements Serializable {

    private static final long serialVersionUID = 1L;

    // numero_loja na base ECONECT
    private String codLojaEconect;

    // Ex.: "102 - Stella Maris"
    private String descricaoLoja;

    // Compatibilidade com a tela (usa o mesmo valor de descricaoLoja)
    private String nomeLoja;

    // COUNT(numero_cupom)
    private long numeroClientes;

    // Compatibilidade com a tela: alias para numeroClientes
    private long quantidadeCupons;

    // SUM(total_liquido)
    private BigDecimal totalVenda;

    // Compatibilidade com a tela: alias para totalVenda
    private BigDecimal valorTotal;

    // totalVenda / numeroClientes
    private BigDecimal ticketMedio;

    // ================= Getters / Setters =================

    public String getCodLojaEconect() {
        return codLojaEconect;
    }

    public void setCodLojaEconect(String codLojaEconect) {
        this.codLojaEconect = codLojaEconect;
    }

    public String getDescricaoLoja() {
        return descricaoLoja;
    }

    public void setDescricaoLoja(String descricaoLoja) {
        this.descricaoLoja = descricaoLoja;
    }

    /**
     * Compatibilidade com a página JSF:
     * se nomeLoja não estiver preenchido, retorna descricaoLoja.
     */
    public String getNomeLoja() {
        if (nomeLoja != null && !nomeLoja.isBlank()) {
            return nomeLoja;
        }
        return descricaoLoja;
    }

    public void setNomeLoja(String nomeLoja) {
        this.nomeLoja = nomeLoja;
    }

    public long getNumeroClientes() {
        return numeroClientes;
    }

    public void setNumeroClientes(long numeroClientes) {
        this.numeroClientes = numeroClientes;
        // mantém alias em sincronia
        this.quantidadeCupons = numeroClientes;
    }

    /**
     * Compatibilidade com a página JSF:
     * se quantidadeCupons for zero, usa numeroClientes.
     */
    public long getQuantidadeCupons() {
        if (quantidadeCupons > 0) {
            return quantidadeCupons;
        }
        return numeroClientes;
    }

    public void setQuantidadeCupons(long quantidadeCupons) {
        this.quantidadeCupons = quantidadeCupons;
        // mantém campo principal em sincronia
        this.numeroClientes = quantidadeCupons;
    }

    public BigDecimal getTotalVenda() {
        return totalVenda;
    }

    public void setTotalVenda(BigDecimal totalVenda) {
        this.totalVenda = totalVenda;
        // mantém alias em sincronia
        this.valorTotal = totalVenda;
    }

    /**
     * Compatibilidade com a página JSF:
     * se valorTotal não estiver preenchido, retorna totalVenda.
     */
    public BigDecimal getValorTotal() {
        if (valorTotal != null) {
            return valorTotal;
        }
        return totalVenda;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
        // mantém campo principal em sincronia
        this.totalVenda = valorTotal;
    }

    public BigDecimal getTicketMedio() {
        return ticketMedio;
    }

    public void setTicketMedio(BigDecimal ticketMedio) {
        this.ticketMedio = ticketMedio;
    }
}
