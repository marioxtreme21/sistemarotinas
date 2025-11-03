package sistema.rotinas.primefaces.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import sistema.rotinas.primefaces.util.PastaUploadUtil;

@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + PastaUploadUtil.PASTA_BASE + "/")
                .setCachePeriod(3600);
   

        // 🔥 Mantém recursos estáticos (Primefaces, JS, CSS, etc)
        registry.addResourceHandler("/resources/**")
                .addResourceLocations("/resources/");
    }
}
