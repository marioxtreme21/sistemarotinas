package sistema.rotinas.primefaces.service;

import java.util.List;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import sistema.rotinas.primefaces.model.ConfiguracaoEmail;
import sistema.rotinas.primefaces.repository.ConfiguracaoEmailRepository;
import sistema.rotinas.primefaces.service.interfaces.IConfiguracaoEmailService;

@Service
public class ConfiguracaoEmailService implements IConfiguracaoEmailService {

    @Autowired
    private ConfiguracaoEmailRepository repository;

    @Override
    @Transactional
    public List<ConfiguracaoEmail> getAll() {
        return repository.findAll();
    }

    @Override
    @Transactional
    public ConfiguracaoEmail save(ConfiguracaoEmail config) {
        return repository.save(config);
    }

    @Override
    @Transactional
    public ConfiguracaoEmail findById(Long id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    @Transactional
    public ConfiguracaoEmail update(ConfiguracaoEmail config) {
        if (repository.existsById(config.getId())) {
            return repository.save(config);
        } else {
            throw new IllegalArgumentException("Configuração com ID " + config.getId() + " não encontrada.");
        }
    }

    @Override
    @Transactional
    public ConfiguracaoEmail getConfiguracaoAtiva() {
        return repository.findByAtivoTrue().orElse(null);
    }

    /**
     * 🔸 Teste de envio SMTP
     */
    public boolean testarEnvioEmail(ConfiguracaoEmail config) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", config.getUsarTlsSmtp().toString());
            props.put("mail.smtp.ssl.enable", config.getUsarSslSmtp().toString());
            props.put("mail.smtp.host", config.getServidorSmtp());
            props.put("mail.smtp.port", config.getPortaSmtp().toString());

            Session session = Session.getInstance(props, new jakarta.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(config.getUsuarioEmail(), config.getSenhaEmail());
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(config.getUsuarioEmail()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(config.getUsuarioEmail()));
            message.setSubject("Teste de E-mail - Sistema Helpdesk");
            message.setText("Este é um e-mail de teste enviado para validar as configurações SMTP.");

            Transport.send(message);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 🔸 Teste de conexão de leitura IMAP ou POP
     */
    public boolean testarConexaoLeitura(ConfiguracaoEmail config) {
        Properties props = new Properties();

        String protocolo = config.getProtocoloLeitura().toLowerCase();

        // 🚀 Se usar SSL, muda o protocolo para imaps ou pop3s diretamente
        boolean usarSsl = Boolean.TRUE.equals(config.getUsarSslLeitura());
        String protocoloFinal = protocolo;

        if (usarSsl) {
            if (protocolo.equals("imap")) {
                protocoloFinal = "imaps";
            } else if (protocolo.equals("pop3")) {
                protocoloFinal = "pop3s";
            }
        }

        props.put("mail.store.protocol", protocoloFinal);
        props.put("mail." + protocoloFinal + ".host", config.getServidorLeitura());
        props.put("mail." + protocoloFinal + ".port", String.valueOf(config.getPortaLeitura()));

        props.put("mail.debug", "true"); // 🔍 Debug log no console

        if (usarSsl) {
            props.put("mail." + protocoloFinal + ".ssl.enable", "true");
        } else if (Boolean.TRUE.equals(config.getUsarTlsLeitura())) {
            props.put("mail." + protocoloFinal + ".starttls.enable", "true");
        }

        try {
            Session session = Session.getInstance(props);
            Store store = session.getStore(protocoloFinal);

            store.connect(
                    config.getServidorLeitura(),
                    config.getUsuarioEmail(),
                    config.getSenhaEmail()
            );

            store.close();
            System.out.println("✅ Conexão de leitura bem-sucedida.");
            return true;

        } catch (Exception e) {
            System.err.println("❌ Falha na conexão de leitura.");
            e.printStackTrace();
            return false;
        }
    }
}
