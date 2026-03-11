package sistema.rotinas.primefaces.service.porteira;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import sistema.rotinas.primefaces.model.porteira.PorteiraBackup;
import sistema.rotinas.primefaces.model.porteira.PorteiraBackupUsuario;
import sistema.rotinas.primefaces.model.porteira.PorteiraEletronica;
import sistema.rotinas.primefaces.repository.porteira.PorteiraBackupRepository;
import sistema.rotinas.primefaces.repository.porteira.PorteiraBackupUsuarioRepository;
import sistema.rotinas.primefaces.service.interfaces.porteira.IPorteiraBackupService;
import sistema.rotinas.primefaces.service.interfaces.porteira.IPorteiraEletronicaService;

@Service
public class PorteiraBackupService implements IPorteiraBackupService {

	private static final Logger LOG = LoggerFactory.getLogger("ROTINA_PORTEIRA");

	/**
	 * Retenção do histórico de backups.
	 * Mantém os backups por 30 dias.
	 */
	private static final int RETENCAO_DIAS = 30;

	private final PorteiraBackupRepository backupRepo;
	private final PorteiraBackupUsuarioRepository usuarioRepo;
	private final IPorteiraEletronicaService porteiraService;
	private final PorteiraBackupRuntimeClient runtimeClient;
	private final NotificacaoPorteiraService notificacaoService;

	private final ObjectMapper mapper = new ObjectMapper();

	public PorteiraBackupService(PorteiraBackupRepository backupRepo,
			PorteiraBackupUsuarioRepository usuarioRepo,
			IPorteiraEletronicaService porteiraService,
			PorteiraBackupRuntimeClient runtimeClient,
			NotificacaoPorteiraService notificacaoService) {
		this.backupRepo = backupRepo;
		this.usuarioRepo = usuarioRepo;
		this.porteiraService = porteiraService;
		this.runtimeClient = runtimeClient;
		this.notificacaoService = notificacaoService;
	}

	@Override
	public List<PorteiraBackup> listar() {
		List<PorteiraBackup> all = backupRepo.findAll();
		all.sort(Comparator.comparing(PorteiraBackup::getCriadoEm,
				Comparator.nullsLast(Comparator.naturalOrder())).reversed());
		return all;
	}

	@Override
	public PorteiraBackup obterPorPorteira(Long porteiraId) {
		if (porteiraId == null) {
			return null;
		}
		return backupRepo.findTopByPorteira_IdOrderByCriadoEmDesc(porteiraId).orElse(null);
	}

	@Override
	public List<PorteiraBackupUsuario> listarUsuariosDoBackup(Long backupId) {
		if (backupId == null)
			return Collections.emptyList();
		return usuarioRepo.findByBackup_IdOrderByUserAsc(backupId);
	}

	// =========================================================
	// EXCLUIR BACKUP (evita FK: apaga usuários antes)
	// =========================================================
	@Override
	@Transactional
	public void excluirBackup(Long backupId) {
		if (backupId == null)
			return;

		try {
			long qtd = usuarioRepo.deleteByBackup_Id(backupId);
			LOG.info("[MANUAL][BACKUP] Excluir backupId={} -> usuários removidos={}", backupId, qtd);
		} catch (Exception e) {
			LOG.warn("[MANUAL][BACKUP] Falha ao remover usuários do backupId={} msg={}", backupId, e.getMessage(), e);
			// mesmo com falha, tenta continuar (mas pode estourar FK ao deletar backup)
		}

		backupRepo.deleteById(backupId);
		LOG.info("[MANUAL][BACKUP] Backup removido backupId={}", backupId);
	}

	// =========================================================
	// MANUAL -> notifica individual
	// =========================================================
	@Override
	@Transactional
	public PorteiraBackup executarBackup(Long porteiraId) {
		return executarBackupInterno(porteiraId, "MANUAL", true);
	}

	@Override
	@Transactional
	public PorteiraBackup executarBackup(Long porteiraId, String origemExecucao) {
		String origem = normOrigem(origemExecucao);
		boolean notificarPorPorteira = !"AUTO".equals(origem);
		return executarBackupInterno(porteiraId, origem, notificarPorPorteira);
	}

