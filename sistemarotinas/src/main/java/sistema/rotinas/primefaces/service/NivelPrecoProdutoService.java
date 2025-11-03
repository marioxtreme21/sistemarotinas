package sistema.rotinas.primefaces.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import sistema.rotinas.primefaces.dto.NivelPrecoProdutoDTO;
import sistema.rotinas.primefaces.repository.NivelPrecoProdutoRepository;
import sistema.rotinas.primefaces.service.interfaces.INivelPrecoProdutoService;

@Service
public class NivelPrecoProdutoService implements INivelPrecoProdutoService {

    @Autowired
    private NivelPrecoProdutoRepository repository;

    @Override
    @Transactional
    public List<NivelPrecoProdutoDTO> listarPorNivel(int codigoNivel) {
        return repository.listarPorNivel(codigoNivel);
    }

    @Override
    @Transactional
    public List<NivelPrecoProdutoDTO> listarPorNivel(
            int codigoNivel, int first, int pageSize, String sortField, boolean ascendente) {
        return repository.listarPorNivel(codigoNivel, first, pageSize, sortField, ascendente);
    }

    @Override
    @Transactional
    public int countPorNivel(int codigoNivel) {
        return repository.countPorNivel(codigoNivel);
    }

    @Override
    @Transactional
    public List<String> listarCodigosEan13PorNivel(int codigoNivel) {
        return repository.listarCodigosEan13PorNivel(codigoNivel);
    }

    /** NOVO: consulta por lista de e.codigo_produto com ORDER BY p.descricao. */
    @Override
    @Transactional
    public List<NivelPrecoProdutoDTO> listarPorNivelComProdutosOrdenadoPorDescricao(
            int codigoNivel, List<Long> codigosProduto) {
        return repository.listarPorNivelComProdutosOrdenadoPorDescricao(codigoNivel, codigosProduto);
    }
}
