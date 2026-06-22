package sistema.rotinas.primefaces.service.loyalty;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "loyalty.batch")
public class LoyaltyBatchProperties {

    private Integer maxCupons = 100;

    public int getMaxCupons() {
        if (maxCupons == null || maxCupons < 1) {
            return 100;
        }
        return maxCupons;
    }

    public void setMaxCupons(Integer maxCupons) {
        this.maxCupons = maxCupons;
    }
}