package sistema.rotinas.primefaces.util;

import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;

public class EmailUtils {

    public static class ConteudoEmail {
        public String texto;
        public String html;
    }

    public static ConteudoEmail extrairConteudoEmailCompleto(Part p) throws Exception {
        ConteudoEmail conteudo = new ConteudoEmail();

        if (p.isMimeType("text/plain")) {
            conteudo.texto = removerAssinatura(p.getContent().toString());
            conteudo.html = "<pre>" + p.getContent().toString() + "</pre>";
        } else if (p.isMimeType("text/html")) {
            conteudo.html = p.getContent().toString();
            conteudo.texto = removerAssinatura(limparHtmlPreservandoQuebras(conteudo.html));
        } else if (p.isMimeType("multipart/alternative")) {
            Multipart mp = (Multipart) p.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                Part bp = mp.getBodyPart(i);
                if (bp.isMimeType("text/plain") && conteudo.texto == null) {
                    conteudo.texto = removerAssinatura(bp.getContent().toString());
                } else if (bp.isMimeType("text/html") && conteudo.html == null) {
                    conteudo.html = bp.getContent().toString();
                }
            }
            if (conteudo.texto == null && conteudo.html != null) {
                conteudo.texto = removerAssinatura(limparHtmlPreservandoQuebras(conteudo.html));
            }
            if (conteudo.html == null && conteudo.texto != null) {
                conteudo.html = "<pre>" + conteudo.texto + "</pre>";
            }
        } else if (p.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) p.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                ConteudoEmail subConteudo = extrairConteudoEmailCompleto(mp.getBodyPart(i));
                if (conteudo.texto == null) conteudo.texto = subConteudo.texto;
                if (conteudo.html == null) conteudo.html = subConteudo.html;
            }
        }

        return conteudo;
    }

    // 🔥 Atualizado para receber LocalDate
    public static String processarImagensInline(Part p, Long idChamado, String html, LocalDate data) throws Exception {
        if (html == null) return null;

        String pastaImagens = PastaUploadUtil.gerarCaminhoImagemEmail(data, idChamado);
        File pasta = new File(pastaImagens);
        if (!pasta.exists()) pasta.mkdirs();

        Map<String, String> cidToFileMap = new HashMap<>();
        html = processarImagensInlineRecursivo(p, data, idChamado, html, cidToFileMap);

        for (Map.Entry<String, String> entry : cidToFileMap.entrySet()) {
            html = html.replace("cid:" + entry.getKey(), entry.getValue());
        }

        return html;
    }

    private static String processarImagensInlineRecursivo(Part p, LocalDate data, Long idChamado, String html, Map<String, String> cidToFileMap) throws Exception {
        if (p.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) p.getContent();

            for (int i = 0; i < mp.getCount(); i++) {
                BodyPart bp = mp.getBodyPart(i);
                String[] headers = bp.getHeader("Content-ID");
                String contentId = (headers != null && headers.length > 0)
                        ? headers[0].replaceAll("[<>]", "").trim()
                        : null;

                if (contentId != null && bp.getContentType().toLowerCase().startsWith("image/")) {
                    String extensao = bp.getContentType().split("/")[1].split(";")[0].trim();
                    String nomeArquivo = contentId + "." + extensao;
                    File arquivo = new File(PastaUploadUtil.gerarCaminhoImagemEmail(data, idChamado), nomeArquivo);

                    try (InputStream is = bp.getInputStream(); FileOutputStream fos = new FileOutputStream(arquivo)) {
                        byte[] buf = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buf)) != -1) {
                            fos.write(buf, 0, bytesRead);
                        }
                    }

                    String urlPublica = PastaUploadUtil.gerarUrlPublicaImagemEmail(data, idChamado, nomeArquivo);
                    cidToFileMap.put(contentId, urlPublica);

                    System.out.println("🖼️ Imagem inline salva: " + nomeArquivo);
                }

                if (bp.isMimeType("multipart/*")) {
                    html = processarImagensInlineRecursivo(bp, data, idChamado, html, cidToFileMap);
                }
            }
        }
        return html;
    }

    public static String limparHtml(String html) {
        if (html == null) return null;

        Document doc = Jsoup.parse(html);

        doc.select("head, style, script, meta, link, xml, o\\:shapedefaults, o\\:shapelayout").remove();

        Elements allElements = doc.getAllElements();
        for (Element el : allElements) {
            el.removeAttr("style");
            el.removeAttr("class");
            el.removeAttr("id");
            el.removeAttr("lang");
            el.removeAttr("width");
            el.removeAttr("height");
            el.removeAttr("border");
            el.removeAttr("cellspacing");
            el.removeAttr("cellpadding");
        }

        doc.select("o\\:p").remove();

        doc.select("*").forEach(element -> {
            List<Node> children = new ArrayList<>(element.childNodes());
            for (Node node : children) {
                if (node.nodeName().equals("#comment") &&
                        node.toString().matches("(?i).*\\[if.*mso.*\\].*")) {
                    node.remove();
                }
            }
        });

        return doc.body().html().trim();
    }

    public static String limparHtmlPreservandoQuebras(String html) {
        if (html == null) return null;

        Document doc = Jsoup.parse(html);
        doc.select("br").append("\\n");
        doc.select("p").append("\\n");
        doc.select("div").append("\\n");
        doc.select("td").append("\\n");
        doc.select("li").append("\\n");

        String texto = doc.text();
        return texto.replace("\\n", "\n").trim();
    }

    public static String removerAssinatura(String texto) {
        if (texto == null) return null;

        String[] linhas = texto.split("\\r?\\n");
        List<String> linhasLimpas = new ArrayList<>();

        String[] delimitadores = {
                "Atenciosamente", "Cordialmente", "Obrigado", "Grato",
                "--", "---", "Telefone:", "Ramal:", "E-mail:",
                "Hiperideal", "Mario Emmanuel", "Coordenador", "T.I",
                "Enviado do meu", "Sent from"
        };

        boolean encontrouAssinatura = false;

        for (String linha : linhas) {
            String linhaTrimada = linha.trim();

            if (!encontrouAssinatura) {
                boolean linhaEhAssinatura = Arrays.stream(delimitadores)
                        .anyMatch(delimitador -> linhaTrimada.toLowerCase().contains(delimitador.toLowerCase()));

                if (linhaEhAssinatura) {
                    encontrouAssinatura = true;
                } else {
                    linhasLimpas.add(linhaTrimada);
                }
            }
        }

        return String.join("\n", linhasLimpas).trim();
    }
}
