package sistema.rotinas.primefaces.service.interfaces;

import java.time.LocalDate;
import java.util.List;

import sistema.rotinas.primefaces.dto.VendaLojaResumo;
import sistema.rotinas.primefaces.model.Loja;

public interface IRelatorioVendasLojasService {

    /**
     * Consulta bruta na base ECONECT, opcionalmente filtrando uma loja.
     * Se codLojaEconect for null ou vazio, busca todas as lojas.
     */
    List<VendaLojaResumo> gerarRelatorio(LocalDate dataInicial,
                                         LocalDate dataFinal,
                                         String codLojaEconect);

    /**
     * Usado pela tela.
     * Hoje o parâmetro {@code apenasLojasPermitidas} é ignorado
     * (relatório NÃO está mais limitado às lojas do usuário).
     * Mantido apenas para compatibilidade com o Bean.
     */
    List<VendaLojaResumo> buscarVendasPeriodo(LocalDate dataInicial,
                                              LocalDate dataFinal,
                                              boolean apenasLojasPermitidas);

    void enviarRelatorioPorEmail(LocalDate dataInicial,
                                 LocalDate dataFinal,
                                 String codLojaEconect);

    List<Loja> listarLojasParaFiltro();
}
