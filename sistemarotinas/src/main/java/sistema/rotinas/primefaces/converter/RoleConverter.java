package sistema.rotinas.primefaces.converter;

import java.util.List;

import org.springframework.web.jsf.FacesContextUtils;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import sistema.rotinas.primefaces.model.Role;
import sistema.rotinas.primefaces.service.RoleService;

@FacesConverter(value = "roleConverter", managed = true)
public class RoleConverter implements Converter<Role> {

    private RoleService getRoleService(FacesContext context) {
        return FacesContextUtils.getWebApplicationContext(context).getBean(RoleService.class);
    }

    @Override
    public Role getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        RoleService roleService = getRoleService(context);
        List<Role> roles = roleService.getAllRoles();

        Role matchedRole = roles.stream()
                .filter(role -> role.getId().toString().equals(value))
                .findFirst()
                .orElse(null);

        // Log para depuração
        System.out.println("Converting role ID: " + value + " to role: " + (matchedRole != null ? matchedRole.getName() : "null"));
        return matchedRole;
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Role role) {
        if (role == null || role.getId() == null) {
            return "";
        }
        return role.getId().toString();
    }
}