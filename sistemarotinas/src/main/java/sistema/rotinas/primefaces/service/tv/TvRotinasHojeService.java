package sistema.rotinas.primefaces.service.tv;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sistema.rotinas.primefaces.dto.tv.RotinasHojeDto;
import sistema.rotinas.primefaces.dto.tv.RotinasHojeDto.RotinaHojeItem;
import sistema.rotinas.primefaces.enums.StatusExecucaoEnum;
import sistema.rotinas.primefaces.enums.TipoRotinaEnum;
import sistema.rotinas.primefaces.model.rotina.RotinaExecucao;
import sistema.rotinas.primefaces.model.rotina.RotinaExecucaoLoja;
import sistema.rotinas.primefaces.repository.RotinaExecucaoLojaRepository;
import sistema.rotinas.primefaces.repository.RotinaExecucaoRepository;

@Service
public class TvRotinasHojeService {

    private static final DateTimeFormatter FMT_DIA  = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_HHMM = DateTimeFormatter.ofPattern("HH:mm");

    // Você pode trocar para property depois, mas por enquanto fixo como você usa:
    private static final ZoneId TV_ZONE = ZoneId.of("America/Bahia");

    @Autowired
    private RotinaExecucaoRepository execRepo;

    @Autowired
    private RotinaExecucaoLojaRepository execLojaRepo;

    @Transactional(readOnly = true)
    public RotinasHojeDto carregar() {

        LocalDate hoje = LocalDate.now(TV_ZONE);
        LocalDateTime ini = hoje.atStartOfDay();
        LocalDateTime fim = hoje.atTime(LocalTime.MAX);

        RotinaHojeItem price = montarItem(TipoRotinaEnum.PRICE, ini, fim);
        RotinaHojeItem mgv   = montarItem(TipoRotinaEnum.MGV, ini, fim);

        return new RotinasHojeDto(hoje.format(FMT_DIA), TV_ZONE.getId(), price, mgv);
    }

    private RotinaHojeItem montarItem(TipoRotinaEnum tipo, LocalDateTime ini, LocalDateTime fim) {

        RotinaExecucao e = execRepo
                .findTopByTipoRotinaAndInicioEmBetweenOrderByInicioEmDesc(tipo, ini, fim)
                .orElse(null);

        if (e == null) {
            RotinaHojeItem sem = new RotinaHojeItem(tipo.name(), "SEM_EXECUCAO", "-", null);
            sem.setLojasTotal(0);
            sem.setLojasOk(0);
            sem.setLojasParcial(0);
            sem.setLojasFalha(0);
            return sem;
        }

        String status = (e.getStatus() != null ? e.getStatus().name() : "SEM_STATUS");

        // “última execução” do card: usar fimEm se tiver, senão inicioEm
        LocalDateTime ref = (e.getFimEm() != null ? e.getFimEm() : e.getInicioEm());
        String hhmm = (ref != null ? ref.format(FMT_HHMM) : "-");

        RotinaHojeItem item = new RotinaHojeItem(tipo.name(), status, hhmm, e.getExecucaoId());

        // ✅ NOVO: contar lojas dessa execução (escopo)
        preencherTotaisLojas(item, e.getExecucaoId());

        return item;
    }

    private void preencherTotaisLojas(RotinaHojeItem item, Long execucaoId) {
        if (item == null || execucaoId == null) return;

        List<RotinaExecucaoLoja> all = execLojaRepo.findAll();

        int total = 0, ok = 0, parcial = 0, falha = 0;

        for (RotinaExecucaoLoja l : all) {
            if (l == null) continue;

            Long id = safeExecucaoId(l);
            if (id == null || !id.equals(execucaoId)) continue;

            total++;

            StatusExecucaoEnum st = l.getStatus();
            if (st == StatusExecucaoEnum.SUCESSO) ok++;
            else if (st == StatusExecucaoEnum.FALHA_PARCIAL) parcial++;
            else if (st == StatusExecucaoEnum.FALHA) falha++;
        }

        item.setLojasTotal(total);
        item.setLojasOk(ok);
        item.setLojasParcial(parcial);
        item.setLojasFalha(falha);
    }

    private Long safeExecucaoId(RotinaExecucaoLoja l) {
        try {
            if (l == null || l.getExecucao() == null) return null;
            return l.getExecucao().getExecucaoId();
        } catch (Exception e) {
            return null;
        }
    }
}