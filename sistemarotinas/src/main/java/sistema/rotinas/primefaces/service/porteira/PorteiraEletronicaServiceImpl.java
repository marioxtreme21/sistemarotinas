// FILE: src/main/java/sistema/rotinas/primefaces/service/porteira/PorteiraEletronicaServiceImpl.java
package sistema.rotinas.primefaces.service.porteira;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sistema.rotinas.primefaces.model.porteira.PorteiraEletronica;
import sistema.rotinas.primefaces.repository.porteira.PorteiraEletronicaRepository;
import sistema.rotinas.primefaces.service.interfaces.porteira.IPorteiraEletronicaService;

@Service
public class PorteiraEletronicaServiceImpl implements IPorteiraEletronicaService {

    private final PorteiraEletronicaRepository repo;

    @PersistenceContext
    private EntityManager em;

    public PorteiraEletronicaServiceImpl(PorteiraEletronicaRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PorteiraEletronica> getAllPorteiras() {
        return repo.findAll();
    }

    @Override
    @Transactional
    public PorteiraEletronica save(PorteiraEletronica p) {
        validar(p);
        return repo.save(p);
    }

    @Override
    @Transactional(readOnly = true)
    public PorteiraEletronica findById(Long id) {
        if (id == null) return null;
        return repo.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        if (id == null) return;
        repo.deleteById(id);
    }

    @Override
    @Transactional
    public PorteiraEletronica update(PorteiraEletronica p) {
        validar(p);
        return repo.save(p);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PorteiraEletronica> buscarPorteirasComRotinaAtiva() {
        return repo.buscarPorteirasComRotinaAtiva();
    }

    @Override
    @Transactional(readOnly = true)
    public String buscarSenhaPelaId(Long id) {
        if (id == null) return null;
        return repo.buscarSenhaPelaId(id);
    }

    // Lazy
    @Override
    @Transactional(readOnly = true)
    public List<PorteiraEletronica> findAllPorteiras(int first, int pageSize, String sortField, boolean ascendente) {
        return findPorteirasByCriteria(null, null, null, first, pageSize, sortField, ascendente);
    }

    @Override
    @Transactional(readOnly = true)
    public int countPorteiras() {
        return countPorteirasByCriteria(null, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PorteiraEletronica> findPorteirasByCriteria(String campo, String condicao, String valor,
                                                            int first, int pageSize, String sortField, boolean ascendente) {

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<PorteiraEletronica> cq = cb.createQuery(PorteiraEletronica.class);
        Root<PorteiraEletronica> root = cq.from(PorteiraEletronica.class);

        List<Predicate> preds = new ArrayList<>();
        Predicate filtro = buildPredicate(campo, condicao, valor, cb, root);
        if (filtro != null) preds.add(filtro);

        cq.where(preds.toArray(new Predicate[0]));

        if (sortField != null && !sortField.isBlank()) {
            Path<?> sortPath = resolveSortPath(sortField, root);
            cq.orderBy(ascendente ? cb.asc(sortPath) : cb.desc(sortPath));
        } else {
            cq.orderBy(cb.asc(root.get("descricao")));
        }

        TypedQuery<PorteiraEletronica> q = em.createQuery(cq);
        q.setFirstResult(Math.max(first, 0));
        q.setMaxResults(Math.max(pageSize, 10));

        return q.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public int countPorteirasByCriteria(String campo, String condicao, String valor) {

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<PorteiraEletronica> root = cq.from(PorteiraEletronica.class);

        List<Predicate> preds = new ArrayList<>();
        Predicate filtro = buildPredicate(campo, condicao, valor, cb, root);
        if (filtro != null) preds.add(filtro);

        cq.select(cb.count(root));
        cq.where(preds.toArray(new Predicate[0]));

        Long total = em.createQuery(cq).getSingleResult();
        return total == null ? 0 : total.intValue();
    }

    // Helpers

    private void validar(PorteiraEletronica p) {
        if (p == null) throw new IllegalArgumentException("Porteira não informada.");
        if (isBlank(p.getDescricao())) throw new IllegalArgumentException("Descrição é obrigatória.");
        if (isBlank(p.getIp())) throw new IllegalArgumentException("IP é obrigatório.");
        if (p.getLoja() == null || p.getLoja().getLojaId() == null) {
            throw new IllegalArgumentException("Loja é obrigatória.");
        }
        if (p.getExecutarRotinaDesativacaoAtiva() == null) {
            p.setExecutarRotinaDesativacaoAtiva(Boolean.FALSE);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private Predicate buildPredicate(String campo, String condicao, String valor,
                                     CriteriaBuilder cb, Root<PorteiraEletronica> root) {

        String c = norm(campo);
        String op = norm(condicao);
        String v = (valor == null ? "" : valor.trim());

        if (c.isBlank() || op.isBlank() || v.isBlank()) return null;

        Expression<String> exp;

        if ("loja.nome".equalsIgnoreCase(c)) {
            exp = cb.upper(root.join("loja", JoinType.LEFT).get("nome"));
        } else if ("ip".equalsIgnoreCase(c)) {
            exp = cb.upper(root.get("ip"));
        } else {
            exp = cb.upper(root.get("descricao"));
        }

        String vUpper = v.toUpperCase(Locale.ROOT);

        if ("equal".equalsIgnoreCase(op)) {
            return cb.equal(exp, vUpper);
        }
        if ("contains".equalsIgnoreCase(op)) {
            return cb.like(exp, "%" + escapeLike(vUpper) + "%", '\\');
        }
        return cb.like(exp, "%" + escapeLike(vUpper) + "%", '\\');
    }

    private Path<?> resolveSortPath(String sortField, Root<PorteiraEletronica> root) {
        String sf = sortField.trim();
        if ("loja.nome".equalsIgnoreCase(sf)) return root.join("loja", JoinType.LEFT).get("nome");
        if ("ip".equalsIgnoreCase(sf)) return root.get("ip");
        if ("descricao".equalsIgnoreCase(sf)) return root.get("descricao");
        return root.get("descricao");
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim();
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}