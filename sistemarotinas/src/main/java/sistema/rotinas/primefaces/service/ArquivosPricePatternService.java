package sistema.rotinas.primefaces.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import sistema.rotinas.primefaces.model.ArquivosPricePattern;
import sistema.rotinas.primefaces.repository.ArquivosPricePatternRepository;
import sistema.rotinas.primefaces.service.interfaces.IArquivosPricePatternService;

@Service
public class ArquivosPricePatternService implements IArquivosPricePatternService {

    @Autowired
    private ArquivosPricePatternRepository repo;

    @Override
    @Transactional
    public ArquivosPricePattern save(ArquivosPricePattern pattern) {
        if (pattern.getPrice() == null || pattern.getPrice().getPriceId() == null) {
            throw new IllegalArgumentException("Referência a ArquivosPrice é obrigatória.");
        }
        return repo.save(pattern);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        repo.deleteById(id);
    }

    @Override
    @Transactional
    public List<ArquivosPricePattern> listarPorPrice(Long priceId) {
        return repo.findByPrice_PriceIdOrderByPatternAsc(priceId);
    }

    @Override
    @Transactional
    public int countObrigatorios(Long priceId) {
        return repo.countByPrice_PriceIdAndRequiredTrue(priceId);
    }
}
