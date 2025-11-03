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

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import sistema.rotinas.primefaces.dto.ProdutoEcommerceExternoDTO;
import sistema.rotinas.primefaces.service.ComparacaoViewService;
import sistema.rotinas.primefaces.service.interfaces.IProdutoEcommerceExternoService;

import org.primefaces.model.StreamedContent;
import org.primefaces.model.DefaultStreamedContent;

@Component
@Named
@SessionScoped
public class ProdutoEcommerceExternoBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger log = LoggerFactory.getLogger(ProdutoEcommerceExternoBean.class);

	@Autowired
	private IProdutoEcommerceExternoService produtoService;

	@Autowired
	private ComparacaoViewService comparacaoService;

	private List<ProdutoEcommerceExternoDTO> ultimosLidos;
	private int limiteLeitura = 10;

	private int sampleComparacao = 50;

	// B \ A
	private Integer totalDiferencas;
	private List<Long> sampleCodsDiferentes;

	// A \ B
	private Integer totalDiferencasRmsMenosExterno;
	private List<Long> sampleCodsDiferentesRmsMenosExterno;

	// "B_A" (EXTERNO\@rms) ou "A_B" (@rms\EXTERNO) para export
	private String direcao = "B_A";

	@PostConstruct
	public void init() {
		log.info("Inicializando ProdutoEcommerceExternoBean (comparação bidirecional + export XLSX)");
	}

	public void lerPrimeiros() {
		try {
			log.info("Lendo os {} primeiros registros da view produtos_ecommerce (Oracle EXTERNO)...", limiteLeitura);
			ultimosLidos = produtoService.getPrimeiros(limiteLeitura);
			log.info("Leitura concluída. Registros obtidos: {}", (ultimosLidos != null ? ultimosLidos.size() : 0));

			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_INFO, "Leitura concluída",
							"Foram lidos " + (ultimosLidos != null ? ultimosLidos.size() : 0) + " registros."));
		} catch (Exception e) {
			log.error("Erro ao ler registros de produtos_ecommerce (EXTERNO).", e);
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro",
					"Falha ao ler registros da view externa. Verifique os logs."));
		}
	}

	// ----- B \ A -----
	public void contarDiferencas() {
		try {
			totalDiferencas = comparacaoService.countDiffExternoMenosRms();
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
					"Contagem EXTERNO \\ @rms", "Total: " + totalDiferencas));
		} catch (Exception e) {
			log.error("Erro ao contar diferenças EXTERNO−@rms.", e);
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Falha ao contar diferenças. Veja os logs."));
		}
	}

	public void compararMostrarSample() {
		try {
			sampleCodsDiferentes = comparacaoService.diffExternoMenosRmsSample(sampleComparacao);
			if (sampleCodsDiferentes != null) {
				sampleCodsDiferentes.forEach(c -> System.out.println("FALTA NO @rms -> COD: " + c));
			}
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_INFO, "Sample EXTERNO \\ @rms",
							"Itens: " + (sampleCodsDiferentes != null ? sampleCodsDiferentes.size() : 0)));
		} catch (Exception e) {
			log.error("Erro ao obter sample EXTERNO−@rms.", e);
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Falha ao coletar sample. Veja os logs."));
		}
	}

	// ----- A \ B -----
	public void contarDiferencasRmsMenosExterno() {
		try {
			totalDiferencasRmsMenosExterno = comparacaoService.countDiffRmsMenosExterno();
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
					"Contagem @rms \\ EXTERNO", "Total: " + totalDiferencasRmsMenosExterno));
		} catch (Exception e) {
			log.error("Erro ao contar diferenças @rms−EXTERNO.", e);
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Falha ao contar diferenças. Veja os logs."));
		}
	}

	public void compararMostrarSampleRmsMenosExterno() {
		try {
			sampleCodsDiferentesRmsMenosExterno = comparacaoService.diffRmsMenosExternoSample(sampleComparacao);
			if (sampleCodsDiferentesRmsMenosExterno != null) {
				sampleCodsDiferentesRmsMenosExterno.forEach(c -> System.out.println("FALTA NO EXTERNO -> COD: " + c));
			}
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_INFO, "Sample @rms \\ EXTERNO",
							"Itens: " + (sampleCodsDiferentesRmsMenosExterno != null
									? sampleCodsDiferentesRmsMenosExterno.size()
									: 0)));
		} catch (Exception e) {
			log.error("Erro ao obter sample @rms−EXTERNO.", e);
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Falha ao coletar sample. Veja os logs."));
		}
	}

	// atalhos
	public void contarDiferencasBidirecional() {
		contarDiferencas();
		contarDiferencasRmsMenosExterno();
	}

	public void compararMostrarSampleBidirecional() {
		compararMostrarSample();
		compararMostrarSampleRmsMenosExterno();
	}

	// ----- Exportar XLSX (por COD) -----
	public StreamedContent getRelatorioDiferencasXlsx() {
		try {
			final List<ProdutoEcommerceExternoDTO> dados;
			final String titulo;

			if ("A_B".equals(direcao)) {
				dados = comparacaoService.detalhesRmsMenosExterno(); // @rms \ EXTERNO
				titulo = "A_B";
			} else {
				dados = comparacaoService.detalhesExternoMenosRms(); // EXTERNO \ @rms
				titulo = "B_A";
			}

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (XSSFWorkbook wb = new XSSFWorkbook()) {
				Sheet sheet = wb.createSheet("Diferencas_" + titulo);

				// cabeçalho
				Row header = sheet.createRow(0);
				header.createCell(0).setCellValue("COD (cod-dg)");
				header.createCell(1).setCellValue("DG");
				header.createCell(2).setCellValue("EAN");
				header.createCell(3).setCellValue("DESCRICAO");
				header.createCell(4).setCellValue("SECAO");
				header.createCell(5).setCellValue("GRUPO");
				header.createCell(6).setCellValue("SGRUPO");

				// linhas
				int rowIdx = 1;
				for (ProdutoEcommerceExternoDTO p : dados) {
					Row r = sheet.createRow(rowIdx++);

					Integer cod = p.getCod();
					Integer dg = p.getDg();
					String codDg = (cod != null ? cod : 0) + "-" + (dg != null ? dg : 0);

					r.createCell(0).setCellValue(codDg); // COD (concatenado com DG)
					r.createCell(1).setCellValue(dg != null ? dg : 0);
					r.createCell(2).setCellValue(p.getEan() != null ? p.getEan() : "");
					r.createCell(3).setCellValue(p.getDescricao() != null ? p.getDescricao() : "");
					r.createCell(4).setCellValue(p.getSecao() != null ? p.getSecao() : 0);
					r.createCell(5).setCellValue(p.getGrupo() != null ? p.getGrupo() : 0);
					r.createCell(6).setCellValue(p.getSgrupo() != null ? p.getSgrupo() : 0);
				}

				for (int i = 0; i < 7; i++)
					sheet.autoSizeColumn(i);
				wb.write(baos);
			}

			String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
			String nome = "Diferencas_" + titulo + "_" + ts + ".xlsx";

			return DefaultStreamedContent.builder().name(nome)
					.contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
					.stream(() -> new ByteArrayInputStream(baos.toByteArray())).build();

		} catch (Exception e) {
			log.error("Erro ao gerar relatório XLSX de diferenças.", e);
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Erro ao gerar XLSX", "Falha ao gerar o arquivo. Veja os logs."));
			return null;
		}
	}

	// ===== Exportar XLSX por LOJA+COD =====
	public StreamedContent getRelatorioDiferencasPorLojaXlsx() {
		try {
			final List<ProdutoEcommerceExternoDTO> dados;
			final String titulo;

			if ("A_B".equals(direcao)) {
				dados = comparacaoService.detalhesRmsMenosExternoPorLoja(); // @rms \ EXTERNO por (loja,cod)
				titulo = "A_B";
			} else {
				dados = comparacaoService.detalhesExternoMenosRmsPorLoja(); // EXTERNO \ @rms por (loja,cod)
				titulo = "B_A";
			}

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (XSSFWorkbook wb = new XSSFWorkbook()) {
				Sheet sheet = wb.createSheet("DiffPorLoja_" + titulo);

				// cabeçalho
				Row header = sheet.createRow(0);
				header.createCell(0).setCellValue("LOJA");
				header.createCell(1).setCellValue("COD (cod-dg)");
				header.createCell(2).setCellValue("DG");
				header.createCell(3).setCellValue("EAN");
				header.createCell(4).setCellValue("DESCRICAO");
				header.createCell(5).setCellValue("SECAO");
				header.createCell(6).setCellValue("GRUPO");
				header.createCell(7).setCellValue("SGRUPO");

				// linhas
				int rowIdx = 1;
				for (ProdutoEcommerceExternoDTO p : dados) {
					Row r = sheet.createRow(rowIdx++);

					Integer loja = p.getLoja();
					Integer cod = p.getCod();
					Integer dg = p.getDg();
					String codDg = (cod != null ? cod : 0) + "-" + (dg != null ? dg : 0);

					r.createCell(0).setCellValue(loja != null ? loja : 0);
					r.createCell(1).setCellValue(codDg);
					r.createCell(2).setCellValue(dg != null ? dg : 0);
					r.createCell(3).setCellValue(p.getEan() != null ? p.getEan() : "");
					r.createCell(4).setCellValue(p.getDescricao() != null ? p.getDescricao() : "");
					r.createCell(5).setCellValue(p.getSecao() != null ? p.getSecao() : 0);
					r.createCell(6).setCellValue(p.getGrupo() != null ? p.getGrupo() : 0);
					r.createCell(7).setCellValue(p.getSgrupo() != null ? p.getSgrupo() : 0);
				}

				for (int i = 0; i < 8; i++)
					sheet.autoSizeColumn(i);
				wb.write(baos);
			}

			String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
			String nome = "DiferencasPorLoja_" + titulo + "_" + ts + ".xlsx";

			return DefaultStreamedContent.builder().name(nome)
					.contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
					.stream(() -> new ByteArrayInputStream(baos.toByteArray())).build();

		} catch (Exception e) {
			log.error("Erro ao gerar relatório XLSX de diferenças por LOJA.", e);
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Erro ao gerar XLSX por loja", "Falha ao gerar o arquivo. Veja os logs."));
			return null;
		}
	}

	// getters/setters
	public List<ProdutoEcommerceExternoDTO> getUltimosLidos() {
		return ultimosLidos;
	}

	public int getLimiteLeitura() {
		return limiteLeitura;
	}

	public void setLimiteLeitura(int limiteLeitura) {
		this.limiteLeitura = limiteLeitura;
	}

	public int getSampleComparacao() {
		return sampleComparacao;
	}

	public void setSampleComparacao(int sampleComparacao) {
		this.sampleComparacao = sampleComparacao;
	}

	public Integer getTotalDiferencas() {
		return totalDiferencas;
	}

	public List<Long> getSampleCodsDiferentes() {
		return sampleCodsDiferentes;
	}

	public Integer getTotalDiferencasRmsMenosExterno() {
		return totalDiferencasRmsMenosExterno;
	}

	public List<Long> getSampleCodsDiferentesRmsMenosExterno() {
		return sampleCodsDiferentesRmsMenosExterno;
	}

	public String getDirecao() {
		return direcao;
	}

	public void setDirecao(String direcao) {
		this.direcao = direcao;
	}
}
