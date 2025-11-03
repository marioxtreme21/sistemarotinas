package sistema.rotinas.primefaces.service.interfaces;

import java.util.List;

import sistema.rotinas.primefaces.dto.RelatorioPrecosAlteradosDTO;

public interface IRelatorioPrecosAlteradosService {

    /**
     * Gera um relatório PDF de preços alterados para uma loja.
     * 
     * @param nroEmpresa número da loja (codLojaRms)
     * @param dtIni data inicial no formato "DD/MM/YYYY HH:mm:ss"
     * @param dtFim data final no formato "DD/MM/YYYY HH:mm:ss"
     * @return caminho absoluto do arquivo PDF gerado ou null se não houver dados
     */
    String gerarPdfLoja(Integer nroEmpresa, String dtIni, String dtFim);

    /**
     * Gera relatórios PDF para várias lojas.
     * 
     * @param lojasIds lista de IDs das lojas (tabela loja)
     * @param dtIni data inicial
     * @param dtFim data final
     * @return lista de caminhos absolutos dos arquivos PDF gerados
     */
    List<String> gerarPdfParaLojas(List<Long> lojasIds, String dtIni, String dtFim);

	List<RelatorioPrecosAlteradosDTO> consultar(Integer nroEmpresa, String dtIni, String dtFim);
}
