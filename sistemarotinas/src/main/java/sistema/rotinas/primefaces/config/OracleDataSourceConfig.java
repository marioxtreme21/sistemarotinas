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
public class OracleDataSourceConfig {

    // Configurações do Oracle definidas diretamente na classe
    private String url = "jdbc:oracle:thin:@10.1.1.115:1521:ecome";
    private String username = "ecommerce";
    private String password = "ecommerce";
    private String driverClassName = "oracle.jdbc.OracleDriver";

    @Bean(name = "oracleDataSource")
    public DataSource oracleDataSource() {
        if (url.isEmpty() || username.isEmpty() || password.isEmpty() || driverClassName.isEmpty()) {
            throw new IllegalArgumentException("Database connection properties for Oracle are missing or not resolved.");
        }

        System.out.println("Oracle URL: " + url);
        System.out.println("Oracle Username: " + username);
        System.out.println("Oracle Driver: " + driverClassName);

        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .driverClassName(driverClassName)
                .build();
    }

    @Bean(name = "oracleEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean oracleEntityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(oracleDataSource());
        em.setPackagesToScan("sistema.rotinas.primefaces.model");
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.dialect", "org.hibernate.dialect.OracleDialect");
        properties.put("hibernate.hbm2ddl.auto", "none");

        em.setJpaPropertyMap(properties);
        return em;
    }

    @Bean(name = "oracleTransactionManager")
    public PlatformTransactionManager oracleTransactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(oracleEntityManagerFactory().getObject());
        return transactionManager;
    }

    // Configuração do JdbcTemplate para consultas no Oracle
    @Bean(name = "oracleJdbcTemplate")
    public JdbcTemplate oracleJdbcTemplate() {
        return new JdbcTemplate(oracleDataSource());
    }
}