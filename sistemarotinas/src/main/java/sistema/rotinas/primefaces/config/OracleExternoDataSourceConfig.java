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
public class OracleExternoDataSourceConfig {

    private final String url = "jdbc:oracle:thin:@//201.157.211.129:1521/CNQ5X7_191256_C";
    private final String username = "CLT191256SOCIN";
    private final String password = "eyxsn54912EHUAM?!";
    private final String driverClassName = "oracle.jdbc.OracleDriver";

    @Bean(name = "oracleExternoDataSource")
    public DataSource oracleExternoDataSource() {
        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .driverClassName(driverClassName)
                .build();
    }

    @Bean(name = "oracleExternoEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean oracleExternoEntityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(oracleExternoDataSource());
        em.setPackagesToScan("sistema.rotinas.primefaces.model");
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.dialect", "org.hibernate.dialect.OracleDialect");
        properties.put("hibernate.hbm2ddl.auto", "none");

        em.setJpaPropertyMap(properties);
        return em;
    }

    @Bean(name = "oracleExternoTransactionManager")
    public PlatformTransactionManager oracleExternoTransactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(oracleExternoEntityManagerFactory().getObject());
        return transactionManager;
    }

    @Bean(name = "oracleExternoJdbcTemplate")
    public JdbcTemplate oracleExternoJdbcTemplate() {
        return new JdbcTemplate(oracleExternoDataSource());
    }
}
