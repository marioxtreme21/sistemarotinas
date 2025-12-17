package sistema.rotinas.primefaces.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class VoucherUsoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private LocalDateTime dataVenda;
    private LocalDateTime dataMovimento; // para o Oracle (DATA_MOVIMENTO)

    private Integer numeroLoja;
    private Integer numeroPdv;
    private Long numeroCupom;

    private BigDecimal valor;
    private Integer sequencia;

    private String cpfCnpj; // vindo do ECONECT (pode vir com/sem zeros)

    public LocalDateTime getDataVenda() {
        return dataVenda;
    }

    public void setDataVenda(LocalDateTime dataVenda) {
        this.dataVenda = dataVenda;
    }

    public LocalDateTime getDataMovimento() {
        return dataMovimento;
    }

    public void setDataMovimento(LocalDateTime dataMovimento) {
        this.dataMovimento = dataMovimento;
    }

    public Integer getNumeroLoja() {
        return numeroLoja;
    }

    public void setNumeroLoja(Integer numeroLoja) {
        this.numeroLoja = numeroLoja;
    }

    public Integer getNumeroPdv() {
        return numeroPdv;
    }

    public void setNumeroPdv(Integer numeroPdv) {
        this.numeroPdv = numeroPdv;
    }

    public Long getNumeroCupom() {
        return numeroCupom;
    }

    public void setNumeroCupom(Long numeroCupom) {
        this.numeroCupom = numeroCupom;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public Integer getSequencia() {
        return sequencia;
    }

    public void setSequencia(Integer sequencia) {
        this.sequencia = sequencia;
    }

    public String getCpfCnpj() {
        return cpfCnpj;
    }

    public void setCpfCnpj(String cpfCnpj) {
        this.cpfCnpj = cpfCnpj;
    }
}
