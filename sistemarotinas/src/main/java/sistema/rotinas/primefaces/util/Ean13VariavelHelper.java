package sistema.rotinas.primefaces.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class Ean13VariavelHelper {

	private Ean13VariavelHelper() {
	}

	/**
	 * Monta EAN-13 variável: prefixo2(2) + plu5(5) + valor5(5) + DV(1) - valor5 =
	 * preço (ex.: preço/kg) com 'decimals' casas (normalmente 2).
	 */
	public static String build(String prefix2, String plu5, BigDecimal valor, int decimals) {
		String pref = normalizePrefix(prefix2); // 2 dígitos
		String plu = normalizePlu(plu5); // 5 dígitos (mantém se já vier 5)
		String v5 = to5Digits(valor, decimals); // 5 dígitos
		String first12 = pref + plu + v5; // 12 dígitos
		return first12 + calcDv(first12);
	}

	/** Dígito verificador EAN-13 (módulo 10). */
	public static char calcDv(String first12) {
		if (first12 == null || !first12.matches("\\d{12}")) {
			throw new IllegalArgumentException("EAN-13 requer base com 12 dígitos");
		}
		int sum = 0;
		for (int i = 0; i < 12; i++) {
			int d = first12.charAt(i) - '0';
			sum += (i % 2 == 0) ? d : d * 3;
		}
		int mod = sum % 10;
		int dv = (10 - mod) % 10;
		return (char) ('0' + dv);
	}

	// --------- helpers ---------

	private static String normalizePrefix(String p) {
		String d = (p == null ? "" : p.replaceAll("\\D", ""));
		if (d.length() < 2)
			d = ("00" + d);
		return d.substring(d.length() - 2);
	}

	/**
	 * PLU com 5 dígitos. Regra legada: se vier com 4, acrescenta '0' no fim (ex.:
	 * 8862 -> 88620). Se já vier com 5 (ex.: "07600"), mantém.
	 */
	public static String normalizePlu(String s) {
		String d = (s == null ? "" : s.replaceAll("\\D", ""));
		if (d.length() == 4)
			d = d + "0";
		if (d.length() < 5)
			d = ("00000" + d);
		return d.substring(d.length() - 5);
	}

	/** Extrai o PLU de 4 dígitos (últimos 4 dígitos da string). */
	public static String extractPlu4(String any) {
		String d = (any == null ? "" : any.replaceAll("\\D", ""));
		if (d.length() < 4)
			d = ("0000" + d);
		return d.substring(d.length() - 4);
	}

	/**
	 * Regra Toledo para formar o PLU5 usado no EAN variável: PLU(4) -> usar os
	 * **últimos 3 dígitos + "00"**. Ex.: 1215 -> 21500; 1076 -> 07600; 8862 ->
	 * 86200.
	 */
	public static String normalizePluToledoLast3Plus00(String any) {
		String d = (any == null ? "" : any.replaceAll("\\D", ""));
		String last3 = (d.length() >= 3) ? d.substring(d.length() - 3)
				: ("000" + d).substring(("000" + d).length() - 3);
		return last3 + "00"; // total 5 dígitos
	}

	private static String to5Digits(BigDecimal n, int decimals) {
		BigDecimal v = Objects.requireNonNullElse(n, BigDecimal.ZERO);
		BigDecimal scale = BigDecimal.TEN.pow(decimals);
		long raw = v.multiply(scale).setScale(0, RoundingMode.HALF_UP).longValue();
		if (raw < 0)
			raw = 0;
		if (raw > 99999)
			raw = 99999;
		return String.format("%05d", raw);
	}
}
