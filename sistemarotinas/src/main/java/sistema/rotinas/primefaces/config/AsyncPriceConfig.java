package sistema.rotinas.primefaces.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Executor para paralelizar lojas (4 em paralelo) com fila e política de rejeição segura.
 */
@Configuration
public class AsyncPriceConfig {

    @Bean("lojasExecutor")
    public ThreadPoolTaskExecutor lojasExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(4);          // 4 lojas simultâneas
        ex.setMaxPoolSize(4);
        ex.setQueueCapacity(128);       // <<< fila para absorver picos de submissão
        ex.setThreadNamePrefix("loja-up-");

        // Em vez de lançar exceção quando cheio, executa no thread do chamador (backpressure)
        ex.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        ex.setWaitForTasksToCompleteOnShutdown(true);
        ex.initialize();
        return ex;
    }
}
