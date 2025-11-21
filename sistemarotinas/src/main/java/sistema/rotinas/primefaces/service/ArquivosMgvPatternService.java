package sistema.rotinas.primefaces.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import sistema.rotinas.primefaces.model.ArquivosMgvPattern;
import sistema.rotinas.primefaces.repository.ArquivosMgvPatternRepository;
import sistema.rotinas.primefaces.service.interfaces.IArquivosMgvPatternService;

@Service
public class ArquivosMgvPatternService implements IArquivosMgvPatternService {

    @Autowired
    private ArquivosMgvPatternRepository repo;

    @Override
    @Transactional
    public ArquivosMgvPattern save(ArquivosMgvPattern pattern) {
        if (pattern.getMgv() == null || pattern.getMgv().getMgvId() == null) {
            throw new IllegalArgumentException("Referência a ArquivosMgv é obrigatória.");
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
    public List<ArquivosMgvPattern> listarPorMgv(Long mgvId) {
        return repo.findByMgv_MgvIdOrderByPatternAsc(mgvId);
    }

    @Override
    @Transactional
    public int countObrigatorios(Long mgvId) {
        return repo.countByMgv_MgvIdAndRequiredTrue(mgvId);
    }
}
