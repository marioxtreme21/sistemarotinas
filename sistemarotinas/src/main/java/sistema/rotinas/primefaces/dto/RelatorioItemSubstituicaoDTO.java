package sistema.rotinas.primefaces.dto;

public class RelatorioItemSubstituicaoDTO {

    private String lojaRms;
    private String pedido;
    private String cliente;
    private String codigo; // SKU
    private String ean;
    private String descricao;
    private Double preco;
    private Integer quantidade;
    private String dataPedido;
    private String estRms;
    private String dataUltimaEntrada;
    private String dataFaturamento;
    private String tipoItem; // "ORIGINAL" ou "SUBSTITUIDO"
    private Double diferencaPercentualPreco; // percentual do precoOriginal para precoSubstituido

    public RelatorioItemSubstituicaoDTO() {
        // construtor vazio
    }

    // Getters e Setters

    public String getLojaRms() {
        return lojaRms;
    }

    public void setLojaRms(String lojaRms) {
        this.lojaRms = lojaRms;
    }

    public String getPedido() {
        return pedido;
    }

    public void setPedido(String pedido) {
        this.pedido = pedido;
    }

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
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

    public Double getPreco() {
        return preco != null ? preco : 0.0;
    }

    public void setPreco(Double preco) {
        this.preco = preco;
    }

    public Integer getQuantidade() {
        return quantidade != null ? quantidade : 0;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public String getDataPedido() {
        return dataPedido;
    }

    public void setDataPedido(String dataPedido) {
        this.dataPedido = dataPedido;
    }

    public String getEstRms() {
        return estRms;
    }

    public void setEstRms(String estRms) {
        this.estRms = estRms;
    }

    public String getDataUltimaEntrada() {
        return dataUltimaEntrada;
    }

    public void setDataUltimaEntrada(String dataUltimaEntrada) {
        this.dataUltimaEntrada = dataUltimaEntrada;
    }

    public String getDataFaturamento() {
        return dataFaturamento;
    }

    public void setDataFaturamento(String dataFaturamento) {
        this.dataFaturamento = dataFaturamento;
    }

    public String getTipoItem() {
        return tipoItem;
    }

    public void setTipoItem(String tipoItem) {
        this.tipoItem = tipoItem;
    }

    public Double getDiferencaPercentualPreco() {
        return diferencaPercentualPreco != null ? diferencaPercentualPreco : 0.0;
    }

    public void setDiferencaPercentualPreco(Double diferencaPercentualPreco) {
        this.diferencaPercentualPreco = diferencaPercentualPreco;
    }

    @Override
    public String toString() {
        return "RelatorioItemSubstituicaoDTO{" +
                "lojaRms='" + lojaRms + '\'' +
                ", pedido='" + pedido + '\'' +
                ", cliente='" + cliente + '\'' +
                ", codigo='" + codigo + '\'' +
                ", ean='" + ean + '\'' +
                ", descricao='" + descricao + '\'' +
                ", preco=" + preco +
                ", quantidade=" + quantidade +
                ", dataPedido='" + dataPedido + '\'' +
                ", estRms='" + estRms + '\'' +
                ", dataUltimaEntrada='" + dataUltimaEntrada + '\'' +
                ", dataFaturamento='" + dataFaturamento + '\'' +
                ", tipoItem='" + tipoItem + '\'' +
                ", diferencaPercentualPreco=" + diferencaPercentualPreco +
                '}';
    }
}
