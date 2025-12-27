package sistema.rotinas.primefaces.crypto;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.core.env.Environment;

public final class CryptoAesGcm {

    private CryptoAesGcm() {}

    private static final String ENV_KEY = "SISTEMAROTINAS_CRYPTO_KEY_B64";
    private static final String PROP_KEY = "sistemarotinas.crypto.key-b64";

    private static final String PREFIX_V1 = "v1:";

    private static final int GCM_TAG_BITS = 128;
    private static final int IV_LEN_BYTES = 12; // recomendado p/ GCM
    private static final SecureRandom RNG = new SecureRandom();

    private static volatile byte[] CACHED_KEY; // cache do decode Base64

    private static byte[] key() {
        byte[] k = CACHED_KEY;
        if (k != null) return k;

        synchronized (CryptoAesGcm.class) {
            if (CACHED_KEY != null) return CACHED_KEY;

            String b64 = resolveKeyB64();
            if (isBlank(b64)) {
                throw new IllegalStateException(
                    "Chave de criptografia não definida. Configure uma das opções: " +
                    "1) Variável de ambiente " + ENV_KEY + "  " +
                    "2) JVM -D" + ENV_KEY + "=...  " +
                    "3) application.properties: " + PROP_KEY + "=..."
                );
            }

            byte[] decoded;
            try {
                decoded = Base64.getDecoder().decode(b64.trim());
            } catch (Exception e) {
                throw new IllegalStateException("Chave inválida: não é Base64 válido.", e);
            }

            // AES aceita 16/24/32 bytes (128/192/256)
            if (!(decoded.length == 16 || decoded.length == 24 || decoded.length == 32)) {
                throw new IllegalStateException(
                    "Chave inválida: tamanho " + decoded.length + " bytes. Use 16, 24 ou 32 bytes (Base64)."
                );
            }

            CACHED_KEY = decoded;
            return decoded;
        }
    }

    private static String resolveKeyB64() {
        // 1) env var
        String b64 = System.getenv(ENV_KEY);
        if (!isBlank(b64)) return b64;

        // 2) system property -D
        b64 = System.getProperty(ENV_KEY);
        if (!isBlank(b64)) return b64;

        // 3) spring Environment (application.properties / yml)
        Environment env = CryptoSpringContext.env();
        if (env != null) {
            b64 = env.getProperty(ENV_KEY);
            if (!isBlank(b64)) return b64;

            b64 = env.getProperty(PROP_KEY);
            if (!isBlank(b64)) return b64;
        }

        // 4) fallback: system property com a chave de property
        b64 = System.getProperty(PROP_KEY);
        return b64;
    }

    public static String encryptToBase64(String plaintext) {
        if (plaintext == null) return null;

        try {
            byte[] iv = new byte[IV_LEN_BYTES];
            RNG.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_BITS, iv);
            SecretKeySpec keySpec = new SecretKeySpec(key(), "AES");

            cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec);

            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] out = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(cipherText, 0, out, iv.length, cipherText.length);

            return PREFIX_V1 + Base64.getEncoder().encodeToString(out);

        } catch (Exception e) {
            throw new RuntimeException("Falha ao criptografar", e);
        }
    }

    public static String decryptFromBase64(String stored) {
        if (stored == null) return null;

        String s = stored.trim();
        if (s.isEmpty()) return null;

        // ✅ Retrocompatível: se não tem prefixo, considera legado (texto puro)
        if (!s.startsWith(PREFIX_V1)) {
            return stored;
        }

        String b64 = s.substring(PREFIX_V1.length());

        try {
            byte[] all = Base64.getDecoder().decode(b64);

            if (all.length <= IV_LEN_BYTES) {
                // dado inválido, mas não derruba a aplicação
                return stored;
            }

            byte[] iv = new byte[IV_LEN_BYTES];
            byte[] cipherText = new byte[all.length - IV_LEN_BYTES];

            System.arraycopy(all, 0, iv, 0, IV_LEN_BYTES);
            System.arraycopy(all, IV_LEN_BYTES, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_BITS, iv);
            SecretKeySpec keySpec = new SecretKeySpec(key(), "AES");

            cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);

            byte[] plain = cipher.doFinal(cipherText);
            return new String(plain, StandardCharsets.UTF_8);

        } catch (Exception e) {
            // ✅ não derruba listagem/edição se tiver dado antigo/inválido
            return stored;
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
