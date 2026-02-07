package sistema.rotinas.primefaces.converter;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;

@FacesConverter(value = "localTimeConverter")
public class LocalTimeConverter implements Converter<LocalTime> {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public LocalTime getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.isBlank()) return null;
        return LocalTime.parse(value.trim(), FMT);
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, LocalTime value) {
        return value == null ? "" : value.format(FMT);
    }
}