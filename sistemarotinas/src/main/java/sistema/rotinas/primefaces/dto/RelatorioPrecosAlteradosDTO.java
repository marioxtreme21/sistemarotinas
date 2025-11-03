package sistema.rotinas.primefaces.dto;

import java.io.Serializable;

public class RelatorioPrecosAlteradosDTO implements Serializable {
	private static final long serialVersionUID = 1L;

	private Integer loja;
	private Long codigo;
	private String ean;
	private String descricao;

	private String promocao; // 'N' ou 'P'
	private Double precoNormal;
	private Double precoPromocional;

	private String dataInicioPromocao; // dd/MM/yyyy HH:mm:ss (opcional)
	private String dataFimPromocao; // dd/MM/yyyy HH:mm:ss (opcional)
	private String dataAlteracao; // dd/MM/yyyy HH:mm:ss
	private Long seqfamilia;

	private String ordemCategoriaN1; // "DescricaoN1 - COD_N1"
	private String ordemCategoriaN2; // "DescricaoN2" (pode vir nulo)
	private Integer codCatN1;
	private Integer codCatN2;

	private Integer qtdembalagem;

	// Flags caso você queira usar printWhenExpression no Jasper
	private Integer breakN1; // 1 quando inicia novo grupo N1
	private Integer breakN2; // 1 quando inicia novo grupo N2

	// Getters/Setters
	public Integer getLoja() {
		return loja;
	}

	public void setLoja(Integer loja) {
		this.loja = loja;
	}

	public Long getCodigo() {
		return codigo;
	}

	public void setCodigo(Long codigo) {
		this.codigo = codigo;
	}

	public String getEan() {
		return ean;
	}

	public void setEan(String ean) {
		this.ean = ean;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}

	public String getPromocao() {
		return promocao;
	}

	public void setPromocao(String promocao) {
		this.promocao = promocao;
	}

	public Double getPrecoNormal() {
		return precoNormal;
	}

	public void setPrecoNormal(Double precoNormal) {
		this.precoNormal = precoNormal;
	}

	public Double getPrecoPromocional() {
		return precoPromocional;
	}

	public void setPrecoPromocional(Double precoPromocional) {
		this.precoPromocional = precoPromocional;
	}

	public String getDataInicioPromocao() {
		return dataInicioPromocao;
	}

	public void setDataInicioPromocao(String dataInicioPromocao) {
		this.dataInicioPromocao = dataInicioPromocao;
	}

	public String getDataFimPromocao() {
		return dataFimPromocao;
	}

	public void setDataFimPromocao(String dataFimPromocao) {
		this.dataFimPromocao = dataFimPromocao;
	}

	public String getDataAlteracao() {
		return dataAlteracao;
	}

	public void setDataAlteracao(String dataAlteracao) {
		this.dataAlteracao = dataAlteracao;
	}

	public Long getSeqfamilia() {
		return seqfamilia;
	}

	public void setSeqfamilia(Long seqfamilia) {
		this.seqfamilia = seqfamilia;
	}

	public String getOrdemCategoriaN1() {
		return ordemCategoriaN1;
	}

	public void setOrdemCategoriaN1(String ordemCategoriaN1) {
		this.ordemCategoriaN1 = ordemCategoriaN1;
	}

	public String getOrdemCategoriaN2() {
		return ordemCategoriaN2;
	}

	public void setOrdemCategoriaN2(String ordemCategoriaN2) {
		this.ordemCategoriaN2 = ordemCategoriaN2;
	}

	public Integer getCodCatN1() {
		return codCatN1;
	}

	public void setCodCatN1(Integer codCatN1) {
		this.codCatN1 = codCatN1;
	}

	public Integer getCodCatN2() {
		return codCatN2;
	}

	public void setCodCatN2(Integer codCatN2) {
		this.codCatN2 = codCatN2;
	}

	public Integer getQtdembalagem() {
		return qtdembalagem;
	}

	public void setQtdembalagem(Integer qtdembalagem) {
		this.qtdembalagem = qtdembalagem;
	}

	public Integer getBreakN1() {
		return breakN1;
	}

	public void setBreakN1(Integer breakN1) {
		this.breakN1 = breakN1;
	}

	public Integer getBreakN2() {
		return breakN2;
	}

	public void setBreakN2(Integer breakN2) {
		this.breakN2 = breakN2;
	}
}
