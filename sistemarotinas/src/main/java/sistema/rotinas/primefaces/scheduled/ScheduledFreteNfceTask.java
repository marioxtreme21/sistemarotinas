package sistema.rotinas.primefaces.scheduled;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import sistema.rotinas.primefaces.service.FreteNfceService;

@Component
public class ScheduledFreteNfceTask {

    private static final Logger log = LoggerFactory.getLogger(ScheduledFreteNfceTask.class);
    private static final ZoneId ZONE = ZoneId.of("America/Bahia");
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final FreteNfceService freteNfceService;

    /**
     * Valor mínimo do frete para entrar no filtro.
     * Deixe 0.00 para considerar qualquer frete > 0 (ou >= 0, conforme seu SQL).
     * Pode ser configurado em application.properties: frete.nfce.minimo=0.00
     */
    @Value("${frete.nfce.minimo:0.00}")
    private BigDecimal freteMinimo;

    public ScheduledFreteNfceTask(FreteNfceService freteNfceService) {
        this.freteNfceService = freteNfceService;
    }

    /**
     * Executa diariamente às 02:00 (horário de Bahia).
     * Cron: segundo minuto hora diaMes mes diaSemana
     */
    //@Scheduled(cron = "0 0 2 * * ?", zone = "America/Bahia")
    public void executar() {
        // data-base sempre o dia anterior (na zona de Bahia)
        LocalDate ontem = LocalDate.now(ZONE).minusDays(1);
        log.info("🚚 Iniciando rotina automática de ajuste de frete (NFCE). dataBase={}, freteMinimo={}",
                 DF.format(ontem), freteMinimo);

        try {
            freteNfceService.processar(ontem, freteMinimo);
            log.info("✅ Rotina de ajuste de frete concluída. dataBase={}", DF.format(ontem));
        } catch (Exception e) {
            log.error("❌ Erro ao executar rotina de ajuste de frete. dataBase=" + DF.format(ontem)
                    + " | msg=" + e.getMessage(), e);
        }
    }
}
