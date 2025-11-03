package sistema.rotinas.primefaces.lazy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;


public class CarregamentoLazyListForObject<T> extends LazyDataModel<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<T> list = new ArrayList<>();
    private int totalRegistroConsulta = 0;
    private String query;

    private final BiFunction<Integer, Integer, List<T>> fetchFunction;
    private final Supplier<Integer> countFunction;

    private LazyFilterFunction<T> fetchWithFilterFunctionV2; // Com ordenação
    private LazyFilterFunctionLegacy<T> fetchWithFilterFunctionLegacy; // Sem ordenação
    private LazyFilterCountFunction countWithFilterFunction;

    private boolean usaFiltro = false;

    // ✅ Versão básica sem filtros
    public CarregamentoLazyListForObject(BiFunction<Integer, Integer, List<T>> fetchFunction,
                                         Supplier<Integer> countFunction) {
        this.fetchFunction = fetchFunction;
        this.countFunction = countFunction;
    }

    // ✅ Filtros sem suporte à ordenação (legado)
    public CarregamentoLazyListForObject(LazyFilterFunctionLegacy<T> fetchWithFilterFunctionLegacy,
                                         Supplier<Integer> countFunction) {
        this.fetchFunction = null;
        this.fetchWithFilterFunctionLegacy = fetchWithFilterFunctionLegacy;
        this.countFunction = countFunction;
        this.usaFiltro = true;
    }

    // ✅ Filtros com suporte a ordenação
    public CarregamentoLazyListForObject(LazyFilterFunction<T> fetchWithFilterFunctionV2,
                                         LazyFilterCountFunction countWithFilterFunction) {
        this.fetchFunction = null;
        this.countFunction = null;
        this.fetchWithFilterFunctionV2 = fetchWithFilterFunctionV2;
        this.countWithFilterFunction = countWithFilterFunction;
        this.usaFiltro = true;
    }

    @Override
    public int count(Map<String, FilterMeta> filterBy) {
        if (usaFiltro && countWithFilterFunction != null) {
            totalRegistroConsulta = countWithFilterFunction.count(filterBy);
        } else if (countFunction != null) {
            totalRegistroConsulta = countFunction.get();
        }
        return totalRegistroConsulta;
    }

    @Override
    public List<T> load(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
        try {
            String sortField = null;
            boolean ascendente = true;

            if (sortBy != null && !sortBy.isEmpty()) {
                Map.Entry<String, SortMeta> entry = sortBy.entrySet().iterator().next();
                sortField = entry.getValue().getField();
                ascendente = entry.getValue().getOrder().isAscending();
            }

            if (usaFiltro) {
                if (fetchWithFilterFunctionV2 != null) {
                    list = fetchWithFilterFunctionV2.apply(first, pageSize, filterBy, sortField, ascendente);
                } else if (fetchWithFilterFunctionLegacy != null) {
                    list = fetchWithFilterFunctionLegacy.apply(first, pageSize, filterBy);
                }
            } else {
                list = fetchFunction.apply(first, pageSize);
            }

            setRowCount(totalRegistroConsulta > 0 ? totalRegistroConsulta : count(filterBy));
            setPageSize(pageSize);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public void setTotalRegistroConsulta(int totalRegistroConsulta, String queryDeBuscaConsulta) {
        this.query = queryDeBuscaConsulta;
        this.totalRegistroConsulta = totalRegistroConsulta;
    }

    public List<T> getList() {
        return list;
    }

    public void clean() {
        this.query = null;
        this.totalRegistroConsulta = 0;
        this.list.clear();
    }

    public void remove(T objetoSelecionado) {
        this.list.remove(objetoSelecionado);
    }

    public void add(T objetoSelecionado) {
        this.list.add(objetoSelecionado);
    }

    public void addAll(List<T> collections) {
        this.list.addAll(collections);
    }

   

   

    // 🧠 NOVO - Suporta sortField e ascendente
    @FunctionalInterface
    public static interface LazyFilterFunction<T> extends Serializable {
        List<T> apply(int first, int pageSize, Map<String, FilterMeta> filterBy, String sortField, boolean ascendente);
    }

    // 🔙 LEGADO - Suporta apenas filtros
    @FunctionalInterface
    public static interface LazyFilterFunctionLegacy<T> extends Serializable {
        List<T> apply(int first, int pageSize, Map<String, FilterMeta> filterBy);
    }

    @FunctionalInterface
    public static interface LazyFilterCountFunction extends Serializable {
        int count(Map<String, FilterMeta> filterBy);
    }
}
