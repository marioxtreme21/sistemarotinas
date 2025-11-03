package sistema.rotinas.primefaces.service.interfaces;

import java.util.List;

import sistema.rotinas.primefaces.model.ConfiguracaoEmail;

public interface IConfiguracaoEmailService {

    List<ConfiguracaoEmail> getAll();

    ConfiguracaoEmail save(ConfiguracaoEmail config);

    ConfiguracaoEmail findById(Long id);

    void deleteById(Long id);

    ConfiguracaoEmail update(ConfiguracaoEmail config);

    ConfiguracaoEmail getConfiguracaoAtiva();

    /**
     * 🔸 Testa conexão de leitura (IMAP ou POP)
     */
    boolean testarConexaoLeitura(ConfiguracaoEmail configuracaoEmail);

    /**
     * 🔸 Testa envio de e-mail via SMTP
     */
    boolean testarEnvioEmail(ConfiguracaoEmail configuracaoEmail);
}
