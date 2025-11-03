package sistema.rotinas.primefaces.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class RelatorioExcelService {

    public String gerarRelatorioXLSX(String codLojaEconect, List<Map<String, Object>> dados) {
        String dataAtual = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        String nomeArquivo = "Relatorio_Loja_" + codLojaEconect + "_" + dataAtual + ".xlsx";

        try {
            String tempDir = System.getProperty("java.io.tmpdir") + File.separator + "rcg_temp_reports";
            Path folderPath = Paths.get(tempDir);
            Files.createDirectories(folderPath);

            Path filePath = folderPath.resolve(nomeArquivo);

            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Relatório Promoções");

                // Criando estilos
                CellStyle tituloStyle = getTituloCellStyle(workbook);
                CellStyle headerStyle = getHeaderCellStyle(workbook);
                CellStyle cellStyle = getCurrencyCellStyle(workbook);
                CellStyle currencyStyle = getCurrencyCellStyle(workbook);
                CellStyle integerStyle = getIntegerCellStyle(workbook); // Novo estilo para inteiros

                // Criar título
                Row tituloRow = sheet.createRow(0);
                Cell tituloCell = tituloRow.createCell(0);
                tituloCell.setCellValue("Relatório de Promoções Mix e Leve & Pague Lançamento Econect - Data de Geração: " + dataAtual);
                tituloCell.setCellStyle(tituloStyle);

                // Mesclar células para o título
                sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 15));

                // Criar cabeçalho com a nova ordem
                Row headerRow = sheet.createRow(2);
                String[][] colunasMap = {
                        {"Loja", "loja"},
                        {"Cod Mix", "cod_mix"},
                        {"Desc Mix", "desc_mix"},
                        {"Cod Produto", "c_produto"},
                        {"Desc Produto", "descricao_produto"},
                        {"Quantidade", "quantidade"},
                        {"Preço Vigente", "preco_vigente"},
                        {"Valor Desconto", "Valor_Desconto"},
                        {"Preço Unitário", "preco_unitario"},
                        {"Desconto Valor", "Desconto_Valor"},
                        {"Desconto Percentual", "Desconto_Percentual"},
                        {"Produto Desconto", "Produto_C_desconto"},
                        {"Data Inicial", "data_inicial"},
                        {"Data Final", "data_final"},
                        {"Origem Promocao", "Origem_Promocao"}
                };

                for (int i = 0; i < colunasMap.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(colunasMap[i][0]);
                    cell.setCellStyle(headerStyle);
                }

                // Preencher os dados
                int rowNum = 3;
                for (Map<String, Object> linha : dados) {
                    Row row = sheet.createRow(rowNum);
                    int cellNum = 0;

                    for (String[] colunaMap : colunasMap) {
                        String nomeColunaBanco = colunaMap[1];
                        Cell cell = row.createCell(cellNum);

                        if ("preco_unitario".equals(nomeColunaBanco)) {
                            String formula = "(F" + (rowNum + 1) + "*G" + (rowNum + 1) + "-H" + (rowNum + 1) + ")/F" + (rowNum + 1);
                            cell.setCellFormula(formula);
                            cell.setCellStyle(currencyStyle);
                        } else {
                            Object valor = linha.get(nomeColunaBanco);
                            if (valor != null) {
                                String valorStr = valor.toString().trim();
                                if (isNumero(valorStr)) {
                                    double valorNumerico = Double.parseDouble(valorStr);
                                    
                                    // Aplicar formatação correta dependendo da coluna
                                    if (nomeColunaBanco.equals("loja") || nomeColunaBanco.equals("cod_mix") || nomeColunaBanco.equals("c_produto") || nomeColunaBanco.equals("quantidade")) {
                                        cell.setCellValue((int) valorNumerico);
                                        cell.setCellStyle(integerStyle); // Usa estilo de número inteiro
                                    } else {
                                        cell.setCellValue(valorNumerico);
                                        cell.setCellStyle(currencyStyle); // Mantém estilo decimal para valores monetários
                                    }
                                } else {
                                    cell.setCellValue(valorStr);
                                    cell.setCellStyle(cellStyle);
                                }
                            }
                        }
                        cellNum++;
                    }
                    rowNum++;
                }

                // Autoajuste de colunas
                for (int i = 0; i < colunasMap.length; i++) {
                    sheet.autoSizeColumn(i);
                }

                // Salvar o arquivo
                try (FileOutputStream fileOut = new FileOutputStream(filePath.toFile())) {
                    workbook.write(fileOut);
                }

                System.out.println("✅ Relatório gerado com sucesso: " + filePath.toString());
                return filePath.toString();

            }
        } catch (IOException e) {
            System.err.println("❌ Erro ao gerar o relatório XLSX: " + e.getMessage());
            return null;
        }
    }

    /**
     * Método para verificar se uma string representa um número válido
     */
    private boolean isNumero(String valor) {
        return valor.matches("^-?\\d+(\\.\\d+)?$"); // Permite números inteiros e decimais
    }

    /**
     * Estilo para o título do relatório
     */
    private CellStyle getTituloCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    /**
     * Estilo para os cabeçalhos da planilha
     */
    private CellStyle getHeaderCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * Estilo para células numéricas (valores monetários)
     */
    private CellStyle getCurrencyCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("0.00")); // 2 casas decimais
        return style;
    }

    /**
     * Estilo para células numéricas inteiras
     */
    private CellStyle getIntegerCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("0")); // Sem casas decimais
        return style;
    }
}
