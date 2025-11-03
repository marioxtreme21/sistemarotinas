package sistema.rotinas.primefaces.model;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "loja")
public class Loja implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "loja_id")
	private Long lojaId;

	@Column(name = "nome")
	private String nome;

	@Column(name = "cnpj")
	private String cnpj;

	@Column(name = "politica_comercial")
	private String politicaComercial;

	@Column(name = "cod_loja_econect")
	private String codLojaEconect;

	@Column(name = "cod_loja_rms")
	private String codLojaRms;

	@Column(name = "cod_loja_rms_dg")
	private String codLojaRmsDg;

	@Column(name = "codigo_empresa_sitef")
	private String codigoEmpresaSitef;

	@Column(name = "ecommerce_ativo")
	private Boolean ecommerceAtivo;

	@Column(name = "pick_and_pack_ativo")
	private Boolean pickAndPackAtivo;

	// ✅ Campos existentes
	@Column(name = "horario_price_update", length = 20)
	private String horarioPriceUpdate;

	@Column(name = "warehouse", length = 100)
	private String warehouse;

	// ✅ NOVOS CAMPOS (prioridade)
	@Column(name = "prioridade_envio_ativo")
	private Boolean prioridadeEnvioAtivo; // se true, participa da fila prioritária

	@Column(name = "prioridade_envio_ranking")
	private Integer prioridadeEnvioRanking; // menor número = mais prioritária (ex.: 1..8)

	// Getters e Setters
	public Long getLojaId() {
		return lojaId;
	}

	public void setLojaId(Long lojaId) {
		this.lojaId = lojaId;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getCnpj() {
		return cnpj;
	}

	public void setCnpj(String cnpj) {
		this.cnpj = cnpj;
	}

	public String getPoliticaComercial() {
		return politicaComercial;
	}

	public void setPoliticaComercial(String politicaComercial) {
		this.politicaComercial = politicaComercial;
	}

	public String getCodLojaEconect() {
		return codLojaEconect;
	}

	public void setCodLojaEconect(String codLojaEconect) {
		this.codLojaEconect = codLojaEconect;
	}

	public String getCodLojaRms() {
		return codLojaRms;
	}

	public void setCodLojaRms(String codLojaRms) {
		this.codLojaRms = codLojaRms;
	}

	public String getCodLojaRmsDg() {
		return codLojaRmsDg;
	}

	public void setCodLojaRmsDg(String codLojaRmsDg) {
		this.codLojaRmsDg = codLojaRmsDg;
	}

	public String getCodigoEmpresaSitef() {
		return codigoEmpresaSitef;
	}

	public void setCodigoEmpresaSitef(String codigoEmpresaSitef) {
		this.codigoEmpresaSitef = codigoEmpresaSitef;
	}

	public Boolean getEcommerceAtivo() {
		return ecommerceAtivo;
	}

	public void setEcommerceAtivo(Boolean ecommerceAtivo) {
		this.ecommerceAtivo = ecommerceAtivo;
	}

	public Boolean getPickAndPackAtivo() {
		return pickAndPackAtivo;
	}

	public void setPickAndPackAtivo(Boolean pickAndPackAtivo) {
		this.pickAndPackAtivo = pickAndPackAtivo;
	}

	public String getHorarioPriceUpdate() {
		return horarioPriceUpdate;
	}

	public void setHorarioPriceUpdate(String horarioPriceUpdate) {
		this.horarioPriceUpdate = horarioPriceUpdate;
	}

	public String getWarehouse() {
		return warehouse;
	}

	public void setWarehouse(String warehouse) {
		this.warehouse = warehouse;
	}

	public Boolean getPrioridadeEnvioAtivo() {
		return prioridadeEnvioAtivo;
	}

	public void setPrioridadeEnvioAtivo(Boolean prioridadeEnvioAtivo) {
		this.prioridadeEnvioAtivo = prioridadeEnvioAtivo;
	}

	public Integer getPrioridadeEnvioRanking() {
		return prioridadeEnvioRanking;
	}

	public void setPrioridadeEnvioRanking(Integer prioridadeEnvioRanking) {
		this.prioridadeEnvioRanking = prioridadeEnvioRanking;
	}

	@Override
	public int hashCode() {
		return Objects.hash(lojaId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if ((obj == null) || (getClass() != obj.getClass()))
			return false;
		Loja other = (Loja) obj;
		return Objects.equals(lojaId, other.lojaId);
	}

	@Override
	public String toString() {
		return "Loja [lojaId=" + lojaId + ", nome=" + nome + ", cnpj=" + cnpj + ", politicaComercial="
				+ politicaComercial + ", codLojaEconect=" + codLojaEconect + ", codLojaRms=" + codLojaRms
				+ ", codLojaRmsDg=" + codLojaRmsDg + ", codigoEmpresaSitef=" + codigoEmpresaSitef + ", ecommerceAtivo="
				+ ecommerceAtivo + ", pickAndPackAtivo=" + pickAndPackAtivo + ", horarioPriceUpdate="
				+ horarioPriceUpdate + ", warehouse=" + warehouse + ", prioridadeEnvioAtivo=" + prioridadeEnvioAtivo
				+ ", prioridadeEnvioRanking=" + prioridadeEnvioRanking + "]";
	}
}