	@Transactional
	public PorteiraBackup executarBackupAutoSemNotificar(Long porteiraId) {
		return executarBackupInterno(porteiraId, "AUTO", false);
	}

	// =========================================================
	// Núcleo do backup
	// =========================================================
	private PorteiraBackup executarBackupInterno(Long porteiraId, String origemExecucao, boolean notificarPorPorteira) {

		long iniMs = System.currentTimeMillis();
		String origem = normOrigem(origemExecucao);

		if (porteiraId == null)
			throw new IllegalArgumentException("porteiraId obrigatório.");

		PorteiraEletronica porteira = porteiraService.findById(porteiraId);
		if (porteira == null)
			throw new IllegalArgumentException("Porteira não encontrada: " + porteiraId);

		Long pid = porteira.getId();
		String desc = nz(porteira.getDescricao());
		String ip = nz(porteira.getIp());

		LOG.info("[{}][BACKUP] Start porteiraId={} desc={} ip={} notificarPorPorteira={}",
				origem, pid, desc, ip, notificarPorPorteira);

		// =====================================================
		// IMPORTANTE:
		// Cada execução cria um NOVO backup.
		// Nunca reutiliza o backup anterior e nunca apaga backups
		// antigos antes do sucesso do backup atual.
		// =====================================================
		PorteiraBackup backup = new PorteiraBackup();
		backup.setPorteira(porteira);
		backup.setCriadoEm(LocalDateTime.now());
		backup.setTotalUsuarios(0);
		backup.setStatus("INICIADO");
		backup.setLogExecucao("Iniciando backup...");

		backup = backupRepo.save(backup);

		LOG.info("[{}][BACKUP] Registro criado backupId={} porteiraId={}",
				origem, backup.getId(), pid);

		StringBuilder log = new StringBuilder();
		log.append("Backup Porteira: ").append(desc)
		   .append(" IP=").append(ip)
		   .append(" em ").append(LocalDateTime.now()).append("\n\n");

		try {
			PorteiraBackupRuntimeClient.RuntimeGetResult gr = runtimeClient.baixarUsuariosJson(porteira);

			if (gr == null) {
				backup.setStatus("FALHA");
				backup.setLogExecucao(log.append("Falha ao consultar users: retorno null do runtimeClient.\n").toString());
				backup = backupRepo.save(backup);

				LOG.error("[{}][BACKUP] FAIL GET users retorno null porteiraId={} backupId={}",
						origem, pid, backup.getId());

				if (notificarPorPorteira) {
					notificacaoService.notificarBackup(porteira, backup, origem, false,
							"Falha ao consultar users (retorno null).", backup.getLogExecucao());
				}
				return backup;
			}

			LOG.info("[{}][BACKUP] GET users httpCode={} ok={} porteiraId={} backupId={}",
					origem, gr.getHttpCode(), gr.isOk(), pid, backup.getId());

			String rawJson = gr.getBody();

			if (!gr.isOk()) {
				backup.setStatus("FALHA");
				log.append("Falha ao consultar users. http=").append(gr.getHttpCode()).append("\n");
				backup.setLogExecucao(log.toString());
				backup = backupRepo.save(backup);

				LOG.error("[{}][BACKUP] FAIL GET users httpCode={} porteiraId={} backupId={}",
						origem, gr.getHttpCode(), pid, backup.getId());

				if (notificarPorPorteira) {
					notificacaoService.notificarBackup(porteira, backup, origem, false,
							"Falha ao consultar users (http=" + gr.getHttpCode() + ")", backup.getLogExecucao());
				}
				return backup;
			}

			if (rawJson == null || rawJson.isBlank()) {
				backup.setStatus("FALHA");
				log.append("Retorno vazio ao consultar users.\n");
				backup.setLogExecucao(log.toString());
				backup = backupRepo.save(backup);

				LOG.error("[{}][BACKUP] FAIL retorno vazio GET users porteiraId={} backupId={}",
						origem, pid, backup.getId());

				if (notificarPorPorteira) {
					notificacaoService.notificarBackup(porteira, backup, origem, false,
							"Retorno vazio ao consultar users.", backup.getLogExecucao());
				}
				return backup;
			}

			int totalLido = 0;
			int salvos = 0;

			JsonNode root = mapper.readTree(rawJson);

			if (!root.isArray()) {
				backup.setStatus("FALHA");
				log.append("JSON não é array.\n");
				backup.setLogExecucao(log.toString());
				backup = backupRepo.save(backup);

				LOG.error("[{}][BACKUP] FAIL JSON não é array porteiraId={} backupId={}",
						origem, pid, backup.getId());

				if (notificarPorPorteira) {
					notificacaoService.notificarBackup(porteira, backup, origem, false,
							"JSON retornado não é array.", backup.getLogExecucao());
				}
				return backup;
			}

			for (JsonNode u : root) {
				totalLido++;

				String payload = mapper.writeValueAsString(u);

				PorteiraBackupUsuario bu = new PorteiraBackupUsuario();
				bu.setBackup(backup);

				bu.setApiId(txt(u, "id"));
				bu.setName(txt(u, "name"));
				bu.setUser(txt(u, "user"));
				bu.setPassword(txt(u, "password"));
				bu.setCard(txt(u, "card"));
				bu.setQrcode(txt(u, "qrcode"));
				bu.setRfcode(txt(u, "rfcode"));
				bu.setFingerprint(txt(u, "fingerprint"));
				bu.setValidity(txt(u, "validity"));
				bu.setLifecount(txt(u, "lifecount"));
				bu.setAccessibility(bool(u, "accessibility"));
				bu.setPanic(bool(u, "panic"));
				bu.setKeyUser(txt(u, "key"));
				bu.setUserInterface(txt(u, "interface"));
				bu.setAdministrator(bool(u, "administrator"));
				bu.setEmail(txt(u, "email"));
				bu.setApn(txt(u, "apn"));
				bu.setFcm(txt(u, "fcm"));
				bu.setVisitor(bool(u, "visitor"));
				bu.setRelay(txt(u, "relay"));
				bu.setFinger(txt(u, "finger"));
				bu.setFace(txt(u, "face"));

				bu.setPayloadJson(payload);

				usuarioRepo.save(bu);
				salvos++;
			}

			backup.setTotalUsuarios(salvos);
			backup.setStatus("OK");
			log.append("Total lido=").append(totalLido).append(" | Total salvo=").append(salvos).append("\n");

			// Limpeza de retenção: somente após backup bem-sucedido
			int removidosRetencao = limparBackupsAntigosDaPorteira(pid, origem, log);
			if (removidosRetencao > 0) {
				log.append("Retenção aplicada: backups antigos removidos=").append(removidosRetencao)
				   .append(" (maiores que ").append(RETENCAO_DIAS).append(" dias)\n");
			}

			backup.setLogExecucao(log.toString());
			backup = backupRepo.save(backup);

			long ms = System.currentTimeMillis() - iniMs;

			LOG.info("[{}][BACKUP] OK porteiraId={} backupId={} totalLido={} salvos={} ms={}",
					origem, pid, backup.getId(), totalLido, salvos, ms);

			if (notificarPorPorteira) {
				notificacaoService.notificarBackup(porteira, backup, origem, true,
						"Backup concluído. usuários=" + salvos, backup.getLogExecucao());
			}

			return backup;

		} catch (Exception e) {
			backup.setStatus("FALHA");
			log.append("ERRO: ").append(e.getMessage()).append("\n");
			backup.setLogExecucao(log.toString());
			backup = backupRepo.save(backup);

			long ms = System.currentTimeMillis() - iniMs;

			LOG.error("[{}][BACKUP] FAIL porteiraId={} backupId={} ms={} msg={}",
					origem, pid, backup.getId(), ms, e.getMessage(), e);

			if (notificarPorPorteira) {
				notificacaoService.notificarBackup(porteira, backup, origem, false,
						"Exceção no backup: " + e.getMessage(), backup.getLogExecucao());
			}

			return backup;
		}
	}

