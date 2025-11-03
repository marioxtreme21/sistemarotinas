package sistema.rotinas.primefaces.dto;

import java.util.ArrayList;
import java.util.List;

public class RelatorioMockFactory {

    public static List<RelatorioItemSubstituicaoDTO> getData() {
        List<RelatorioItemSubstituicaoDTO> lista = new ArrayList<>();

        RelatorioItemSubstituicaoDTO dto = new RelatorioItemSubstituicaoDTO();
        dto.setLojaRms("103");
        dto.setPedido("1729321");
        dto.setCliente("Jamile Santos");
        dto.setCodigo("200197");
        dto.setEan("78961101952");
        dto.setDescricao("Papel Toalha Scala 30 CM");
        dto.setPreco(15.98);
        dto.setQuantidade(1);
        dto.setDataPedido("12-06-2025");
        dto.setEstRms("10");
        dto.setDataFaturamento("13-06-2025");
        dto.setDataUltimaEntrada("2025-06-13 00:00:00.0");
        dto.setTipoItem("ORIGINAL");
        dto.setDiferencaPercentualPreco(21.5565656);

        lista.add(dto);
        return lista;
    }
}
