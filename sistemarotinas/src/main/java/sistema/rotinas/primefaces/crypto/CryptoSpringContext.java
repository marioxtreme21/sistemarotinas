package sistema.rotinas.primefaces.crypto;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class CryptoSpringContext implements ApplicationContextAware {

    private static volatile ApplicationContext ctx;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        ctx = applicationContext;
    }

    public static Environment env() {
        return (ctx != null ? ctx.getEnvironment() : null);
    }
}
