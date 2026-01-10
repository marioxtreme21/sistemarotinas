package sistema.rotinas.primefaces.config;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpConnectorConfig {

    @Value("${server.http.port:0}")
    private int httpPort;

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> httpConnectorCustomizer() {
        return factory -> {
            // se não configurou, não cria connector adicional
            if (httpPort <= 0) return;

            Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
            connector.setPort(httpPort);

            factory.addAdditionalTomcatConnectors(connector);
        };
    }
}