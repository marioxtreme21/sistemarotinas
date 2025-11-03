package sistema.rotinas.primefaces.dto;

public class PedidoClienteDTO {

    private Long numPedido;
    private String nomeCliente;
    private String dataPedido;       // Novo campo
    private String dataFaturamento;  // Novo campo

    public PedidoClienteDTO() {
        // construtor vazio para uso em mapeamento
    }

    public PedidoClienteDTO(Long numPedido, String nomeCliente, String dataPedido, String dataFaturamento) {
        this.numPedido = numPedido;
        this.nomeCliente = nomeCliente;
        this.dataPedido = dataPedido;
        this.dataFaturamento = dataFaturamento;
    }

    public Long getNumPedido() {
        return numPedido;
    }

    public void setNumPedido(Long numPedido) {
        this.numPedido = numPedido;
    }

    public String getNomeCliente() {
        return nomeCliente;
    }

    public void setNomeCliente(String nomeCliente) {
        this.nomeCliente = nomeCliente;
    }

    public String getDataPedido() {
        return dataPedido;
    }

    public void setDataPedido(String dataPedido) {
        this.dataPedido = dataPedido;
    }

    public String getDataFaturamento() {
        return dataFaturamento;
    }

    public void setDataFaturamento(String dataFaturamento) {
        this.dataFaturamento = dataFaturamento;
    }

    @Override
    public String toString() {
        return "PedidoClienteDTO{" +
                "numPedido=" + numPedido +
                ", nomeCliente='" + nomeCliente + '\'' +
                ", dataPedido='" + dataPedido + '\'' +
                ", dataFaturamento='" + dataFaturamento + '\'' +
                '}';
    }
}
