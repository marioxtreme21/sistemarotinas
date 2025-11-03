package sistema.rotinas.primefaces.config;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import jakarta.annotation.PostConstruct;
import sistema.rotinas.primefaces.model.ConfiguracaoEmail;
import sistema.rotinas.primefaces.service.interfaces.IConfiguracaoEmailService;

@Configuration
public class MailConfig {

    @Autowired
    private IConfiguracaoEmailService configuracaoEmailService;

    private ConfiguracaoEmail configuracaoEmailAtiva;

    @PostConstruct
    public void init() {
        configuracaoEmailAtiva = configuracaoEmailService.getConfiguracaoAtiva();
        if (configuracaoEmailAtiva == null) {
            throw new RuntimeException("❌ Nenhuma configuração de e-mail ativa encontrada no banco de dados.");
        }
    }

    @Bean
    public JavaMailSender mailSender() {
        if (configuracaoEmailAtiva == null) {
            throw new RuntimeException("❌ Configuração de e-mail não inicializada.");
        }

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(configuracaoEmailAtiva.getServidorSmtp());
        mailSender.setPort(configuracaoEmailAtiva.getPortaSmtp());
        mailSender.setUsername(configuracaoEmailAtiva.getUsuarioEmail());
        mailSender.setPassword(configuracaoEmailAtiva.getSenhaEmail());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");

        if (Boolean.TRUE.equals(configuracaoEmailAtiva.getUsarTlsSmtp())) {
            props.put("mail.smtp.starttls.enable", "true");
        } else {
            props.put("mail.smtp.starttls.enable", "false");
        }

        if (Boolean.TRUE.equals(configuracaoEmailAtiva.getUsarSslSmtp())) {
            props.put("mail.smtp.ssl.enable", "true");
        }

        props.put("mail.debug", "true"); // Pode remover em produção

        return mailSender;
    }

    @Bean
    public String remetenteEmail() {
        if (configuracaoEmailAtiva == null) {
            throw new RuntimeException("❌ Configuração de e-mail não inicializada.");
        }
        return configuracaoEmailAtiva.getUsuarioEmail();
    }
}
