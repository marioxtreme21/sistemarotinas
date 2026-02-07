// FILE: src/main/java/sistema/rotinas/primefaces/converter/PorteiraEletronicaConverter.java
package sistema.rotinas.primefaces.converter;

import java.util.Collection;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UISelectItem;
import jakarta.faces.component.UISelectItems;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import jakarta.faces.model.SelectItem;

import sistema.rotinas.primefaces.model.porteira.PorteiraEletronica;
import sistema.rotinas.primefaces.service.interfaces.porteira.IPorteiraEletronicaService;

@Component
@FacesConverter(value = "porteiraEletronicaConverter", managed = true)
public class PorteiraEletronicaConverter implements Converter<PorteiraEletronica> {

    @Autowired
    private IPorteiraEletronicaService service;

    @Override
    public PorteiraEletronica getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null) return null;

        String v = value.trim();
        if (v.isEmpty() || "null".equalsIgnoreCase(v)) return null;

        final Long id;
        try {
            id = Long.valueOf(v);
        } catch (NumberFormatException e) {
            return null;
        }

        // 1) tenta resolver pelo que já está no selectItems (melhor e evita DB)
        PorteiraEletronica fromComponent = findInSelectItems(component, id);
        if (fromComponent != null) return fromComponent;

        // 2) fallback: busca no service
        try {
            Object r = service.findById(id);

            // se o service retornar Optional
            if (r instanceof Optional) {
                @SuppressWarnings("unchecked")
                Optional<PorteiraEletronica> opt = (Optional<PorteiraEletronica>) r;
                return opt.orElse(null);
            }

            // se o service retornar direto a entidade
            return (PorteiraEletronica) r;

        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, PorteiraEletronica value) {
        if (value == null || value.getId() == null) return "";
        return String.valueOf(value.getId());
    }

    private PorteiraEletronica findInSelectItems(UIComponent component, Long id) {
        // percorre filhos: UISelectItems / UISelectItem
        for (UIComponent child : component.getChildren()) {

            if (child instanceof UISelectItems) {
                Object val = ((UISelectItems) child).getValue();

                if (val instanceof SelectItem) {
                    PorteiraEletronica p = fromSelectItem((SelectItem) val, id);
                    if (p != null) return p;
                }

                if (val instanceof SelectItem[]) {
                    for (SelectItem si : (SelectItem[]) val) {
                        PorteiraEletronica p = fromSelectItem(si, id);
                        if (p != null) return p;
                    }
                }

                if (val instanceof Iterable) {
                    for (Object o : (Iterable<?>) val) {
                        if (o instanceof SelectItem) {
                            PorteiraEletronica p = fromSelectItem((SelectItem) o, id);
                            if (p != null) return p;
                        } else if (o instanceof PorteiraEletronica) {
                            PorteiraEletronica p = (PorteiraEletronica) o;
                            if (p.getId() != null && p.getId().equals(id)) return p;
                        }
                    }
                }

                if (val instanceof Collection) {
                    for (Object o : (Collection<?>) val) {
                        if (o instanceof PorteiraEletronica) {
                            PorteiraEletronica p = (PorteiraEletronica) o;
                            if (p.getId() != null && p.getId().equals(id)) return p;
                        }
                    }
                }
            }

            if (child instanceof UISelectItem) {
                Object itemVal = ((UISelectItem) child).getItemValue();
                if (itemVal instanceof PorteiraEletronica) {
                    PorteiraEletronica p = (PorteiraEletronica) itemVal;
                    if (p.getId() != null && p.getId().equals(id)) return p;
                }
                if (itemVal instanceof SelectItem) {
                    PorteiraEletronica p = fromSelectItem((SelectItem) itemVal, id);
                    if (p != null) return p;
                }
            }
        }
        return null;
    }

    private PorteiraEletronica fromSelectItem(SelectItem si, Long id) {
        if (si == null) return null;
        Object iv = si.getValue();
        if (iv instanceof PorteiraEletronica) {
            PorteiraEletronica p = (PorteiraEletronica) iv;
            if (p.getId() != null && p.getId().equals(id)) return p;
        }
        return null;
    }
}