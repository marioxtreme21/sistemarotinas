package sistema.rotinas.primefaces.service.interfaces;

import java.time.Duration;
import java.util.List;

import sistema.rotinas.primefaces.model.LojaRemoteConfig;

public interface ILojaRemoteConfigService {

    LojaRemoteConfig save(LojaRemoteConfig cfg);

    LojaRemoteConfig update(LojaRemoteConfig cfg);

    LojaRemoteConfig findById(Long id);

    void deleteById(Long id);

    List<LojaRemoteConfig> findAll();

    LojaRemoteConfig findByLojaId(Long lojaId);

    boolean existsByLojaId(Long lojaId);

    /**
     * Retorna a configuração marcada como global (padrão do sistema),
     * ou null se não existir.
     */
    LojaRemoteConfig findGlobal();

    /**
     * Existe configuração global?
     */
    boolean existsGlobal();

    /**
     * Resolve a configuração efetiva para a loja:
     * - Se existir específica da loja, retorna essa;
     * - Caso contrário, retorna a global (se existir);
     * - Senão, retorna null.
     */
    LojaRemoteConfig resolveEffectiveForLoja(Long lojaId);

    /**
     * Testa a conexão remota da configuração informada, com timeout.
     * @param cfg objeto LojaRemoteConfig contendo host, porta, usuário e senha
     * @param timeout duração máxima da tentativa
     * @return texto resumindo o resultado (OK / Falha / Detalhe)
     */
    String testConnection(LojaRemoteConfig cfg, Duration timeout);
}
