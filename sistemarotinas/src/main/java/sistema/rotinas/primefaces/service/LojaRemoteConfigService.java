package sistema.rotinas.primefaces.service;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import sistema.rotinas.primefaces.model.LojaRemoteConfig;
import sistema.rotinas.primefaces.repository.LojaRemoteConfigRepository;
import sistema.rotinas.primefaces.service.interfaces.ILojaRemoteConfigService;

@Service
public class LojaRemoteConfigService implements ILojaRemoteConfigService {

    private static final Logger log = LoggerFactory.getLogger(LojaRemoteConfigService.class);
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);

    @Autowired
    private LojaRemoteConfigRepository repo;

    /* ========================= CRUD ========================= */

    @Override
    @Transactional(readOnly = true)
    public List<LojaRemoteConfig> findAll() {
        return repo.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public LojaRemoteConfig findById(Long id) {
        return repo.findById(id).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public LojaRemoteConfig findByLojaId(Long lojaId) {
        if (lojaId == null) return null;
        return repo.findByLoja_LojaId(lojaId).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByLojaId(Long lojaId) {
        if (lojaId == null) return false;
        return repo.existsByLoja_LojaId(lojaId);
    }

    @Override
    @Transactional(readOnly = true)
    public LojaRemoteConfig findGlobal() {
        return repo.findByGlobalTrue().orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsGlobal() {
        return repo.existsByGlobalTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public LojaRemoteConfig resolveEffectiveForLoja(Long lojaId) {
        LojaRemoteConfig porLoja = findByLojaId(lojaId);
        if (porLoja != null) return porLoja;
        return findGlobal();
    }

    @Override
    @Transactional
    public LojaRemoteConfig save(LojaRemoteConfig cfg) {
        prepararEValidar(cfg, true);
        return repo.save(cfg);
    }

    @Override
    @Transactional
    public LojaRemoteConfig update(LojaRemoteConfig cfg) {
        prepararEValidar(cfg, false);
        return repo.save(cfg);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        repo.deleteById(id);
    }

    /* ==================== Regras/validações ==================== */

    private void prepararEValidar(LojaRemoteConfig cfg, boolean novo) {
        validarBasico(cfg);

        // Regra 1: global X loja (exclusivos)
        if (Boolean.TRUE.equals(cfg.getGlobal())) {
            // global => loja obrigatoriamente null
            cfg.setLoja(null);

            // permitir no máximo 1 global
            LojaRemoteConfig existenteGlobal = findGlobal();
            if (existenteGlobal != null) {
                // se estamos salvando um novo OU atualizando outro registro diferente do existente
                if (novo || !Objects.equals(existenteGlobal.getRemoteConfigId(), cfg.getRemoteConfigId())) {
                    throw new IllegalArgumentException("Já existe uma configuração global. Edite a existente ou desmarque 'global'.");
                }
            }
        } else {
            // não-global => loja obrigatória
            if (cfg.getLoja() == null || cfg.getLoja().getLojaId() == null) {
                throw new IllegalArgumentException("Selecione a Loja ou marque a configuração como Global.");
            }
            // Só para mensagem amigável: já existe config para essa loja?
            LojaRemoteConfig existenteLoja = findByLojaId(cfg.getLoja().getLojaId());
            if (existenteLoja != null) {
                if (novo || !Objects.equals(existenteLoja.getRemoteConfigId(), cfg.getRemoteConfigId())) {
                    throw new IllegalArgumentException("Já existe configuração para a loja selecionada.");
                }
            }
        }
    }

    private void validarBasico(LojaRemoteConfig cfg) {
        if (cfg == null) throw new IllegalArgumentException("Configuração não informada.");
        if (cfg.getHostRemoto() == null || cfg.getHostRemoto().isBlank())
            throw new IllegalArgumentException("Informe o Host remoto.");
        if (cfg.getUsuarioRemoto() == null || cfg.getUsuarioRemoto().isBlank())
            throw new IllegalArgumentException("Informe o Usuário remoto.");
        if (cfg.getPortaRemota() == null || cfg.getPortaRemota() <= 0)
            throw new IllegalArgumentException("Informe a Porta remota válida.");
        // Nada mais aqui para não interferir no que já estava funcional.
    }

    /* ==================== Teste de conexão ==================== */

    @Override
    public String testConnection(LojaRemoteConfig cfg, Duration timeout) {
        validarBasico(cfg);
        Duration to = (timeout == null ? DEFAULT_TIMEOUT : timeout);

        switch (cfg.getProtocolo()) {
            case SFTP:
                return testarSFTP(cfg, to);
            case FTP:
            case FTPS:
                // Mantido simples para não introduzir dependências agora.
                return "Protocolo " + cfg.getProtocolo() + " ainda não implementado no teste de conexão.";
            default:
                return "Protocolo desconhecido: " + cfg.getProtocolo();
        }
    }

    private String testarSFTP(LojaRemoteConfig cfg, Duration timeout) {
        Session session = null;
        ChannelSftp channel = null;

        try {
            JSch jsch = new JSch();

            // Suporte opcional a chave privada (se o campo estiver preenchido)
            if (cfg.getCaminhoChavePrivada() != null && !cfg.getCaminhoChavePrivada().isBlank()) {
                jsch.addIdentity(cfg.getCaminhoChavePrivada());
            }

            session = jsch.getSession(cfg.getUsuarioRemoto(), cfg.getHostRemoto(), cfg.getPortaRemota());

            if (cfg.getSenhaRemota() != null && !cfg.getSenhaRemota().isBlank()) {
                session.setPassword(cfg.getSenhaRemota());
            }

            // Evita prompt de host key em primeiro uso
            session.setConfig("StrictHostKeyChecking", "no");

            log.info("Conectando via SFTP para {}:{} (timeout={} ms)", cfg.getHostRemoto(), cfg.getPortaRemota(), timeout.toMillis());
            session.connect((int) timeout.toMillis());

            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect((int) timeout.toMillis());

            // Se tiver baseDirRemoto, tenta mudar de diretório (valida permissão)
            if (cfg.getBaseDirRemoto() != null && !cfg.getBaseDirRemoto().isBlank()) {
                try {
                    channel.cd(cfg.getBaseDirRemoto());
                } catch (Exception e) {
                    log.warn("Conexão SFTP ok, porém não foi possível acessar o diretório remoto '{}': {}",
                             cfg.getBaseDirRemoto(), e.getMessage());
                    return "OK parcial: conectou via SFTP, mas não acessou o diretório '" + cfg.getBaseDirRemoto() + "'. Motivo: " + e.getMessage();
                }
            }

            return "OK: Conexão SFTP estabelecida com " + cfg.getHostRemoto() + ":" + cfg.getPortaRemota();

        } catch (Exception e) {
            log.error("Falha ao testar SFTP para {}:{} - {}", cfg.getHostRemoto(), cfg.getPortaRemota(), e.getMessage(), e);
            return "Falha: " + e.getClass().getSimpleName() + " - " + e.getMessage();
        } finally {
            try {
                if (channel != null && channel.isConnected()) channel.disconnect();
            } catch (Exception ignore) {}
            try {
                if (session != null && session.isConnected()) session.disconnect();
            } catch (Exception ignore) {}
        }
    }
}
