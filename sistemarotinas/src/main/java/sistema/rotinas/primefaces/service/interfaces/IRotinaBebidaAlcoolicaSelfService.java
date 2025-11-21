package sistema.rotinas.primefaces.service.interfaces;

import java.util.List;

import sistema.rotinas.primefaces.dto.ProdutoBebidaAlcoolicaDTO;

public interface IRotinaBebidaAlcoolicaSelfService {

    /**
     * Uso na TELA:
     *  1) Executa os dois UPDATEs de bebida_alcoolica.
     *  2) Faz SELECT dos produtos com bebida_alcoolica = 1.
     *  3) Retorna a lista para exibição na tela.
     */
    List<ProdutoBebidaAlcoolicaDTO> executarRotina();

    /**
     * Uso no SCHEDULER:
     *  Executa apenas os dois UPDATEs de bebida_alcoolica,
     *  sem SELECT e sem retorno.
     */
    void executarRotinaSemSelect();
}
