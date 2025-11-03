package sistema.rotinas.primefaces.converter;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@FacesConverter(value = "localDateTimeConverter")
public class LocalDateTimeConverter implements Converter<LocalDateTime> {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @Override
    public LocalDateTime getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(value, formatter);
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, LocalDateTime value) {
        if (value == null) {
            return "";
        }
        return value.format(formatter);
    }
}