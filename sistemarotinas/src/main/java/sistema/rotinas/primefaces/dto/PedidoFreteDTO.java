// src/main/java/sistema/ecommerce/primefaces/dto/PedidoFreteDTO.java
package sistema.rotinas.primefaces.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PedidoFreteDTO {
    private LocalDate dataVenda;
    private Integer codLoja;
    private Integer numeroPdv;
    private Integer numCupom;
    private Integer numPedido;
    private String nomeCliente;
    private BigDecimal valorPedido;
    private BigDecimal valorFrete;
    private Long numeroNfce;   // mov_nfc.num_lot
    private String qrcode;
    private String chaveAcesso;
	public LocalDate getDataVenda() {
		return dataVenda;
	}
	public void setDataVenda(LocalDate dataVenda) {
		this.dataVenda = dataVenda;
	}
	public Integer getCodLoja() {
		return codLoja;
	}
	public void setCodLoja(Integer codLoja) {
		this.codLoja = codLoja;
	}
	
	
	public Integer getNumeroPdv() {
		return numeroPdv;
	}
	public void setNumeroPdv(Integer numeroPdv) {
		this.numeroPdv = numeroPdv;
	}
	public Integer getNumCupom() {
		return numCupom;
	}
	public void setNumCupom(Integer numCupom) {
		this.numCupom = numCupom;
	}
	public Integer getNumPedido() {
		return numPedido;
	}
	public void setNumPedido(Integer numPedido) {
		this.numPedido = numPedido;
	}
	public String getNomeCliente() {
		return nomeCliente;
	}
	public void setNomeCliente(String nomeCliente) {
		this.nomeCliente = nomeCliente;
	}
	public BigDecimal getValorPedido() {
		return valorPedido;
	}
	public void setValorPedido(BigDecimal valorPedido) {
		this.valorPedido = valorPedido;
	}
	public BigDecimal getValorFrete() {
		return valorFrete;
	}
	public void setValorFrete(BigDecimal valorFrete) {
		this.valorFrete = valorFrete;
	}
	public Long getNumeroNfce() {
		return numeroNfce;
	}
	public void setNumeroNfce(Long numeroNfce) {
		this.numeroNfce = numeroNfce;
	}
	public String getQrcode() {
		return qrcode;
	}
	public void setQrcode(String qrcode) {
		this.qrcode = qrcode;
	}
	public String getChaveAcesso() {
		return chaveAcesso;
	}
	public void setChaveAcesso(String chaveAcesso) {
		this.chaveAcesso = chaveAcesso;
	}

    
    
	
    
}
