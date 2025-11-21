package sistema.rotinas.primefaces.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import sistema.rotinas.primefaces.model.ArquivosMgv;
import sistema.rotinas.primefaces.repository.ArquivosMgvRepository;
import sistema.rotinas.primefaces.service.interfaces.IArquivosMgvService;

@Service
public class ArquivosMgvService implements IArquivosMgvService {

    @Autowired
    private ArquivosMgvRepository repo;

    @Override
    @Transactional
    public ArquivosMgv save(ArquivosMgv mgv) {
        if (mgv.getLoja() == null || mgv.getLoja().getLojaId() == null) {
            throw new IllegalArgumentException("Loja é obrigatória.");
        }
        if (repo.existsByLoja_LojaId(mgv.getLoja().getLojaId()) && (mgv.getMgvId() == null)) {
            throw new IllegalArgumentException("Já existe configuração MGV para esta loja.");
        }
        return repo.save(mgv);
    }

    @Override
    @Transactional
    public ArquivosMgv update(ArquivosMgv mgv) {
        if (mgv.getMgvId() == null) throw new IllegalArgumentException("ID obrigatório para update.");
        return repo.save(mgv);
    }

    @Override
    @Transactional
    public ArquivosMgv findById(Long id) {
        return repo.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        repo.deleteById(id);
    }

    @Override
    @Transactional
    public List<ArquivosMgv> findAll() {
        return repo.findAll();
    }

    @Override
    @Transactional
    public ArquivosMgv findByLojaId(Long lojaId) {
        return repo.findByLoja_LojaId(lojaId).orElse(null);
    }

    @Override
    @Transactional
    public boolean existsByLojaId(Long lojaId) {
        return repo.existsByLoja_LojaId(lojaId);
    }
}
