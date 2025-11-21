package sistema.rotinas.primefaces.service.interfaces;

import java.util.List;

import sistema.rotinas.primefaces.model.ArquivosMgv;

public interface IArquivosMgvService {

    ArquivosMgv save(ArquivosMgv mgv);

    ArquivosMgv update(ArquivosMgv mgv);

    ArquivosMgv findById(Long id);

    void deleteById(Long id);

    List<ArquivosMgv> findAll();

    ArquivosMgv findByLojaId(Long lojaId);

    boolean existsByLojaId(Long lojaId);
}
