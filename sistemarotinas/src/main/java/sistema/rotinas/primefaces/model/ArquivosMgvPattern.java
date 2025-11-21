package sistema.rotinas.primefaces.model;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.*;

@Entity
@Table(name = "arquivos_mgv_pattern")
public class ArquivosMgvPattern implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "pattern_id")
	private Long patternId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "mgv_id", nullable = false)
	private ArquivosMgv mgv;

	@Column(name = "pattern", nullable = false, length = 200)
	private String pattern; // ex.: VENDAS_*.csv

	@Column(name = "required", nullable = false)
	private Boolean required = true;

	// Getters/Setters/equals/hashCode/toString
	public Long getPatternId() {
		return patternId;
	}

	public void setPatternId(Long patternId) {
		this.patternId = patternId;
	}

	public ArquivosMgv getMgv() {
		return mgv;
	}

	public void setMgv(ArquivosMgv mgv) {
		this.mgv = mgv;
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
		if (!(o instanceof ArquivosMgvPattern))
			return false;
		ArquivosMgvPattern that = (ArquivosMgvPattern) o;
		return Objects.equals(patternId, that.patternId);
	}

	@Override
	public String toString() {
		return "ArquivosMgvPattern{id=" + patternId + ", pattern='" + pattern + "'}";
	}
}