	/**
	 * Remove backups antigos da mesma porteira conforme a retenção configurada.
	 * A limpeza só é chamada após backup bem-sucedido.
	 */
	private int limparBackupsAntigosDaPorteira(Long porteiraId, String origem, StringBuilder log) {
		if (porteiraId == null) {
			return 0;
		}

		LocalDateTime limite = LocalDateTime.now().minusDays(RETENCAO_DIAS);
		List<PorteiraBackup> antigos = backupRepo.findByPorteira_IdAndCriadoEmBefore(porteiraId, limite);

		if (antigos == null || antigos.isEmpty()) {
			return 0;
		}

		int removidos = 0;

		for (PorteiraBackup antigo : antigos) {
			if (antigo == null || antigo.getId() == null) {
				continue;
			}

			try {
				long qtdUsuarios = usuarioRepo.deleteByBackup_Id(antigo.getId());
				backupRepo.deleteById(antigo.getId());
				removidos++;

				LOG.info("[{}][BACKUP][RETENCAO] backupAntigoId={} porteiraId={} usuariosRemovidos={} criadoEm={}",
						origem, antigo.getId(), porteiraId, qtdUsuarios, antigo.getCriadoEm());

			} catch (Exception e) {
				LOG.warn("[{}][BACKUP][RETENCAO] Falha ao remover backupAntigoId={} porteiraId={} msg={}",
						origem, antigo.getId(), porteiraId, e.getMessage(), e);

				if (log != null) {
					log.append("Falha ao remover backup antigo id=").append(antigo.getId())
					   .append(" msg=").append(e.getMessage()).append("\n");
				}
			}
		}

		return removidos;
	}

