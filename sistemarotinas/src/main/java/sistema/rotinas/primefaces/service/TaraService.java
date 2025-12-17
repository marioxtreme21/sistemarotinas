package sistema.rotinas.primefaces.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import sistema.rotinas.primefaces.model.Tara;
import sistema.rotinas.primefaces.repository.TaraRepository;
import sistema.rotinas.primefaces.service.interfaces.ITaraService;

@Service
public class TaraService implements ITaraService {

    @Autowired
    private TaraRepository taraRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    @Qualifier("mysqlExternalJdbcTemplate")
    private JdbcTemplate mysqlExternalJdbcTemplate; // servidor 144 (concentrador)

    @Override
    @Transactional
    public List<Tara> getAllTaras() {
        return taraRepository.findAll();
    }

    @Override
    @Transactional
    public Tara save(Tara tara) {
        if (tara.getPrd() == null) {
            throw new IllegalArgumentException("O código do produto (prd) é obrigatório.");
        }

        if (tara.getPsoEmb() == null) {
            throw new IllegalArgumentException("O peso de embalagem (pso_emb) é obrigatório.");
        }

        // Ajuste padrão: se algum campo for nulo, setar 0 para sec/grp/sgr
        if (tara.getSec() == null) {
            tara.setSec(0);
        }
        if (tara.getGrp() == null) {
            tara.setGrp(0);
        }
        if (tara.getSgr() == null) {
            tara.setSgr(0);
        }

        // dat_atz sempre atualizado na gravação
        tara.setDatAtz(LocalDateTime.now());

        return taraRepository.save(tara);
    }

    @Override
    @Transactional
    public Tara findById(Long prd) {
        return taraRepository.findById(prd).orElse(null);
    }

    @Override
    @Transactional
    public void deleteById(Long prd) {
        taraRepository.deleteById(prd);
    }

    @Override
    @Transactional
    public Tara update(Tara tara) {
        if (taraRepository.existsById(tara.getPrd())) {
            // atualiza data de alteração
            tara.setDatAtz(LocalDateTime.now());
            return taraRepository.save(tara);
        } else {
            throw new IllegalArgumentException("Registro de tara com PRD " + tara.getPrd() + " não encontrado.");
        }
    }

    @Override
    @Transactional
    public List<Tara> findAllTaras(int first, int pageSize, String sortField, boolean ascendente) {
        String sql = "SELECT * FROM cad_pso_emb";
        if (sortField != null) {
            sql += " ORDER BY " + sortField + (ascendente ? " ASC" : " DESC");
        }
        Query query = entityManager.createNativeQuery(sql, Tara.class);
        query.setFirstResult(first);
        query.setMaxResults(pageSize);
        return query.getResultList();
    }

    @Override
    @Transactional
    public int countTaras() {
        Query query = entityManager.createNativeQuery("SELECT COUNT(*) FROM cad_pso_emb");
        return ((Number) query.getSingleResult()).intValue();
    }

    @Override
    @Transactional
    public List<Tara> findTarasByCriteria(String campo, String condicao, String valor,
                                           int first, int pageSize, String sortField, boolean ascendente) {

        if (campo == null || campo.isEmpty() || condicao == null || condicao.isEmpty()) {
            return findAllTaras(first, pageSize, sortField, ascendente);
        }

        String sql = "SELECT * FROM cad_pso_emb WHERE " + campo;
        boolean isNumericField = "prd".equalsIgnoreCase(campo)
                || "sec".equalsIgnoreCase(campo)
                || "grp".equalsIgnoreCase(campo)
                || "sgr".equalsIgnoreCase(campo)
                || "pso_emb".equalsIgnoreCase(campo);

        if (isNumericField) {
            sql += " = :valor";
        } else {
            sql += condicao.equals("equal") ? " = :valor" : " LIKE :valor";
        }

        if (sortField != null) {
            sql += " ORDER BY " + sortField + (ascendente ? " ASC" : " DESC");
        }

        Query query = entityManager.createNativeQuery(sql, Tara.class);

        if (isNumericField) {
            query.setParameter("valor", Integer.valueOf(valor));
        } else {
            query.setParameter("valor", condicao.equals("equal") ? valor : "%" + valor + "%");
        }

        query.setFirstResult(first);
        query.setMaxResults(pageSize);
        return query.getResultList();
    }

    @Override
    @Transactional
    public int countTarasByCriteria(String campo, String condicao, String valor) {
        if (campo == null || campo.isEmpty() || condicao == null || condicao.isEmpty()) {
            return countTaras();
        }

        String sql = "SELECT COUNT(*) FROM cad_pso_emb WHERE " + campo;
        boolean isNumericField = "prd".equalsIgnoreCase(campo)
                || "sec".equalsIgnoreCase(campo)
                || "grp".equalsIgnoreCase(campo)
                || "sgr".equalsIgnoreCase(campo)
                || "pso_emb".equalsIgnoreCase(campo);

        if (isNumericField) {
            sql += " = :valor";
        } else {
            sql += condicao.equals("equal") ? " = :valor" : " LIKE :valor";
        }

        Query query = entityManager.createNativeQuery(sql);

        if (isNumericField) {
            query.setParameter("valor", Integer.valueOf(valor));
        } else {
            query.setParameter("valor", condicao.equals("equal") ? valor : "%" + valor + "%");
        }

        return ((Number) query.getSingleResult()).intValue();
    }

    @Override
    @Transactional
    public void sincronizarComServidor144() {
        // Lê todos os registros do banco local
        List<Tara> taras = taraRepository.findAll();

        // Limpa a tabela remota no 144
        String deleteSql = "DELETE FROM cad_pso_emb";
        int deletados = mysqlExternalJdbcTemplate.update(deleteSql);

        // Insere novamente tudo que está no banco local
        String insertSql = """
                INSERT INTO cad_pso_emb (dat_atz, sec, grp, sgr, prd, pso_emb)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        mysqlExternalJdbcTemplate.batchUpdate(insertSql, taras, taras.size(), (ps, tara) -> {
            LocalDateTime datAtz = tara.getDatAtz() != null ? tara.getDatAtz() : LocalDateTime.now();
            ps.setTimestamp(1, Timestamp.valueOf(datAtz));
            ps.setInt(2, tara.getSec() != null ? tara.getSec() : 0);
            ps.setInt(3, tara.getGrp() != null ? tara.getGrp() : 0);
            ps.setInt(4, tara.getSgr() != null ? tara.getSgr() : 0);
            ps.setLong(5, tara.getPrd());
            ps.setInt(6, tara.getPsoEmb() != null ? tara.getPsoEmb() : 0);
        });

        // Apenas log (se quiser algo mais detalhado, ajustamos)
        System.out.println("[TaraService] Sincronização com servidor 144 concluída. Registros locais: "
                + taras.size() + " | Registros deletados no 144: " + deletados);
    }
}
