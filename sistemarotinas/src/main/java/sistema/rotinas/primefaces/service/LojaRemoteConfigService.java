package sistema.rotinas.primefaces.service;

import java.time.Duration;
import java.util.ArrayList;
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

    // ✅ logger dedicado (vai para remote-conn-test_ftp.log)
    private static final Logger logConn = LoggerFactory.getLogger("REMOTE_CONN_TEST_FTP");

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);

    // Limites para não explodir tela/log caso o diretório tenha MUITO arquivo
    private static final int LIST_MAX_ITENS = 80;
    private static final int RETURN_MAX_ITENS = 15;

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

        if (Boolean.TRUE.equals(cfg.getGlobal())) {
            cfg.setLoja(null);

            LojaRemoteConfig existenteGlobal = findGlobal();
            if (existenteGlobal != null) {
                if (novo || !Objects.equals(existenteGlobal.getRemoteConfigId(), cfg.getRemoteConfigId())) {
                    throw new IllegalArgumentException(
                        "Já existe uma configuração global. Edite a existente ou desmarque 'global'."
                    );
                }
            }
        } else {
            if (cfg.getLoja() == null || cfg.getLoja().getLojaId() == null) {
                throw new IllegalArgumentException("Selecione a Loja ou marque a configuração como Global.");
            }

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
                return "Protocolo " + cfg.getProtocolo() + " ainda não implementado no teste de conexão.";
            default:
                return "Protocolo desconhecido: " + cfg.getProtocolo();
        }
    }

    private String testarSFTP(LojaRemoteConfig cfg, Duration timeout) {
        Session session = null;
        ChannelSftp channel = null;

        String baseDirSolicitado = (cfg.getBaseDirRemoto() == null ? "" : cfg.getBaseDirRemoto().trim());

        try {
            JSch jsch = new JSch();

            if (cfg.getCaminhoChavePrivada() != null && !cfg.getCaminhoChavePrivada().isBlank()) {
                jsch.addIdentity(cfg.getCaminhoChavePrivada());
            }

            session = jsch.getSession(cfg.getUsuarioRemoto(), cfg.getHostRemoto(), cfg.getPortaRemota());

            if (cfg.getSenhaRemota() != null && !cfg.getSenhaRemota().isBlank()) {
                session.setPassword(cfg.getSenhaRemota());
            }

            session.setConfig("StrictHostKeyChecking", "no");

            log.info("Conectando via SFTP para {}:{} (timeout={} ms)",
                    cfg.getHostRemoto(), cfg.getPortaRemota(), timeout.toMillis());

            // ✅ log dedicado (sem senha)
            logConn.info("TESTE SFTP -> host={} porta={} usuario={} baseDir='{}' timeoutMs={}",
                    cfg.getHostRemoto(), cfg.getPortaRemota(), cfg.getUsuarioRemoto(),
                    (baseDirSolicitado.isBlank() ? "(vazio)" : baseDirSolicitado),
                    timeout.toMillis());

            session.connect((int) timeout.toMillis());

            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect((int) timeout.toMillis());

            // Diretório alvo: se vazio, tenta "/"
            String dirAlvo = baseDirSolicitado.isBlank() ? "/" : baseDirSolicitado;
            String dirEfetivo = null;

            // tenta cd, se falhar, mantém diretório atual
            try {
                channel.cd(dirAlvo);
                dirEfetivo = channel.pwd();
            } catch (Exception cdEx) {
                // fallback: diretório atual (home)
                try {
                    dirEfetivo = channel.pwd();
                } catch (Exception ignore) {
                    dirEfetivo = "(desconhecido)";
                }
                log.warn("Conexão SFTP ok, porém não foi possível acessar o diretório remoto '{}': {}",
                        dirAlvo, cdEx.getMessage());

                logConn.warn("SFTP OK, mas CD falhou. dirSolicitado='{}' fallbackDir='{}' motivo={}",
                        dirAlvo, dirEfetivo, cdEx.getMessage());
            }

            // ✅ Lista conteúdo do diretório efetivo (ou do atual)
            List<String> itens = listarSftpItens(channel, ".", LIST_MAX_ITENS);

            logConn.info("SFTP LIST -> dirEfetivo='{}' itensRetornados={} (max={})",
                    dirEfetivo, itens.size(), LIST_MAX_ITENS);

            // log detalhado (1 por linha) para facilitar leitura
            for (String it : itens) {
                logConn.debug("SFTP ITEM -> {}", it);
            }

            // Resumo para retorno (UI)
            String resumo = montarResumo(itens, RETURN_MAX_ITENS);

            return "OK: Conexão SFTP estabelecida com " + cfg.getHostRemoto() + ":" + cfg.getPortaRemota()
                    + " | Dir: " + dirEfetivo
                    + " | Itens: " + resumo;

        } catch (Exception e) {
            log.error("Falha ao testar SFTP para {}:{} - {}", cfg.getHostRemoto(), cfg.getPortaRemota(), e.getMessage(), e);
            logConn.error("FALHA TESTE SFTP -> host={} porta={} usuario={} erro={} msg={}",
                    cfg.getHostRemoto(), cfg.getPortaRemota(), cfg.getUsuarioRemoto(),
                    e.getClass().getSimpleName(), e.getMessage(), e);

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

    @SuppressWarnings("unchecked")
    private List<String> listarSftpItens(ChannelSftp channel, String dir, int maxItens) {
        List<String> out = new ArrayList<>();
        try {
            List<ChannelSftp.LsEntry> ls = channel.ls(dir);
            for (ChannelSftp.LsEntry e : ls) {
                String name = e.getFilename();
                if (".".equals(name) || "..".equals(name)) continue;

                boolean isDir = e.getAttrs() != null && e.getAttrs().isDir();
                String label = (isDir ? "[DIR] " : "[FILE] ") + name;

                out.add(label);
                if (out.size() >= maxItens) {
                    out.add("... (limitado a " + maxItens + " itens)");
                    break;
                }
            }
        } catch (Exception ex) {
            out.add("Falha ao listar: " + ex.getMessage());
            logConn.warn("Falha ao listar SFTP dir='{}' motivo={}", dir, ex.getMessage());
        }
        return out;
    }

    private String montarResumo(List<String> itens, int max) {
        if (itens == null || itens.isEmpty()) return "(vazio)";
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String s : itens) {
            if (s == null) continue;
            if (count >= max) break;
            if (sb.length() > 0) sb.append(", ");
            sb.append(s.replace("[DIR] ", "").replace("[FILE] ", ""));
            count++;
        }
        if (itens.size() > max) {
            sb.append(" ... (+").append(itens.size() - max).append(")");
        }
        return sb.toString();
    }
}
