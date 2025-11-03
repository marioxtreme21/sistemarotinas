package sistema.rotinas.primefaces.dto;

public class ItemSubstituidoDTO {

    // Informações do item original
    private String skuOriginal;
    private String itemNameOriginal;
    private String eanOriginal;
    private Double precoOriginal;
    private Integer quantidadeOriginal;

    // Informações do item substituído
    private String skuSubstituido;
    private String itemNameSubstituido;
    private String eanSubstituido;
    private Double precoSubstituido;
    private Integer quantidadeSubstituida;

    public ItemSubstituidoDTO() {
    }

    public ItemSubstituidoDTO(String skuOriginal, String itemNameOriginal, String eanOriginal, Double precoOriginal, Integer quantidadeOriginal,
                              String skuSubstituido, String itemNameSubstituido, String eanSubstituido, Double precoSubstituido, Integer quantidadeSubstituida) {
        this.skuOriginal = skuOriginal;
        this.itemNameOriginal = itemNameOriginal;
        this.eanOriginal = eanOriginal;
        this.precoOriginal = precoOriginal;
        this.quantidadeOriginal = quantidadeOriginal;
        this.skuSubstituido = skuSubstituido;
        this.itemNameSubstituido = itemNameSubstituido;
        this.eanSubstituido = eanSubstituido;
        this.precoSubstituido = precoSubstituido;
        this.quantidadeSubstituida = quantidadeSubstituida;
    }

    public String getSkuOriginal() {
        return skuOriginal;
    }

    public void setSkuOriginal(String skuOriginal) {
        this.skuOriginal = skuOriginal;
    }

    public String getItemNameOriginal() {
        return itemNameOriginal;
    }

    public void setItemNameOriginal(String itemNameOriginal) {
        this.itemNameOriginal = itemNameOriginal;
    }

    public String getEanOriginal() {
        return eanOriginal;
    }

    public void setEanOriginal(String eanOriginal) {
        this.eanOriginal = eanOriginal;
    }

    public Double getPrecoOriginal() {
        return precoOriginal;
    }

    public void setPrecoOriginal(Double precoOriginal) {
        this.precoOriginal = precoOriginal;
    }

    public Integer getQuantidadeOriginal() {
        return quantidadeOriginal;
    }

    public void setQuantidadeOriginal(Integer quantidadeOriginal) {
        this.quantidadeOriginal = quantidadeOriginal;
    }

    public String getSkuSubstituido() {
        return skuSubstituido;
    }

    public void setSkuSubstituido(String skuSubstituido) {
        this.skuSubstituido = skuSubstituido;
    }

    public String getItemNameSubstituido() {
        return itemNameSubstituido;
    }

    public void setItemNameSubstituido(String itemNameSubstituido) {
        this.itemNameSubstituido = itemNameSubstituido;
    }

    public String getEanSubstituido() {
        return eanSubstituido;
    }

    public void setEanSubstituido(String eanSubstituido) {
        this.eanSubstituido = eanSubstituido;
    }

    public Double getPrecoSubstituido() {
        return precoSubstituido;
    }

    public void setPrecoSubstituido(Double precoSubstituido) {
        this.precoSubstituido = precoSubstituido;
    }

    public Integer getQuantidadeSubstituida() {
        return quantidadeSubstituida;
    }

    public void setQuantidadeSubstituida(Integer quantidadeSubstituida) {
        this.quantidadeSubstituida = quantidadeSubstituida;
    }

    @Override
    public String toString() {
        return "ItemSubstituidoDTO{" +
                "skuOriginal='" + skuOriginal + '\'' +
                ", itemNameOriginal='" + itemNameOriginal + '\'' +
                ", eanOriginal='" + eanOriginal + '\'' +
                ", precoOriginal=" + precoOriginal +
                ", quantidadeOriginal=" + quantidadeOriginal +
                ", skuSubstituido='" + skuSubstituido + '\'' +
                ", itemNameSubstituido='" + itemNameSubstituido + '\'' +
                ", eanSubstituido='" + eanSubstituido + '\'' +
                ", precoSubstituido=" + precoSubstituido +
                ", quantidadeSubstituida=" + quantidadeSubstituida +
                '}';
    }
}