	// =========================================================
	// RESTORE
	// =========================================================
	@Override
	public RestoreResult restaurarBackupParaPorteira(Long backupId, Long porteiraDestinoId, boolean dryRun) {
		return restaurarBackupParaPorteira(backupId, porteiraDestinoId, dryRun, "MANUAL");
	}

	@Override
	public RestoreResult restaurarBackupParaPorteira(Long backupId, Long porteiraDestinoId, boolean dryRun,
			String origemExecucao) {

		long iniMs = System.currentTimeMillis();
		String origem = normOrigem(origemExecucao);

		if (backupId == null)
			throw new IllegalArgumentException("backupId obrigatório.");
		if (porteiraDestinoId == null)
			throw new IllegalArgumentException("porteiraDestinoId obrigatório.");

		PorteiraBackup backup = backupRepo.findById(backupId)
				.orElseThrow(() -> new IllegalArgumentException("Backup não encontrado: " + backupId));

		PorteiraEletronica destino = porteiraService.findById(porteiraDestinoId);
		if (destino == null)
			throw new IllegalArgumentException("Porteira destino não encontrada: " + porteiraDestinoId);

		PorteiraEletronica origemPorteira = backup.getPorteira();

		String origemDesc = nz(origemPorteira != null ? origemPorteira.getDescricao() : null);
		String origemIp = nz(origemPorteira != null ? origemPorteira.getIp() : null);

		String destDesc = nz(destino.getDescricao());
		String destIp = nz(destino.getIp());

		LOG.info("[{}][RESTORE] Start backupId={} ORIGEM={}({}) -> DESTINO={}({}) dryRun={}",
				origem, backupId, origemDesc, origemIp, destDesc, destIp, dryRun);

		List<PorteiraBackupUsuario> usuarios = usuarioRepo.findByBackup_IdOrderByUserAsc(backupId);

		RestoreResult rr = new RestoreResult();
		rr.total = (usuarios != null ? usuarios.size() : 0);
		rr.ok = 0;
		rr.falha = 0;

		StringBuilder log = new StringBuilder();
		log.append("Restore backupId=").append(backupId)
		   .append(" ORIGEM=").append(origemDesc).append(" (").append(origemIp).append(")")
		   .append(" -> DESTINO=").append(destDesc).append(" (").append(destIp).append(")")
		   .append(" dryRun=").append(dryRun)
		   .append(" em ").append(LocalDateTime.now()).append("\n\n");

		try {
			if (usuarios == null || usuarios.isEmpty()) {
				rr.log = log.append("Nenhum usuário no backup.\n").toString();

				LOG.warn("[{}][RESTORE] Sem usuários backupId={} destinoId={} dryRun={}",
						origem, backupId, porteiraDestinoId, dryRun);

				notificacaoService.notificarRestore(origemPorteira, destino, backupId,
						rr.total, rr.ok, rr.falha, dryRun, origem, rr.log);

				return rr;
			}

			for (PorteiraBackupUsuario u : usuarios) {
				try {
					String payload = u.getPayloadJson();
					String payloadNoId = removeId(payload);

					if (dryRun) {
						rr.ok++;
						log.append("[DRY] user=").append(nz(u.getUser()))
						   .append(" name=").append(nz(u.getName()))
						   .append(" payloadOk\n");
						continue;
					}

					PorteiraBackupRuntimeClient.RuntimePostResult r =
							runtimeClient.enviarUsuario(destino, payloadNoId);

					if (r != null && r.isOk()) {
						rr.ok++;
						log.append("[OK] user=").append(nz(u.getUser()))
						   .append(" http=").append(r.getHttpCode()).append("\n");
					} else {
						rr.falha++;
						log.append("[FALHA] user=").append(nz(u.getUser()))
						   .append(" http=").append(r != null ? r.getHttpCode() : 0)
						   .append(" resp=").append(r != null ? nz(r.getBody()) : "null").append("\n");
					}

				} catch (Exception e) {
					rr.falha++;
					log.append("[EXCEPTION] user=").append(nz(u.getUser()))
					   .append(" msg=").append(e.getMessage()).append("\n");
				}
			}

			rr.log = log.toString();

			long ms = System.currentTimeMillis() - iniMs;

			LOG.info("[{}][RESTORE] End backupId={} destinoId={} total={} ok={} falha={} dryRun={} ms={}",
					origem, backupId, porteiraDestinoId, rr.total, rr.ok, rr.falha, dryRun, ms);

			notificacaoService.notificarRestore(origemPorteira, destino, backupId,
					rr.total, rr.ok, rr.falha, dryRun, origem, rr.log);

			return rr;

		} catch (Exception e) {
			rr.falha = rr.falha + 1;
			rr.log = log.append("\nERRO GERAL: ").append(e.getMessage()).append("\n").toString();

			long ms = System.currentTimeMillis() - iniMs;

			LOG.error("[{}][RESTORE] FAIL backupId={} destinoId={} total={} ok={} falha={} dryRun={} ms={} msg={}",
					origem, backupId, porteiraDestinoId, rr.total, rr.ok, rr.falha, dryRun, ms, e.getMessage(), e);

			notificacaoService.notificarRestore(origemPorteira, destino, backupId,
					rr.total, rr.ok, rr.falha, dryRun, origem, rr.log);

			return rr;
		}
	}

