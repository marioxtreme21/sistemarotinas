package sistema.rotinas.primefaces.converter;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import sistema.rotinas.primefaces.model.ArquivosPrice;

@FacesConverter(value = "tipoDestinoPriceConverter")
public class TipoDestinoPriceConverter implements Converter {

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        // value vem como "FS", "SFTP", "SMB"
        return ArquivosPrice.TipoDestino.valueOf(value);
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        if (value == null) {
            return "";
        }

        // Se já for enum, ok
        if (value instanceof ArquivosPrice.TipoDestino td) {
            return td.name();
        }

        // Se vier como String (o que estava quebrando antes), devolve como está
        return value.toString();
    }
}
