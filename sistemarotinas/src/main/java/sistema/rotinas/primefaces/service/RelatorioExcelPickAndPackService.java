package sistema.rotinas.primefaces.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import sistema.rotinas.primefaces.dto.RelatorioItemSubstituicaoDTO;
import sistema.rotinas.primefaces.util.PastaUploadUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class RelatorioExcelPickAndPackService {

    public String gerarRelatorioXLSX(String codLojaRms, String dataReferencia, List<RelatorioItemSubstituicaoDTO> dados) {
        try {
            String nomeArquivo = String.format("RelatorioItensSubstituidosPickAndPac_%s_%s.xlsx", codLojaRms, dataReferencia);
            String baseDir = PastaUploadUtil.PASTA_RELATORIOS; // ✅ agora usa o caminho configurado via app.upload.base-dir
            Path dirPath = Paths.get(baseDir);
            Files.createDirectories(dirPath);

            Path filePath = dirPath.resolve(nomeArquivo);

            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Itens Substituídos");

                // Estilos
                CellStyle headerStyle = criarEstiloCabecalho(workbook);
                CellStyle cellStyle = criarEstiloCelula(workbook);
                CellStyle decimalStyle = criarEstiloDecimal(workbook);

                // Cabeçalhos
                String[] colunas = {
                    "Loja RMS", "Pedido", "Cliente", "Código", "EAN", "Descrição", "Preço", "Quantidade",
                    "Tipo Item", "Estoque RMS", "Data Última Entrada", "Data Pedido", "Data Faturamento", "Diferença % Preço"
                };

                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < colunas.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(colunas[i]);
                    cell.setCellStyle(headerStyle);
                }

                int rowIdx = 1;
                for (RelatorioItemSubstituicaoDTO item : dados) {
                    Row row = sheet.createRow(rowIdx++);

                    row.createCell(0).setCellValue(item.getLojaRms());
                    row.createCell(1).setCellValue(item.getPedido());
                    row.createCell(2).setCellValue(item.getCliente());
                    row.createCell(3).setCellValue(item.getCodigo());
                    row.createCell(4).setCellValue(item.getEan());
                    row.createCell(5).setCellValue(item.getDescricao());

                    Cell precoCell = row.createCell(6);
                    if (item.getPreco() != null) {
                        precoCell.setCellValue(item.getPreco());
                        precoCell.setCellStyle(decimalStyle);
                    }

                    Cell qtdCell = row.createCell(7);
                    if (item.getQuantidade() != null) {
                        qtdCell.setCellValue(item.getQuantidade());
                        qtdCell.setCellStyle(cellStyle);
                    }

                    row.createCell(8).setCellValue(item.getTipoItem());
                    row.createCell(9).setCellValue(item.getEstRms());
                    row.createCell(10).setCellValue(item.getDataUltimaEntrada());
                    row.createCell(11).setCellValue(item.getDataPedido());
                    row.createCell(12).setCellValue(item.getDataFaturamento());

                    Cell difCell = row.createCell(13);
                    if (item.getDiferencaPercentualPreco() != null) {
                        difCell.setCellValue(item.getDiferencaPercentualPreco());
                        difCell.setCellStyle(decimalStyle);
                    }
                }

                // Auto ajuste
                for (int i = 0; i < colunas.length; i++) {
                    sheet.autoSizeColumn(i);
                }

                // Salva o arquivo
                try (FileOutputStream out = new FileOutputStream(filePath.toFile())) {
                    workbook.write(out);
                }

                System.out.println("✅ Relatório XLSX gerado: " + filePath.toString());
                return filePath.toString();
            }

        } catch (Exception e) {
            System.err.println("❌ Erro ao gerar relatório XLSX: " + e.getMessage());
            return null;
        }
    }

    private CellStyle criarEstiloCabecalho(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle criarEstiloCelula(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }

    private CellStyle criarEstiloDecimal(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        DataFormat format = wb.createDataFormat();
        style.setDataFormat(format.getFormat("0.00"));
        return style;
    }
}