	// =========================
	// Helpers JSON
	// =========================
	private String removeId(String json) {
		try {
			JsonNode node = mapper.readTree(json);
			if (node != null && node.isObject()) {
				ObjectNode obj = (ObjectNode) node;
				obj.remove("id");
				return mapper.writeValueAsString(obj);
			}
			return json;
		} catch (Exception e) {
			return json;
		}
	}

	private String txt(JsonNode n, String key) {
		if (n == null || key == null)
			return null;
		JsonNode v = n.get(key);
		if (v == null || v.isNull())
			return null;
		String s = v.asText();
		return (s != null ? s.trim() : null);
	}

	private Boolean bool(JsonNode n, String key) {
		if (n == null || key == null)
			return null;
		JsonNode v = n.get(key);
		if (v == null || v.isNull())
			return null;

		if (v.isBoolean())
			return v.asBoolean();

		String s = v.asText();
		if (s == null)
			return null;
		s = s.trim().toLowerCase();

		if ("true".equals(s))
			return true;
		if ("false".equals(s))
			return false;

		return null;
	}

	private static String nz(String s) {
		return (s == null || s.isBlank()) ? "-" : s.trim();
	}

	private static String normOrigem(String origemExecucao) {
		String o = (origemExecucao == null ? "" : origemExecucao.trim().toUpperCase());
		if (!"AUTO".equals(o) && !"MANUAL".equals(o))
			return "MANUAL";
		return o;
	}
}