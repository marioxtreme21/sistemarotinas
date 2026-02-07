package sistema.rotinas.primefaces.scheduled;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import sistema.rotinas.primefaces.service.DivergenciaPrecoCrmService;

/**
 * ✅ Rotina agendada: Divergência Preço CRM x Preço Normal
 * Padrão do projeto: package ...scheduled e nome da classe iniciando com "Scheduled..."
 *
 * - Executa todos os dias às 06:00
 * - Sempre executa para TODAS as lojas (passa null)
 * - Se não houver divergências em nenhuma loja, o serviço não envia e-mail (regra já no service)
 */
@Component
public class ScheduledDivergenciaPrecoCrm {

    private static final Logger log = LoggerFactory.getLogger(ScheduledDivergenciaPrecoCrm.class);

    @Autowired
    private DivergenciaPrecoCrmService divergenciaPrecoCrmService;

    //@Scheduled(cron = "0 00 06 * * *")
    public void executar() {
        try {
            log.info("⏰ Iniciando rotina agendada: Divergência Preço CRM x Preço Normal (todas as lojas) - 06:00");
            divergenciaPrecoCrmService.executarManual(null); // null = todas as lojas
            log.info("✅ Rotina agendada finalizada: Divergência Preço CRM x Preço Normal.");
        } catch (Exception e) {
            log.error("❌ Erro ao executar rotina agendada Divergência Preço CRM x Preço Normal.", e);
        }
    }
}
