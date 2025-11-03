package sistema.rotinas.primefaces.bean;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import sistema.rotinas.primefaces.dto.NivelPrecoProdutoDTO;
import sistema.rotinas.primefaces.lazy.CarregamentoLazyListForObject;
import sistema.rotinas.primefaces.service.interfaces.INivelPrecoProdutoService;
import sistema.rotinas.primefaces.util.Ean13VariavelHelper;

@Component
@Named
@SessionScoped
public class BarcodeBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger log = LoggerFactory.getLogger(BarcodeBean.class);

	@Autowired
	private INivelPrecoProdutoService nivelPrecoProdutoService;

	private List<NivelPrecoProdutoDTO> itens;
	private int idx = 0;

	private CarregamentoLazyListForObject<NivelPrecoProdutoDTO> itensLazy;

	private int codigoNivel = 13;
	private boolean mostrarPainel = true;

	// ===== modo de consulta (qual SELECT executar) =====
	/** Opções: "TODOS" (BASE_SQL), "LISTA_A", "LISTA_B", "LISTA_C" */
	private String modoConsulta = "TODOS";

	/** Listas de e.codigo_produto para cada opção */
	private List<Long> listaA = List.of(2017300L, 2017296L, 1677411L, 1791818L, 2192624L, 2127075L, 1965670L, 1852043L,
			1852019L, 1501356L, 1501348L, 2061481L, 2196735L, 2196743L, 1851454L, 1917943L, 1851462L, 2134950L,
			2150867L, 2195151L, 2134942L, 1831763L, 2026422L, 2160447L, 2186489L, 1971808L, 2160455L, 1972073L,
			1902571L, 1902555L, 1902563L, 149845L, 95206L, 1826786L, 1826794L, 2056470L, 2056445L, 2056437L, 2056453L,
			1968157L, 1969072L, 1968190L, 1968165L, 92509L, 2161494L, 2184885L, 2184931L, 2184923L, 2184907L, 2184893L,
			2195380L, 2184915L, 2180120L, 2157870L, 1298275L, 2157888L, 1836676L, 2110130L, 2110164L, 21610L, 850845L,
			1773399L, 443921L, 2051036L, 2051044L, 2195585L, 2196026L, 2195631L, 2195607L, 1734105L, 2185156L, 2185164L,
			2066483L, 2201267L, 2201275L, 420727L);

	private List<Long> listaB = List.of(1277979L, 2194724L, 2130718L, 2170175L, 2108453L, 2080591L, 2080109L, 2108461L,
			2024438L, 2024411L, 2127784L, 2127776L, 2156032L, 2156059L, 2156040L, 2168090L, 2189542L, 2159279L,
			2159287L, 2015129L, 1966928L, 2103001L, 2059320L, 2170493L, 2170477L, 2067935L, 2189550L, 2189569L,
			2189577L, 1816403L, 1816390L, 2196204L, 91138L, 1848844L, 2031680L, 2139898L, 2139901L, 1810804L, 1810790L,
			2005603L, 1928597L, 2049988L, 1678663L, 1921657L, 1921649L, 1949420L, 2067463L, 2067471L, 1963295L,
			2015110L, 118010L, 534420L, 2133636L, 1293672L, 2095157L, 13480L, 20419L, 56065L, 2189771L, 2189780L,
			2189763L, 85430L, 75833L, 12726L, 25941L, 25925L, 25933L, 2015145L, 2015196L, 2188260L, 493325L, 1657259L,
			1657305L, 2031647L, 2015161L, 2015170L, 2015099L, 2018748L, 2194880L, 2194902L, 2083957L, 2018713L,
			2194910L, 2018730L, 2018705L, 2018691L, 2018683L, 1933825L, 1933817L, 44172L, 1910876L, 2195011L, 1889559L,
			1889540L);

	private List<Long> listaC = List.of(2195135L, 2195178L, 2177153L, 2195143L, 2195160L, 2033941L, 2033933L, 2006090L,
			2033917L, 2064693L, 2033925L, 2033879L, 2134934L, 2134926L, 2177129L, 2061392L, 446173L, 1689355L, 584070L,
			584053L, 584061L, 2192268L, 2192241L, 1968084L, 1979639L, 1944223L);

	// ===== Configurações de pesáveis =====
	private String prefixoPesavelDefault = "21";
	private int casasPreco = 2; // preço em centavos
	private int loteTamanho = 400;
	private int loteIndice = 0;

	private final Map<String, String> prefixoPorPlu = Map.of("1215", "21", "1076", "21", "8862", "28");

	private final Map<String, BigDecimal> valorOverridePorPlu = Map.of(
	// "8862", new BigDecimal("18.10")
	);

	@PostConstruct
	public void init() {
		log.info("Inicializando BarcodeBean");
		carregarItensSobDemanda();
		carregarRotacao();
		if (itens == null || itens.isEmpty()) {
			// construtor LEGADO do DTO ainda funciona
			itens = List.of(new NivelPrecoProdutoDTO(13, "7890123456789", Boolean.FALSE, "UN", null));
		}
	}

	public void carregarRotacao() {
	    try {
	        log.info("carregarRotacao() -> modoConsulta={}, nivel={}", modoConsulta, codigoNivel);

	        switch (modoConsulta) {
	            case "LISTA_A":
	                log.info("Usando LISTA_A com {} códigos", listaA.size());
	                this.itens = nivelPrecoProdutoService
	                        .listarPorNivelComProdutosOrdenadoPorDescricao(codigoNivel, listaA);
	                break;
	            case "LISTA_B":
	                log.info("Usando LISTA_B com {} códigos", listaB.size());
	                this.itens = nivelPrecoProdutoService
	                        .listarPorNivelComProdutosOrdenadoPorDescricao(codigoNivel, listaB);
	                break;
	            case "LISTA_C":
	                log.info("Usando LISTA_C com {} códigos", listaC.size());
	                this.itens = nivelPrecoProdutoService
	                        .listarPorNivelComProdutosOrdenadoPorDescricao(codigoNivel, listaC);
	                break;
	            case "TODOS":
	            default:
	                log.info("Usando BASE_SQL (todos)");
	                this.itens = nivelPrecoProdutoService.listarPorNivel(codigoNivel);
	                break;
	        }

	        this.idx = 0;
	        log.info("Retornou {} itens no modo {}", (itens == null ? 0 : itens.size()), modoConsulta);

	        if (itens == null || itens.isEmpty()) {
	            FacesContext.getCurrentInstance().addMessage(
	                null, new FacesMessage(FacesMessage.SEVERITY_WARN,
	                    "Sem resultados",
	                    "A consulta selecionada não retornou itens para o nível " + codigoNivel)
	            );
	        }

	    } catch (Exception e) {
	        log.error("Erro ao carregar lista de rotação", e);
	        FacesContext.getCurrentInstance().addMessage(
	            null,
	            new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Não foi possível carregar a lista de códigos.")
	        );
	    }
	}

	/** chamado pelo selectOneMenu (via p:ajax) */
	public void onModoConsultaChange() {
		refresh();
	}

	public void carregarItensSobDemanda() {
		itensLazy = new CarregamentoLazyListForObject<>(
				(first, pageSize) -> nivelPrecoProdutoService.listarPorNivel(codigoNivel, first, pageSize, null, true),
				() -> nivelPrecoProdutoService.countPorNivel(codigoNivel));
	}

	public void proximo() {
		if (itens == null || itens.isEmpty())
			return;
		idx = (idx + 1) % itens.size();
	}

	public void refresh() {
		carregarRotacao();
		carregarItensSobDemanda();
		loteIndice = 0;
		FacesContext.getCurrentInstance().addMessage(null,
				new FacesMessage(FacesMessage.SEVERITY_INFO, "Atualizado", "Lista recarregada com sucesso."));
	}

	public void aplicarNivel() {
		refresh();
		this.mostrarPainel = true;
	}

	// ===== Regras de pesável =====
	private boolean isPesavelKg(NivelPrecoProdutoDTO it) {
		return Boolean.TRUE.equals(it.getPesavel()) && isEmbalagemKg(it.getEmbalagem());
	}

	private boolean isEmbalagemKg(String embalagem) {
		if (embalagem == null)
			return false;
		String lettersOnly = embalagem.toUpperCase(Locale.ROOT).replaceAll("[^A-Z]", "");
		return lettersOnly.equals("KG");
	}

	private String pickPrefixForPlu(String plu4) {
		String manual = prefixoPorPlu.get(plu4);
		if (manual != null)
			return manual;

		if (plu4 != null && plu4.length() == 4 && Character.isDigit(plu4.charAt(0))) {
			char milhar = plu4.charAt(0);
			if (milhar >= '1' && milhar <= '9') {
				return "2" + milhar; // 1->21, 2->22, ..., 9->29
			}
		}
		return prefixoPesavelDefault;
	}

	private BigDecimal pickValorOverride(String plu4) {
		return valorOverridePorPlu.get(plu4);
	}

	/** Gera o valor exibido (EAN-13 variável ou EAN fixo). */
	public String gerarCodigoExibicao(NivelPrecoProdutoDTO it) {
		if (it == null)
			return "";
		// usa o EAN (n.codigo_produto -> alias codigoEan)
		String raw = it.getCodigoEan() == null ? "" : it.getCodigoEan().trim();

		if (isPesavelKg(it)) {
			String plu4 = Ean13VariavelHelper.extractPlu4(raw);
			String plu5 = Ean13VariavelHelper.normalizePluToledoLast3Plus00(plu4);
			String prefix = pickPrefixForPlu(plu4);

			BigDecimal valor = pickValorOverride(plu4);
			if (valor == null)
				valor = it.getPreco();

			try {
				return Ean13VariavelHelper.build(prefix, plu5, valor, casasPreco);
			} catch (Exception ex) {
				log.warn("Falha ao gerar EAN-13 variável (PLU {} / raw '{}'): {}", plu4, raw, ex.toString());
				return raw;
			}
		}
		return raw; // não-pesáveis: usa o EAN original
	}

	// ===== Lógica de lotes =====
	private List<String> getTodosOsCodigos() {
		if (itens == null)
			return List.of();
		return itens.stream().map(this::gerarCodigoExibicao).filter(s -> s != null && !s.isBlank())
				.collect(Collectors.toList());
	}

	/**
	 * Subconjunto do lote atual (para exportar metadados combinando com os códigos
	 * exibidos).
	 */
	private List<NivelPrecoProdutoDTO> getItensDoLote() {
		if (itens == null)
			return List.of();
		int start = Math.min(loteIndice * loteTamanho, itens.size());
		int end = Math.min(start + loteTamanho, itens.size());
		return itens.subList(start, end);
	}

	/** JSON paralelo aos códigos exibidos: e.codigo_produto (código interno). */
	public String getProdutosParaTelaJson() {
		var lst = getItensDoLote().stream().map(NivelPrecoProdutoDTO::getCodigoProduto).map(s -> s == null ? "" : s)
				.collect(Collectors.toList());
		return toJsonArray(lst);
	}

	/** JSON paralelo aos códigos exibidos: n.codigo_produto (EAN/PLU). */
	public String getEansParaTelaJson() {
		var lst = getItensDoLote().stream().map(NivelPrecoProdutoDTO::getCodigoEan).map(s -> s == null ? "" : s)
				.collect(Collectors.toList());
		return toJsonArray(lst);
	}

	/** JSON paralelo aos códigos exibidos: descricao (p.descricao). */
	public String getDescricoesParaTelaJson() {
		var lst = getItensDoLote().stream().map(it -> it.getDescricao() == null ? "" : it.getDescricao())
				.collect(Collectors.toList());
		return toJsonArray(lst);
	}

	/** JSON paralelo aos códigos exibidos: preço (BigDecimal.toPlainString). */
	public String getPrecosParaTelaJson() {
		var lst = getItensDoLote().stream().map(it -> it.getPreco() == null ? "" : it.getPreco().toPlainString())
				.collect(Collectors.toList());
		return toJsonArray(lst);
	}

	private String toJsonArray(List<String> lst) {
		StringBuilder sb = new StringBuilder("[");
		for (int i = 0; i < lst.size(); i++) {
			String s = lst.get(i).replace("\\", "\\\\").replace("\"", "\\\"");
			if (i > 0)
				sb.append(',');
			sb.append('"').append(s).append('"');
		}
		return sb.append(']').toString();
	}

	public List<String> getCodigosParaTela() {
		List<String> full = getTodosOsCodigos();
		if (full.isEmpty())
			return full;

		int start = Math.min(loteIndice * loteTamanho, full.size());
		int end = Math.min(start + loteTamanho, full.size());
		return full.subList(start, end);
	}

	public String getCodigosParaTelaJson() {
		var lst = getCodigosParaTela();
		StringBuilder sb = new StringBuilder("[");
		for (int i = 0; i < lst.size(); i++) {
			String s = lst.get(i).replace("\\", "\\\\").replace("\"", "\\\"");
			if (i > 0)
				sb.append(',');
			sb.append('"').append(s).append('"');
		}
		return sb.append(']').toString();
	}

	public int getTotalItens() {
		return getTodosOsCodigos().size();
	}

	public int getTotalLotes() {
		int total = getTotalItens();
		return total == 0 ? 0 : ((total + loteTamanho - 1) / loteTamanho);
	}

	public void proximoLote() {
		int totalLotes = getTotalLotes();
		if (totalLotes <= 0) {
			loteIndice = 0;
			return;
		}
		loteIndice = (loteIndice + 1) % totalLotes;
		FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Lote",
				"Avançou para o lote " + (loteIndice + 1) + "/" + totalLotes));
	}

	public void loteAnterior() {
		int totalLotes = getTotalLotes();
		if (totalLotes <= 0) {
			loteIndice = 0;
			return;
		}
		loteIndice = (loteIndice - 1 + totalLotes) % totalLotes;
		FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Lote",
				"Voltou para o lote " + (loteIndice + 1) + "/" + totalLotes));
	}

	// ===== Getters/Setters =====
	public List<NivelPrecoProdutoDTO> getItens() {
		return itens;
	}

	public int getIdx() {
		return idx;
	}

	public void setIdx(int idx) {
		this.idx = idx;
	}

	public CarregamentoLazyListForObject<NivelPrecoProdutoDTO> getItensLazy() {
		return itensLazy;
	}

	public void setItensLazy(CarregamentoLazyListForObject<NivelPrecoProdutoDTO> itensLazy) {
		this.itensLazy = itensLazy;
	}

	public int getCodigoNivel() {
		return codigoNivel;
	}

	public void setCodigoNivel(int codigoNivel) {
		this.codigoNivel = codigoNivel;
	}

	public boolean isMostrarPainel() {
		return mostrarPainel;
	}

	public void setMostrarPainel(boolean mostrarPainel) {
		this.mostrarPainel = mostrarPainel;
	}

	public String getPrefixoPesavelDefault() {
		return prefixoPesavelDefault;
	}

	public void setPrefixoPesavelDefault(String prefixoPesavelDefault) {
		this.prefixoPesavelDefault = prefixoPesavelDefault;
	}

	public int getCasasPreco() {
		return casasPreco;
	}

	public void setCasasPreco(int casasPreco) {
		this.casasPreco = casasPreco;
	}

	public int getLoteTamanho() {
		return loteTamanho;
	}

	public void setLoteTamanho(int loteTamanho) {
		this.loteTamanho = Math.max(1, loteTamanho);
	}

	public int getLoteIndice() {
		return loteIndice;
	}

	public void setLoteIndice(int loteIndice) {
		this.loteIndice = Math.max(0, loteIndice);
	}

	public String getModoConsulta() {
		return modoConsulta;
	}

	public void setModoConsulta(String modoConsulta) {
		this.modoConsulta = modoConsulta;
	}

	public List<Long> getListaA() {
		return listaA;
	}

	public void setListaA(List<Long> listaA) {
		this.listaA = listaA;
	}

	public List<Long> getListaB() {
		return listaB;
	}

	public void setListaB(List<Long> listaB) {
		this.listaB = listaB;
	}

	public List<Long> getListaC() {
		return listaC;
	}

	public void setListaC(List<Long> listaC) {
		this.listaC = listaC;
	}
}
