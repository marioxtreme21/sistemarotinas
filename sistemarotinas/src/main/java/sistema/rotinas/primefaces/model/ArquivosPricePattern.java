package sistema.rotinas.primefaces.model;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.*;

@Entity
@Table(name = "arquivos_price_pattern")
public class ArquivosPricePattern implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "pattern_id")
	private Long patternId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "price_id", nullable = false)
	private ArquivosPrice price;

	@Column(name = "pattern", nullable = false, length = 200)
	private String pattern; // ex.: PRICE_*.csv

	@Column(name = "required", nullable = false)
	private Boolean required = true;

	// Getters/Setters/equals/hashCode/toString
	public Long getPatternId() {
		return patternId;
	}

	public void setPatternId(Long patternId) {
		this.patternId = patternId;
	}

	public ArquivosPrice getPrice() {
		return price;
	}

	public void setPrice(ArquivosPrice price) {
		this.price = price;
	}

	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	public Boolean getRequired() {
		return required;
	}

	public void setRequired(Boolean required) {
		this.required = required;
	}

	@Override
	public int hashCode() {
		return Objects.hash(patternId);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof ArquivosPricePattern))
			return false;
		ArquivosPricePattern that = (ArquivosPricePattern) o;
		return Objects.equals(patternId, that.patternId);
	}

	@Override
	public String toString() {
		return "ArquivosPricePattern{id=" + patternId + ", pattern='" + pattern + "'}";
	}
}
