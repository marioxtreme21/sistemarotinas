package sistema.rotinas.primefaces.repository;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ItemRmsRepositoryImpl implements ItemRmsRepository {

	private final JdbcTemplate oracleRms;
	private static final int IN_CHUNK = 900; // segurança para listas grandes no IN

	public ItemRmsRepositoryImpl(@Qualifier("oracleRmsJdbcTemplate") JdbcTemplate oracleRms) {
		this.oracleRms = oracleRms;
	}

	@Override
	public Map<Integer, String> buscarSkuNovoPorSkus(List<Integer> skus) {
		Map<Integer, String> mapa = new HashMap<>();
		if (skus == null || skus.isEmpty())
			return mapa;

		List<Integer> lista = skus.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
		int i = 0;
		while (i < lista.size()) {
			int end = Math.min(i + IN_CHUNK, lista.size());
			List<Integer> slice = lista.subList(i, end);

			String placeholders = String.join(",", Collections.nCopies(slice.size(), "?"));
			String sql = "SELECT git_cod_item AS codigo, git_digito AS digito "
					+ "FROM aa3citem WHERE git_cod_item IN (" + placeholders + ")";

			oracleRms.query(sql, slice.toArray(), rs -> {
				BigDecimal cod = rs.getBigDecimal("codigo");
				BigDecimal dig = rs.getBigDecimal("digito");
				if (cod != null && dig != null) {
					mapa.put(cod.intValue(), cod.toPlainString() + dig.toPlainString()); // concat sem separador
				} else if (cod != null) {
					mapa.put(cod.intValue(), cod.toPlainString());
				}
			});

			i = end;
		}
		return mapa;
	}
}
