package sistema.rotinas.primefaces.service.loyalty;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LoyaltyHashService {

    @Value("${loyalty.hash.use-salt:false}")
    private boolean useSalt;

    @Value("${loyalty.hash.salt:}")
    private String salt;

    public String hashCpf(String cpf) {
        String cpfNormalizado = normalizarCpf(cpf);
        if (cpfNormalizado == null) {
            return null;
        }

        return useSalt ? md5ComSalt(cpfNormalizado) : md5SemSalt(cpfNormalizado);
    }

    private String normalizarCpf(String valor) {
        if (valor == null) return null;

        String digits = valor.replaceAll("\\D", "");
        if (digits.isBlank() || "0".equals(digits)) {
            return null;
        }

        return digits;
    }

    private String md5SemSalt(String valor) {
        return md5(valor);
    }

    private String md5ComSalt(String cpfNormalizado) {
        String saltNormalizado = (salt == null ? "" : salt.trim());

        if (saltNormalizado.isEmpty()) {
            throw new IllegalStateException(
                "loyalty.hash.use-salt=true, mas loyalty.hash.salt não foi configurado."
            );
        }

        String base = saltNormalizado + ":" + cpfNormalizado;
        return md5(base);
    }

    private String md5(String valor) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gerar MD5.", e);
        }
    }
}