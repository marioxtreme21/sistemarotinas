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
public class MySQLEconectRmsDataSource144Config {

    // Configurações do banco de dados MySQL (somente leitura) no IP 10.1.1.144
    private String url = "jdbc:mysql://10.1.1.144:3306/concentrador";
    private String username = "root";
    private String password = "123456";
    private String driverClassName = "com.mysql.cj.jdbc.Driver";

    @Bean(name = "mysqlExternalDataSource144")
    public DataSource mysqlExternalDataSource144() {
        if (url.isEmpty() || username.isEmpty() || password.isEmpty() || driverClassName.isEmpty()) {
            throw new IllegalArgumentException("Database connection properties for MySQL (10.1.1.144) are missing or not resolved.");
        }

        System.out.println("MySQL(144) URL: " + url);
        System.out.println("MySQL(144) Username: " + username);
        System.out.println("MySQL(144) Driver: " + driverClassName);

        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .driverClassName(driverClassName)
                .build();
    }

    @Bean(name = "mysqlExternalEntityManagerFactory144")
    public LocalContainerEntityManagerFactoryBean mysqlExternalEntityManagerFactory144() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(mysqlExternalDataSource144());
        em.setPackagesToScan("sistema.rotinas.primefaces.model");
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect");
        properties.put("hibernate.hbm2ddl.auto", "none");

        em.setJpaPropertyMap(properties);
        return em;
    }

    @Bean(name = "mysqlExternalTransactionManager144")
    public PlatformTransactionManager mysqlExternalTransactionManager144() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(mysqlExternalEntityManagerFactory144().getObject());
        return transactionManager;
    }

    // JdbcTemplate para consultas no MySQL 10.1.1.144
    @Bean(name = "mysqlExternalJdbcTemplate144")
    public JdbcTemplate mysqlExternalJdbcTemplate144() {
        return new JdbcTemplate(mysqlExternalDataSource144());
    }
}
