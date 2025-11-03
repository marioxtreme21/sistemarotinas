package sistema.rotinas.primefaces.bean;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import sistema.rotinas.primefaces.dto.SkuMigracaoDTO;
import sistema.rotinas.primefaces.service.interfaces.ISkuMigracaoService;

@Component
@Named
@SessionScoped
public class SkuMigracaoBean implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final Logger log = LoggerFactory.getLogger(SkuMigracaoBean.class);

	@Autowired
	private ISkuMigracaoService service;

	/** Exporta XLSX: colunas Sku | Sku Novo | Descrição */
	public StreamedContent getRelatorioFalhaAtivacaoXlsx() {
		try {
			List<SkuMigracaoDTO> dados = service.gerarDadosRelatorio();

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (XSSFWorkbook wb = new XSSFWorkbook()) {
				Sheet sheet = wb.createSheet("Plan1");

				// Cabeçalho
				Row header = sheet.createRow(0);
				header.createCell(0).setCellValue("Sku");
				header.createCell(1).setCellValue("Sku Novo");
				header.createCell(2).setCellValue("Descrição");

				// Linhas
				int rowIdx = 1;
				for (SkuMigracaoDTO d : dados) {
					Row r = sheet.createRow(rowIdx++);
					r.createCell(0).setCellValue(d.getSku() != null ? d.getSku() : 0);
					r.createCell(1).setCellValue(d.getSkuNovo() != null ? d.getSkuNovo() : "");
					r.createCell(2).setCellValue(d.getDescricao() != null ? d.getDescricao() : "");
				}

				for (int i = 0; i < 3; i++)
					sheet.autoSizeColumn(i);
				wb.write(baos);
			}

			String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
			String nome = "Falha_Ativacao_Produto_" + ts + ".xlsx";

			return DefaultStreamedContent.builder().name(nome)
					.contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
					.stream(() -> new ByteArrayInputStream(baos.toByteArray())).build();

		} catch (Exception e) {
			log.error("Erro ao gerar XLSX de Falha na Ativação do Produto.", e);
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Falha ao gerar o XLSX. Veja os logs."));
			return null;
		}
	}
}
