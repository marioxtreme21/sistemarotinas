package sistema.rotinas.primefaces.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import sistema.rotinas.primefaces.model.Loja;
import sistema.rotinas.primefaces.model.User;
import sistema.rotinas.primefaces.repository.LojaRepository;
import sistema.rotinas.primefaces.repository.UserRepository;
import sistema.rotinas.primefaces.service.interfaces.ILojaService;

@Service
public class LojaService implements ILojaService {

	@Autowired
	private LojaRepository lojaRepository;

	@Autowired
	private UserRepository userRepository;

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	@Transactional
	public List<Loja> getAllLojas() {
		return lojaRepository.findAll();
	}

	@Override
	@Transactional
	public Loja save(Loja loja) {
		// 🔍 Validação por Nome
		var lojaPorNome = lojaRepository.findByNome(loja.getNome()).orElse(null);
		if (lojaPorNome != null) {
			boolean ehMesmoRegistro = loja.getLojaId() != null && loja.getLojaId().equals(lojaPorNome.getLojaId());
			if (!ehMesmoRegistro) {
				throw new IllegalArgumentException("Já existe uma loja com o nome: " + loja.getNome());
			}
		}

		// 🔍 Validação por CNPJ
		var lojaPorCnpj = lojaRepository.findByCnpj(loja.getCnpj()).orElse(null);
		if (lojaPorCnpj != null) {
			boolean ehMesmoRegistro = loja.getLojaId() != null && loja.getLojaId().equals(lojaPorCnpj.getLojaId());
			if (!ehMesmoRegistro) {
				throw new IllegalArgumentException("Já existe uma loja com o CNPJ: " + loja.getCnpj());
			}
		}

		return lojaRepository.save(loja);
	}

	@Override
	@Transactional
	public Loja findById(Long id) {
		return lojaRepository.findById(id).orElse(null);
	}

	@Override
	@Transactional
	public void deleteById(Long id) {
		lojaRepository.deleteById(id);
	}

	@Override
	@Transactional
	public Loja update(Loja loja) {
		if (lojaRepository.existsById(loja.getLojaId())) {
			return lojaRepository.save(loja);
		} else {
			throw new IllegalArgumentException("Loja com ID " + loja.getLojaId() + " não encontrada.");
		}
	}

	@Override
	@Transactional
	public List<Loja> findAllLojas(int first, int pageSize, String sortField, boolean ascendente) {
		String sql = "SELECT * FROM loja";
		if (sortField != null) {
			sql += " ORDER BY " + sortField + (ascendente ? " ASC" : " DESC");
		}
		Query query = entityManager.createNativeQuery(sql, Loja.class);
		query.setFirstResult(first);
		query.setMaxResults(pageSize);
		return query.getResultList();
	}

	@Override
	@Transactional
	public int countLojas() {
		Query query = entityManager.createNativeQuery("SELECT COUNT(*) FROM loja");
		return ((Number) query.getSingleResult()).intValue();
	}

	@Override
	@Transactional
	public List<Loja> findLojasByCriteria(String campo, String condicao, String valor, int first, int pageSize,
			String sortField, boolean ascendente) {
		if (campo == null || campo.isEmpty() || condicao == null || condicao.isEmpty()) {
			return findAllLojas(first, pageSize, sortField, ascendente);
		}

		String sql = "SELECT * FROM loja WHERE " + campo;
		sql += condicao.equals("equal") ? " = :valor" : " LIKE :valor";
		if (sortField != null) {
			sql += " ORDER BY " + sortField + (ascendente ? " ASC" : " DESC");
		}

		Query query = entityManager.createNativeQuery(sql, Loja.class);
		query.setParameter("valor", condicao.equals("equal") ? valor : "%" + valor + "%");
		query.setFirstResult(first);
		query.setMaxResults(pageSize);
		return query.getResultList();
	}

	@Override
	@Transactional
	public int countLojasByCriteria(String campo, String condicao, String valor) {
		if (campo == null || campo.isEmpty() || condicao == null || condicao.isEmpty()) {
			return countLojas();
		}

		String sql = "SELECT COUNT(*) FROM loja WHERE " + campo;
		sql += condicao.equals("equal") ? " = :valor" : " LIKE :valor";
		Query query = entityManager.createNativeQuery(sql);
		query.setParameter("valor", condicao.equals("equal") ? valor : "%" + valor + "%");
		return ((Number) query.getSingleResult()).intValue();
	}

	@Override
	@Transactional
	public List<Loja> getLojasPermitidasDoUsuarioLogado() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated()) {
			return List.of();
		}

		String username = auth.getName();
		User user = userRepository.findByUsername(username).orElse(null);

		if (user != null) {
			return new ArrayList<>(user.getLojas());
		}

		return List.of();
	}
}
