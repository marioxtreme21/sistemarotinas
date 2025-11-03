package sistema.rotinas.primefaces.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private String remetenteEmail;

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
            helper.setText("<html><body>" + corpoHtml + "</body></html>", true);

            if (anexos != null) {
                for (File anexo : anexos) {
                    helper.addAttachment(anexo.getName(), anexo);
                }
            }

            mailSender.send(mensagem);
            System.out.println("📧 E-mail enviado com sucesso para: " + destinatarios);

        } catch (MessagingException e) {
            System.err.println("❌ Erro ao enviar e-mail: " + e.getMessage());
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
                + "<p>Atenciosamente,<br>Equipe de Suporte Hiperideal</p>";

        enviarEmailSimples(List.of(destinatario), assunto, corpo);
    }

    /**
     * ✅ Enviar e-mail com anexos - passando lista de caminhos (String)
     */
    public void enviarEmailComAnexosPaths(List<String> destinatarios, String assunto, String corpoHtml, List<String> caminhosArquivosAnexos) {
        List<File> anexos = new ArrayList<>();
        if (caminhosArquivosAnexos != null) {
            for (String caminho : caminhosArquivosAnexos) {
                anexos.add(new File(caminho));
            }
        }
        enviarEmail(destinatarios, assunto, corpoHtml, anexos);
    }
}
