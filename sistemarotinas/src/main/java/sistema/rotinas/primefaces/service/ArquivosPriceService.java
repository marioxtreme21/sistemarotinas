package sistema.rotinas.primefaces.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import sistema.rotinas.primefaces.model.ArquivosPrice;
import sistema.rotinas.primefaces.repository.ArquivosPriceRepository;
import sistema.rotinas.primefaces.service.interfaces.IArquivosPriceService;

@Service
public class ArquivosPriceService implements IArquivosPriceService {

    @Autowired
    private ArquivosPriceRepository repo;

    @Override
    @Transactional
    public ArquivosPrice save(ArquivosPrice price) {
        if (price.getLoja() == null || price.getLoja().getLojaId() == null) {
            throw new IllegalArgumentException("Loja é obrigatória.");
        }
        if (repo.existsByLoja_LojaId(price.getLoja().getLojaId()) && (price.getPriceId() == null)) {
            throw new IllegalArgumentException("Já existe configuração PRICE para esta loja.");
        }
        return repo.save(price);
    }

    @Override
    @Transactional
    public ArquivosPrice update(ArquivosPrice price) {
        if (price.getPriceId() == null) throw new IllegalArgumentException("ID obrigatório para update.");
        return repo.save(price);
    }

    @Override
    @Transactional
    public ArquivosPrice findById(Long id) {
        return repo.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        repo.deleteById(id);
    }

    @Override
    @Transactional
    public List<ArquivosPrice> findAll() {
        return repo.findAll();
    }

    @Override
    @Transactional
    public ArquivosPrice findByLojaId(Long lojaId) {
        return repo.findByLoja_LojaId(lojaId).orElse(null);
    }

    @Override
    @Transactional
    public boolean existsByLojaId(Long lojaId) {
        return repo.existsByLoja_LojaId(lojaId);
    }
}
