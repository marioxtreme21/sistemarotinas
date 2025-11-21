package sistema.rotinas.primefaces.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.ServletContext;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    /**
     * ⚠️ Voltando ao comportamento anterior:
     * remetenteEmail é injetado via @Autowired (provavelmente por algum @Bean na sua MailConfig).
     */
    @Autowired
    private String remetenteEmail;

    /**
     * Caminho configurável (opcional).
     * Se estiver vazio, vamos tentar usar o realPath do servidor.
     */
    @Value("${sistema.rotinas.email.logo-path:}")
    private String logoPathConfig;

    /**
     * ServletContext para resolver o realPath (WAR em Tomcat/Payara/etc).
     * Em alguns cenários (teste unitário, app standalone) pode ser null.
     */
    @Autowired(required = false)
    private ServletContext servletContext;

    /**
     * Caminho “web” padrão da logo dentro do projeto JSF.
     * Corresponde a: src/main/webapp/resources/images/logo.png
     */
    private static final String DEFAULT_LOGO_WEB_PATH = "/resources/images/logo.png";

    /**
     * Resolve o arquivo físico da logo:
     * 1) Tenta a property sistema.rotinas.email.logo-path
     * 2) Se vazio ou não encontrado, tenta servletContext.getRealPath(...)
     */
    private File resolverArquivoLogo() {
        // 1) Property explícita (opcional, mas prioritária se existir)
        if (logoPathConfig != null && !logoPathConfig.isBlank()) {
            File logo = new File(logoPathConfig);
            if (logo.exists()) {
                System.out.println("✅ Logo encontrada pela property: " + logo.getAbsolutePath());
                return logo;
            } else {
                System.err.println("⚠️ Logo NÃO encontrada no caminho configurado: " + logo.getAbsolutePath());
            }
        }

        // 2) Tentar via realPath do ServletContext
        if (servletContext != null) {
            try {
                String realPath = servletContext.getRealPath(DEFAULT_LOGO_WEB_PATH);
                if (realPath != null) {
                    File logo = new File(realPath);
                    if (logo.exists()) {
                        System.out.println("✅ Logo encontrada via ServletContext.getRealPath: " + logo.getAbsolutePath());
                        return logo;
                    } else {
                        System.err.println("⚠️ Logo NÃO encontrada no realPath: " + logo.getAbsolutePath());
                    }
                } else {
                    System.err.println("⚠️ servletContext.getRealPath(" + DEFAULT_LOGO_WEB_PATH + ") retornou null (talvez rodando como JAR).");
                }
            } catch (Exception e) {
                System.err.println("❌ Erro ao resolver realPath da logo: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("⚠️ ServletContext é null (provavelmente rodando fora de um container web completo).");
        }

        // 3) Se nada funcionar, devolve null (sem logo)
        System.err.println("⚠️ Nenhuma logo encontrada; e-mails serão enviados sem imagem inline.");
        return null;
    }

    /**
     * ✅ Enviar e-mail genérico (com ou sem anexos - List<File>)
     */
    public void enviarEmail(List<String> destinatarios, String assunto, String corpoHtml, List<File> anexos) {
        try {
            MimeMessage mensagem = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensagem, true, StandardCharsets.UTF_8.name());

            helper.setFrom(remetenteEmail);
            helper.setTo(destinatarios.toArray(new String[0]));
            helper.setSubject(assunto);

            // Envolve corpo HTML num <html><body>...</body></html>
            String htmlFinal = "<html><body>" + corpoHtml + "</body></html>";
            helper.setText(htmlFinal, true);

            // ✅ Embute a logo inline com dois CIDs:
            // - logoHiperideal        -> usado nos e-mails do Helpdesk
            // - logoRelatorioVendas   -> usado no Relatório de Vendas por Loja
            File logoFile = resolverArquivoLogo();
            if (logoFile != null) {
                FileSystemResource resource = new FileSystemResource(logoFile);
                helper.addInline("logoHiperideal", resource);
                helper.addInline("logoRelatorioVendas", resource);
            }

            // Anexos (se houver)
            if (anexos != null) {
                for (File anexo : anexos) {
                    if (anexo != null && anexo.exists()) {
                        helper.addAttachment(anexo.getName(), anexo);
                    }
                }
            }

            mailSender.send(mensagem);
            System.out.println("📧 E-mail enviado com sucesso para: " + destinatarios);

        } catch (MessagingException e) {
            System.err.println("❌ Erro ao enviar e-mail: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("❌ Erro inesperado ao enviar e-mail: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * ✅ Enviar e-mail simples (sem anexos)
     */
    public void enviarEmailSimples(List<String> destinatarios, String assunto, String corpoHtml) {
        enviarEmail(destinatarios, assunto, corpoHtml, null);
    }

    /**
     * ✅ Enviar confirmação de abertura de chamado
     */
    public void enviarConfirmacaoAberturaChamado(String destinatario, Long chamadoId, String titulo, String status) {
        String assunto = "[Helpdesk #" + chamadoId + "] Chamado aberto - " + titulo;

        String corpo = "<p>Olá,</p>"
                + "<p>Seu chamado foi aberto com sucesso no sistema de suporte.</p>"
                + "<p><b>Número do Chamado:</b> " + chamadoId + "<br>"
                + "<b>Assunto:</b> " + titulo + "<br>"
                + "<b>Status:</b> " + status + "</p>"
                + "<p>Para continuar esse atendimento, basta responder este e-mail mantendo no assunto "
                + "<b>[Helpdesk #" + chamadoId + "]</b>.</p>"
                + "<p>Atenciosamente,<br>Equipe de Suporte Hiperideal</p>"
                + "<img src='cid:logoHiperideal' alt='Hiperideal' height='40' style='margin-top:10px;'/>";

        enviarEmailSimples(List.of(destinatario), assunto, corpo);
    }

    /**
     * ✅ Enviar e-mail com anexos - passando lista de caminhos (String)
     */
    public void enviarEmailComAnexosPaths(List<String> destinatarios, String assunto, String corpoHtml, List<String> caminhosArquivosAnexos) {
        List<File> anexos = new ArrayList<>();
        if (caminhosArquivosAnexos != null) {
            for (String caminho : caminhosArquivosAnexos) {
                if (caminho != null && !caminho.isBlank()) {
                    anexos.add(new File(caminho));
                }
            }
        }
        enviarEmail(destinatarios, assunto, corpoHtml, anexos);
    }
}
