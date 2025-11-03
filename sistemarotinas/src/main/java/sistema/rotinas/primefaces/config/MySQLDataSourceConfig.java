package sistema.rotinas.primefaces.config;

import java.util.HashMap;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.zaxxer.hikari.HikariDataSource;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackages = "sistema.rotinas.primefaces.repository",
    entityManagerFactoryRef = "mysqlEntityManagerFactory",
    transactionManagerRef = "mysqlTransactionManager"
)
public class MySQLDataSourceConfig {

    @Primary
    @Bean(name = "mySQLDataSource")
    public DataSource mySQLDataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:mysql://10.1.1.64:3306/sistemarotinas");
        ds.setUsername("root");
        ds.setPassword("Machi@21@");
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // Ajustes recomendados do pool (podem ser refinados conforme carga)
        ds.setMaximumPoolSize(15);
        ds.setMinimumIdle(3);
        ds.setIdleTimeout(300_000);       // 5 min
        ds.setConnectionTimeout(30_000);  // 30s
        ds.setMaxLifetime(1_800_000);     // 30 min
        ds.setPoolName("MySQLPool");

        return ds;
    }

    @Primary
    @Bean(name = "mysqlEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean mysqlEntityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(mySQLDataSource());
        em.setPackagesToScan("sistema.rotinas.primefaces.model");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);

        HashMap<String, Object> properties = new HashMap<>();
        // Em produção, prefira "none". Mantenho "update" para compatibilidade com seu ambiente atual.
        properties.put("hibernate.hbm2ddl.auto", "update");
        // Dialect moderno do Hibernate 6 para MySQL 8
        properties.put("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect");
        // SQL formatado (opcional)
        properties.put("hibernate.format_sql", "true");

        em.setJpaPropertyMap(properties);

        return em;
    }

    @Primary
    @Bean(name = "mysqlTransactionManager")
    public JpaTransactionManager mysqlTransactionManager() {
        JpaTransactionManager tx = new JpaTransactionManager();
        tx.setEntityManagerFactory(mysqlEntityManagerFactory().getObject());
        return tx;
    }

    /**
     * JdbcTemplate do MySQL marcado como @Primary.
     * Qualquer injeção de JdbcTemplate sem @Qualifier usará este bean.
     */
    @Primary
    @Bean(name = "mysqlJdbcTemplate")
    public JdbcTemplate mysqlJdbcTemplate(DataSource mySQLDataSource) {
        return new JdbcTemplate(mySQLDataSource);
    }
}
