package sistema.rotinas.primefaces.bean;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import sistema.rotinas.primefaces.dto.CtrIntVdaDTO;
import sistema.rotinas.primefaces.service.interfaces.ICtrIntVdaService;

@Component
@Named
@SessionScoped
public class CtrIntVdaBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(CtrIntVdaBean.class);

    private final ICtrIntVdaService service;

    public CtrIntVdaBean(ICtrIntVdaService service) {
        this.service = service;
    }

    /** Limite de caracteres por célula no Excel (POI) */
    private static final int EXCEL_CELL_MAX = 32767;
    /** Reservamos alguns caracteres para o sufixo de truncamento */
    private static final String TRUNC_SUFFIX = " [TRUNCATED]";
    private static final int EXCEL_SAFE_LEN = EXCEL_CELL_MAX - TRUNC_SUFFIX.length();

    /** Converte qualquer valor para texto aceitável pelo Excel, truncando se necessário. */
    private String toExcelText(Object val) {
        if (val == null) return "";
        String s = String.valueOf(val);

        // Opcional: normalizar quebras de linha/caracteres de controle que às vezes quebram planilhas
        s = s.replace("\r", "\n"); // unificar
        // Remove caracteres de controle não imprimíveis (exceto tab/newline)
        StringBuilder cleaned = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\n' || ch == '\t' || (ch >= 32 && ch != 127)) {
                cleaned.append(ch);
            }
        }
        s = cleaned.toString();

        if (s.length() > EXCEL_CELL_MAX) {
            return s.substring(0, EXCEL_SAFE_LEN) + TRUNC_SUFFIX;
        }
        return s;
    }

    /** Exporta XLSX com todas as colunas originais + TRANSACTION_ID (derivada de JSO_ENV). */
    public StreamedContent getRelatorioCtrIntVdaXlsx() {
        try {
            List<CtrIntVdaDTO> dados = service.listarComTransactionId();

            if (dados == null || dados.isEmpty()) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Atenção", "Nenhum registro encontrado (tip_int=16)."));
                return null;
            }

            // Descobrir as colunas originais (em ordem) a partir da primeira linha
            LinkedHashMap<String, Object> firstRow = dados.get(0).getRow();
            List<String> colunas = new ArrayList<>(firstRow.keySet());
            // Acrescenta a derivada
            colunas.add("TRANSACTION_ID");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (XSSFWorkbook wb = new XSSFWorkbook()) {
                Sheet sheet = wb.createSheet("ctr_int_vda_16");

                // Cabeçalho
                Row header = sheet.createRow(0);
                for (int c = 0; c < colunas.size(); c++) {
                    header.createCell(c).setCellValue(colunas.get(c));
                }

                // Linhas
                int rowIdx = 1;
                for (CtrIntVdaDTO dto : dados) {
                    Row r = sheet.createRow(rowIdx++);
                    int c = 0;

                    // originais
                    for (String col : firstRow.keySet()) {
                        Object val = dto.getRow().get(col);
                        r.createCell(c++).setCellValue(toExcelText(val));
                    }
                    // derivada
                    r.createCell(c).setCellValue(toExcelText(dto.getTransactionId()));
                }

                for (int i = 0; i < colunas.size(); i++) sheet.autoSizeColumn(i);
                wb.write(baos);
            }

            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
            String nome = "ctr_int_vda_tip16_" + ts + ".xlsx";

            return DefaultStreamedContent.builder()
                    .name(nome)
                    .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .stream(() -> new ByteArrayInputStream(baos.toByteArray()))
                    .build();

        } catch (Exception e) {
            log.error("Erro ao gerar XLSX de ctr_int_vda (tip_int=16).", e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Falha ao gerar o XLSX. Veja os logs."));
            return null;
        }
    }
}
