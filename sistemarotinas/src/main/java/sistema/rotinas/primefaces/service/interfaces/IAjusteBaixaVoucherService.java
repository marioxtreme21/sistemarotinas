package sistema.rotinas.primefaces.service.interfaces;

import java.time.LocalDate;

import sistema.rotinas.primefaces.dto.ResultadoRotinaVoucherDTO;

public interface IAjusteBaixaVoucherService {

    ResultadoRotinaVoucherDTO executar(LocalDate dataInicial, LocalDate dataFinal);

}
