package sistema.rotinas.primefaces.service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import sistema.rotinas.primefaces.dto.DivergenciaPrecoDTO;
import sistema.rotinas.primefaces.dto.ProdutoNivelPrecoDTO;
import sistema.rotinas.primefaces.repository.ProdutoNivelPrecoRepository;
import sistema.rotinas.primefaces.repository.Promocao144Repository;
import sistema.rotinas.primefaces.service.interfaces.IDivergenciaPrecoService;

@Service
public class DivergenciaPrecoService implements IDivergenciaPrecoService {

	private final Promocao144Repository promocao144Repository;
	private final ProdutoNivelPrecoRepository nivel50Repository;

	private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	public DivergenciaPrecoService(Promocao144Repository promocao144Repository,
			ProdutoNivelPrecoRepository nivel50Repository) {
		this.promocao144Repository = promocao144Repository;
		this.nivel50Repository = nivel50Repository;
	}

	@Override
	public List<DivergenciaPrecoDTO> comparar(int loja144, int nivel50, LocalDateTime referencia) {
		// Se você quiser usar o limite <= 2025-12-01 00:00:00, troque a linha abaixo:
		// List<ProdutoNivelPrecoDTO> promocoes =
		// promocao144Repository.listarPromocoesVigentesAteDez2025(loja144, referencia);
		List<ProdutoNivelPrecoDTO> promocoes = promocao144Repository.listarPromocoesVigentes(loja144, referencia);

		List<ProdutoNivelPrecoDTO> itensNivel50 = nivel50Repository.buscarNo50(nivel50);

		Map<String, ProdutoNivelPrecoDTO> porEan50 = new LinkedHashMap<>();
		for (ProdutoNivelPrecoDTO dto : itensNivel50) {
			if (dto.getCodigoEan() != null) {
				porEan50.put(dto.getCodigoEan(), dto);
			}
		}

		return promocoes.stream()
				.filter(p144 -> p144.getCodigoEan() != null && porEan50.containsKey(p144.getCodigoEan()))
				.filter(p144 -> {
					BigDecimal a = p144.getPreco();
					BigDecimal b = porEan50.get(p144.getCodigoEan()).getPreco();
					if (a == null && b == null)
						return false;
					if (a == null || b == null)
						return true;
					return a.compareTo(b) != 0;
				}).map(p144 -> {
					ProdutoNivelPrecoDTO p50 = porEan50.get(p144.getCodigoEan());
					DivergenciaPrecoDTO d = new DivergenciaPrecoDTO();
					d.setLoja144(loja144);
					d.setNivel50(nivel50);
					d.setCodigoEan(p144.getCodigoEan());
					d.setCodigoProduto(p144.getCodigoProduto());
					d.setDescricao(p144.getDescricao());
					d.setEmbalagem(p144.getEmbalagem());
					d.setPreco144(p144.getPreco());
					d.setPreco50(p50.getPreco());
					if (d.getPreco144() != null && d.getPreco50() != null) {
						d.setDiferenca(d.getPreco144().subtract(d.getPreco50()));
					}
					// Datas da promoção (144)
					d.setDataInicial(p144.getDataInicial());
					d.setDataFinal(p144.getDataFinal());
					return d;
				}).collect(Collectors.toList());
	}

	@Override
	public byte[] gerarPlanilhaExcel(List<DivergenciaPrecoDTO> itens) {
		try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			Sheet sheet = wb.createSheet("Divergencias");

			String[] cols = { "loja144", "nivel50", "codigo_ean", "codigo_produto", "descricao", "embalagem",
					"data_inicial", "data_final", "preco_144", "preco_50", "diferenca" };
			Row header = sheet.createRow(0);
			for (int i = 0; i < cols.length; i++)
				header.createCell(i).setCellValue(cols[i]);

			int r = 1;
			for (DivergenciaPrecoDTO d : (itens == null ? List.<DivergenciaPrecoDTO>of() : itens)) {
				Row row = sheet.createRow(r++);
				row.createCell(0).setCellValue(d.getLoja144() == null ? "" : String.valueOf(d.getLoja144()));
				row.createCell(1).setCellValue(d.getNivel50() == null ? "" : String.valueOf(d.getNivel50()));
				row.createCell(2).setCellValue(d.getCodigoEan() == null ? "" : d.getCodigoEan());
				row.createCell(3)
						.setCellValue(d.getCodigoProduto() == null ? "" : String.valueOf(d.getCodigoProduto()));
				row.createCell(4).setCellValue(d.getDescricao() == null ? "" : d.getDescricao());
				row.createCell(5).setCellValue(d.getEmbalagem() == null ? "" : d.getEmbalagem());

				// Datas como texto formatado (mais simples e legível)
				row.createCell(6).setCellValue(d.getDataInicial() == null ? "" : d.getDataInicial().format(FMT));
				row.createCell(7).setCellValue(d.getDataFinal() == null ? "" : d.getDataFinal().format(FMT));

				// Preço 144 (numérico, em branco se null)
				Cell c6 = row.createCell(8);
				if (d.getPreco144() != null)
					c6.setCellValue(d.getPreco144().doubleValue());

				// Preço 50 (numérico, em branco se null)
				Cell c7 = row.createCell(9);
				if (d.getPreco50() != null)
					c7.setCellValue(d.getPreco50().doubleValue());

				// Diferença (numérico, em branco se null)
				Cell c8 = row.createCell(10);
				if (d.getDiferenca() != null)
					c8.setCellValue(d.getDiferenca().doubleValue());
			}

			for (int i = 0; i < cols.length; i++)
				sheet.autoSizeColumn(i);
			wb.write(bos);
			return bos.toByteArray();
		} catch (Exception e) {
			throw new RuntimeException("Erro ao gerar XLSX de divergências", e);
		}
	}
}
