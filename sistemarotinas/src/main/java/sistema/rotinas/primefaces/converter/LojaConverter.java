package sistema.rotinas.primefaces.converter;

import java.util.List;

import org.springframework.web.jsf.FacesContextUtils;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import sistema.rotinas.primefaces.model.Loja;
import sistema.rotinas.primefaces.service.interfaces.ILojaService;

@FacesConverter(value = "lojaConverter", managed = true)
public class LojaConverter implements Converter<Loja> {

    private ILojaService getLojaService(FacesContext context) {
        return FacesContextUtils.getWebApplicationContext(context).getBean(ILojaService.class);
    }

    @Override
    public Loja getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        ILojaService lojaService = getLojaService(context);
        List<Loja> lojas = lojaService.getAllLojas();

        // Encontrar a loja pelo ID
        return lojas.stream()
                .filter(loja -> loja.getLojaId().toString().equals(value))
                .findFirst()
                .orElse(null);
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Loja loja) {
        if (loja == null || loja.getLojaId() == null) {
            return "";
        }
        return loja.getLojaId().toString();
    }
}