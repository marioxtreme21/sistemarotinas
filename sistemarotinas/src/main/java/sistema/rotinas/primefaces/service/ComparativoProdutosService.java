package sistema.rotinas.primefaces.service;

import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import sistema.rotinas.primefaces.dto.ProdutoNivelPrecoDTO;
import sistema.rotinas.primefaces.repository.ProdutoNivelPrecoRepository;
import sistema.rotinas.primefaces.service.interfaces.IComparativoProdutosService;

@Service
public class ComparativoProdutosService implements IComparativoProdutosService {

    private final ProdutoNivelPrecoRepository repository;

    public ComparativoProdutosService(ProdutoNivelPrecoRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ProdutoNivelPrecoDTO> eansPresentes144Ausentes50(int nivel144, int nivel50) {
        List<ProdutoNivelPrecoDTO> base144 = repository.buscarNo144(nivel144);
        List<ProdutoNivelPrecoDTO> base50  = repository.buscarNo50(nivel50);

        Set<String> eans50 = base50.stream()
                .map(ProdutoNivelPrecoDTO::getCodigoEan)
                .collect(Collectors.toCollection(HashSet::new));

        return base144.stream()
                .filter(dto -> dto.getCodigoEan() != null && !eans50.contains(dto.getCodigoEan()))
                .collect(Collectors.toList());
    }

    @Override
    public byte[] gerarPlanilhaExcel(List<ProdutoNivelPrecoDTO> itens) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Ausentes_no_50");

            // Cabeçalho
            Row header = sheet.createRow(0);
            String[] cols = {"codigo_nivel", "codigo_ean", "codigo_produto", "descricao", "pesavel", "embalagem", "preco"};
            for (int i = 0; i < cols.length; i++) header.createCell(i).setCellValue(cols[i]);

            // Dados (evita ternários misturando tipos)
            int r = 1;
            for (ProdutoNivelPrecoDTO dto : itens) {
                Row row = sheet.createRow(r++);

                row.createCell(0).setCellValue(dto.getCodigoNivel()  == null ? "" : String.valueOf(dto.getCodigoNivel()));
                row.createCell(1).setCellValue(dto.getCodigoEan()    == null ? "" : dto.getCodigoEan());
                row.createCell(2).setCellValue(dto.getCodigoProduto()== null ? "" : String.valueOf(dto.getCodigoProduto()));
                row.createCell(3).setCellValue(dto.getDescricao()     == null ? "" : dto.getDescricao());
                row.createCell(4).setCellValue(dto.getPesavel()       == null ? "" : String.valueOf(dto.getPesavel()));
                row.createCell(5).setCellValue(dto.getEmbalagem()     == null ? "" : dto.getEmbalagem());

                if (dto.getPreco() != null) {
                    row.createCell(6).setCellValue(dto.getPreco().doubleValue());
                } else {
                    row.createCell(6).setCellValue("");
                }
            }

            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);
            wb.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar XLSX do comparativo", e);
        }
    }
}
