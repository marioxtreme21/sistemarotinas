package sistema.rotinas.primefaces.config;

import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class OracleDataSourceRmsConfig {

    // TODO: ajuste para o IP/porta/serviço/usuário/senha do RMS
    private String url = "jdbc:oracle:thin:@10.1.1.185:1521/ideal";
    private String username = "rms";
    private String password = "rms";
    private String driverClassName = "oracle.jdbc.OracleDriver";

    @Bean(name = "oracleRmsDataSource")
    public DataSource oracleRmsDataSource() {
        if (url.isEmpty() || username.isEmpty() || password.isEmpty() || driverClassName.isEmpty()) {
            throw new IllegalArgumentException("Oracle RMS connection properties are missing.");
        }
        System.out.println("Oracle(RMS) URL: " + url);
        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .driverClassName(driverClassName)
                .build();
    }

    @Bean(name = "oracleRmsEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean oracleRmsEntityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(oracleRmsDataSource());
        em.setPackagesToScan("sistema.rotinas.primefaces.model");
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.dialect", "org.hibernate.dialect.OracleDialect");
        properties.put("hibernate.hbm2ddl.auto", "none");

        em.setJpaPropertyMap(properties);
        return em;
    }

    @Bean(name = "oracleRmsTransactionManager")
    public PlatformTransactionManager oracleRmsTransactionManager() {
        JpaTransactionManager tx = new JpaTransactionManager();
        tx.setEntityManagerFactory(oracleRmsEntityManagerFactory().getObject());
        return tx;
    }

    @Bean(name = "oracleRmsJdbcTemplate")
    public JdbcTemplate oracleRmsJdbcTemplate() {
        return new JdbcTemplate(oracleRmsDataSource());
    }
}
