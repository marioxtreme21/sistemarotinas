package sistema.rotinas.primefaces.service.interfaces;

import java.util.List;

import sistema.rotinas.primefaces.model.ArquivosPricePattern;

public interface IArquivosPricePatternService {

    ArquivosPricePattern save(ArquivosPricePattern pattern);

    void deleteById(Long id);

    List<ArquivosPricePattern> listarPorPrice(Long priceId);

    int countObrigatorios(Long priceId);
}
