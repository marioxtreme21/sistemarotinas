package sistema.rotinas.primefaces.service.interfaces;

import java.util.List;

import sistema.rotinas.primefaces.model.ArquivosPrice;

public interface IArquivosPriceService {

    ArquivosPrice save(ArquivosPrice price);

    ArquivosPrice update(ArquivosPrice price);

    ArquivosPrice findById(Long id);

    void deleteById(Long id);

    List<ArquivosPrice> findAll();

    ArquivosPrice findByLojaId(Long lojaId);

    boolean existsByLojaId(Long lojaId);
}
