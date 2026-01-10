package sistema.rotinas.primefaces.controller.tv;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sistema.rotinas.primefaces.dto.tv.RotinaSaudeDiaDto;
import sistema.rotinas.primefaces.dto.tv.RotinasHojeDto;
import sistema.rotinas.primefaces.service.tv.TvRotinasHojeService;
import sistema.rotinas.primefaces.service.tv.TvSaudeDiaService;

@RestController
@RequestMapping("/api/tv")
public class TvRotinasController {

    @Autowired
    private TvRotinasHojeService hojeService;

    @Autowired
    private TvSaudeDiaService saudeDiaService;

    /**
     * Força retorno em JSON, mesmo que exista converter XML no classpath.
     */
    @GetMapping(value = "/rotinas/hoje", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RotinasHojeDto> rotinasHoje() {
        return ResponseEntity.ok(hojeService.carregar());
    }

    /**
     * Card: Saúde do dia (pendências por loja)
     */
    @GetMapping(value = "/rotinas/saude-dia", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RotinaSaudeDiaDto> saudeDia() {
        return ResponseEntity.ok(saudeDiaService.carregar());
    }
}