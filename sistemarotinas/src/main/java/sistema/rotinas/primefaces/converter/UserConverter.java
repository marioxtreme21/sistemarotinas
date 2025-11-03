package sistema.rotinas.primefaces.converter;

import java.util.List;
import org.springframework.web.jsf.FacesContextUtils;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import sistema.rotinas.primefaces.model.User;
import sistema.rotinas.primefaces.service.interfaces.IUserService;

@FacesConverter(value = "userConverter", managed = true)
public class UserConverter implements Converter<User> {

    private IUserService getService(FacesContext context) {
        return FacesContextUtils.getWebApplicationContext(context).getBean(IUserService.class);
    }

    @Override
    public User getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            Long id = Long.valueOf(value);
            IUserService service = getService(context);
            List<User> usuarios = service.getAllUsers();

            return usuarios.stream()
                    .filter(u -> u.getId().equals(id))
                    .findFirst()
                    .orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, User user) {
        if (user == null || user.getId() == null) {
            return "";
        }
        return user.getId().toString();
    }
}
