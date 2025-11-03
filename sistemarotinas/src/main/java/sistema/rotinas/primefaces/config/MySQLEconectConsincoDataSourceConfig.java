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
public class MySQLEconectConsincoDataSourceConfig {

    // Configurações do banco de dados MySQL externo (somente leitura)
    private String url = "jdbc:mysql://10.1.1.144:3306/concentrador";
    private String username = "root";
    private String password = "123456";
    private String driverClassName = "com.mysql.cj.jdbc.Driver";

    @Bean(name = "mysqlExternalDataSource")
    public DataSource mysqlExternalDataSource() {
        if (url.isEmpty() || username.isEmpty() || password.isEmpty() || driverClassName.isEmpty()) {
            throw new IllegalArgumentException("Database connection properties for MySQL are missing or not resolved.");
        }

        System.out.println("MySQL URL: " + url);
        System.out.println("MySQL Username: " + username);
        System.out.println("MySQL Driver: " + driverClassName);

        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .driverClassName(driverClassName)
                .build();
    }

    @Bean(name = "mysqlExternalEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean mysqlExternalEntityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(mysqlExternalDataSource());
        em.setPackagesToScan("sistema.rotinas.primefaces.model");
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect"); // MySQL 8
        properties.put("hibernate.hbm2ddl.auto", "none");

        em.setJpaPropertyMap(properties);
        return em;
    }

    @Bean(name = "mysqlExternalTransactionManager")
    public PlatformTransactionManager mysqlExternalTransactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(mysqlExternalEntityManagerFactory().getObject());
        return transactionManager;
    }

    // JdbcTemplate para consultas no MySQL externo
    @Bean(name = "mysqlExternalJdbcTemplate")
    public JdbcTemplate mysqlExternalJdbcTemplate() {
        return new JdbcTemplate(mysqlExternalDataSource());
    }
}
