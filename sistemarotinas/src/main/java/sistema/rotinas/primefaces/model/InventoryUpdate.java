// src/main/java/sistema/ecommerce/primefaces/model/InventoryUpdate.java
package sistema.rotinas.primefaces.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.*;

@Entity
@Table(name = "inventory_update")
public class InventoryUpdate implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "inventory_update_id")
	private Long inventoryUpdateId;

	@Column(name = "codigo", nullable = false)
	private Integer codigo;

	@Column(name = "descricao")
	private String descricao;

	@Column(name = "quantidade", nullable = false)
	private Integer quantidade;

	@Column(name = "data_ultimo_envio")
	private LocalDateTime dataUltimoEnvio;

	@Column(name = "status_envio_vtex")
	private Boolean statusEnvioVtex;

	@Column(name = "reprocessamento")
	private Boolean reprocessamento;

	@ManyToOne
	@JoinColumn(name = "loja_id", referencedColumnName = "loja_id")
	private Loja loja;

	// Getters/Setters
	public Long getInventoryUpdateId() {
		return inventoryUpdateId;
	}

	public void setInventoryUpdateId(Long id) {
		this.inventoryUpdateId = id;
	}

	public Integer getCodigo() {
		return codigo;
	}

	public void setCodigo(Integer codigo) {
		this.codigo = codigo;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}

	public Integer getQuantidade() {
		return quantidade;
	}

	public void setQuantidade(Integer quantidade) {
		this.quantidade = quantidade;
	}

	public LocalDateTime getDataUltimoEnvio() {
		return dataUltimoEnvio;
	}

	public void setDataUltimoEnvio(LocalDateTime dataUltimoEnvio) {
		this.dataUltimoEnvio = dataUltimoEnvio;
	}

	public Boolean getStatusEnvioVtex() {
		return statusEnvioVtex;
	}

	public void setStatusEnvioVtex(Boolean statusEnvioVtex) {
		this.statusEnvioVtex = statusEnvioVtex;
	}

	public Boolean getReprocessamento() {
		return reprocessamento;
	}

	public void setReprocessamento(Boolean reprocessamento) {
		this.reprocessamento = reprocessamento;
	}

	public Loja getLoja() {
		return loja;
	}

	public void setLoja(Loja loja) {
		this.loja = loja;
	}

	@Override
	public int hashCode() {
		return Objects.hash(inventoryUpdateId);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof InventoryUpdate))
			return false;
		InventoryUpdate other = (InventoryUpdate) o;
		return Objects.equals(inventoryUpdateId, other.inventoryUpdateId);
	}

	@Override
	public String toString() {
		return "InventoryUpdate{" + "inventoryUpdateId=" + inventoryUpdateId + ", codigo=" + codigo + ", descricao='"
				+ descricao + '\'' + ", quantidade=" + quantidade + ", dataUltimoEnvio=" + dataUltimoEnvio
				+ ", statusEnvioVtex=" + statusEnvioVtex + ", reprocessamento=" + reprocessamento + ", loja="
				+ (loja != null ? loja.getLojaId() : null) + '}';
	}
}
