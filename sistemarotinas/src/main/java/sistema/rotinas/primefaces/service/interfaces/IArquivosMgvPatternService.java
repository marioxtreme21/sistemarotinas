package sistema.rotinas.primefaces.service.interfaces;

import java.util.List;

import sistema.rotinas.primefaces.model.ArquivosMgvPattern;

public interface IArquivosMgvPatternService {

    ArquivosMgvPattern save(ArquivosMgvPattern pattern);

    void deleteById(Long id);

    List<ArquivosMgvPattern> listarPorMgv(Long mgvId);

    int countObrigatorios(Long mgvId);
}
