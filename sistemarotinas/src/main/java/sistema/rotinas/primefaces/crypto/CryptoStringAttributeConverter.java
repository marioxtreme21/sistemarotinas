package sistema.rotinas.primefaces.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class CryptoStringAttributeConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        // ✅ não criptografa vazio -> evita tentar criptografar "" e falhar à toa
        if (attribute == null) return null;
        if (attribute.trim().isEmpty()) return null;

        return CryptoAesGcm.encryptToBase64(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        if (dbData.trim().isEmpty()) return null;

        return CryptoAesGcm.decryptFromBase64(dbData);
    }
}
