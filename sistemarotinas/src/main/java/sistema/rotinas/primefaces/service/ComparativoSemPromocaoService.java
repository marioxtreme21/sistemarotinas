package sistema.rotinas.primefaces.service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
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

import sistema.rotinas.primefaces.dto.ProdutoNivelPrecoDTO;
import sistema.rotinas.primefaces.dto.ResultadoSemPromocaoDTO;
import sistema.rotinas.primefaces.repository.NivelPreco144Repository;
import sistema.rotinas.primefaces.repository.ProdutoNivelPrecoRepository;
import sistema.rotinas.primefaces.service.interfaces.IComparativoSemPromocaoService;

@Service
public class ComparativoSemPromocaoService implements IComparativoSemPromocaoService {

	private final NivelPreco144Repository nivel144Repository;
	private final ProdutoNivelPrecoRepository nivel50Repository;

	public ComparativoSemPromocaoService(NivelPreco144Repository nivel144Repository,
			ProdutoNivelPrecoRepository nivel50Repository) {
		this.nivel144Repository = nivel144Repository;
		this.nivel50Repository = nivel50Repository;
	}

	@Override
	public List<ResultadoSemPromocaoDTO> comparar(int loja144, int nivel50, LocalDate dataReferencia) {

		// 1) 144 (nível) sem promoção vigente na data
		List<ProdutoNivelPrecoDTO> base144 = nivel144Repository.listarSemPromocaoVigente(loja144, dataReferencia);

		// 2) 50 (nível)
		List<ProdutoNivelPrecoDTO> base50 = nivel50Repository.buscarNo50(nivel50);

		// Índice por EAN da base 50
		Map<String, ProdutoNivelPrecoDTO> porEan50 = new LinkedHashMap<>();
		for (ProdutoNivelPrecoDTO dto : base50) {
			if (dto.getCodigoEan() != null) {
				porEan50.put(dto.getCodigoEan(), dto);
			}
		}

		// 3) Monta resultado: NAO_LOCALIZADO ou DIVERGENTE
		return base144.stream().flatMap(item144 -> {
			ProdutoNivelPrecoDTO item50 = porEan50.get(item144.getCodigoEan());
			if (item50 == null) {
				// Não localizado
				ResultadoSemPromocaoDTO r = new ResultadoSemPromocaoDTO();
				r.setLoja144(loja144);
				r.setNivel50(nivel50);
				r.setCodigoEan(item144.getCodigoEan());
				r.setCodigoProduto(item144.getCodigoProduto());
				r.setDescricao(item144.getDescricao());
				r.setTipoEmbalagem(item144.getEmbalagem());
				r.setPreco144(item144.getPreco());
				r.setStatus("NAO_LOCALIZADO");
				return java.util.stream.Stream.of(r);
			} else {
				// Localizado -> checa divergência
				BigDecimal a = item144.getPreco();
				BigDecimal b = item50.getPreco();
				boolean diverge = (a == null && b != null) || (a != null && b == null)
						|| (a != null && b != null && a.compareTo(b) != 0);

				if (diverge) {
					ResultadoSemPromocaoDTO r = new ResultadoSemPromocaoDTO();
					r.setLoja144(loja144);
					r.setNivel50(nivel50);
					r.setCodigoEan(item144.getCodigoEan());
					r.setCodigoProduto(item144.getCodigoProduto());
					r.setDescricao(item144.getDescricao());
					r.setTipoEmbalagem(item144.getEmbalagem());
					r.setPreco144(a);
					r.setPreco50(b);
					if (a != null && b != null)
						r.setDiferenca(a.subtract(b));
					r.setStatus("DIVERGENTE");
					return java.util.stream.Stream.of(r);
				}
				// Se não divergir, não entra no resultado
				return java.util.stream.Stream.empty();
			}
		}).collect(Collectors.toList());
	}

	@Override
	public byte[] gerarPlanilhaExcel(List<ResultadoSemPromocaoDTO> itens) {
		try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			Sheet sheet = wb.createSheet("NaoPromo_144_vs_50");

			String[] cols = { "loja144", "nivel50", "codigo_ean", "codigo_produto", "descricao", "tipo_embalagem",
					"preco_144", "preco_50", "diferenca", "status" };
			Row header = sheet.createRow(0);
			for (int i = 0; i < cols.length; i++)
				header.createCell(i).setCellValue(cols[i]);

			int r = 1;
			for (ResultadoSemPromocaoDTO d : (itens == null ? List.<ResultadoSemPromocaoDTO>of() : itens)) {
				Row row = sheet.createRow(r++);
				row.createCell(0).setCellValue(d.getLoja144() == null ? "" : String.valueOf(d.getLoja144()));
				row.createCell(1).setCellValue(d.getNivel50() == null ? "" : String.valueOf(d.getNivel50()));
				row.createCell(2).setCellValue(d.getCodigoEan() == null ? "" : d.getCodigoEan());
				row.createCell(3)
						.setCellValue(d.getCodigoProduto() == null ? "" : String.valueOf(d.getCodigoProduto()));
				row.createCell(4).setCellValue(d.getDescricao() == null ? "" : d.getDescricao());
				row.createCell(5).setCellValue(d.getTipoEmbalagem() == null ? "" : d.getTipoEmbalagem());

				Cell c6 = row.createCell(6);
				if (d.getPreco144() != null)
					c6.setCellValue(d.getPreco144().doubleValue());

				Cell c7 = row.createCell(7);
				if (d.getPreco50() != null)
					c7.setCellValue(d.getPreco50().doubleValue());

				Cell c8 = row.createCell(8);
				if (d.getDiferenca() != null)
					c8.setCellValue(d.getDiferenca().doubleValue());

				row.createCell(9).setCellValue(d.getStatus() == null ? "" : d.getStatus());
			}

			for (int i = 0; i < cols.length; i++)
				sheet.autoSizeColumn(i);
			wb.write(bos);
			return bos.toByteArray();
		} catch (Exception e) {
			throw new RuntimeException("Erro ao gerar XLSX (Sem Promoção 144 x 50)", e);
		}
	}
}
