package sistema.rotinas.primefaces.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sistema.rotinas.primefaces.model.Loja;
import sistema.rotinas.primefaces.service.interfaces.ILojaService;

@RestController
@RequestMapping("/api/lojas")
public class LojaController {

    @Autowired
    private ILojaService lojaService;

    // Endpoint para obter todas as lojas
    @GetMapping
    public ResponseEntity<List<Loja>> getAllLojas() {
        List<Loja> lojas = lojaService.getAllLojas();
        return ResponseEntity.ok(lojas);
    }

    // Endpoint para obter uma loja pelo ID
    @GetMapping("/{id}")
    public ResponseEntity<Loja> getLojaById(@PathVariable Long id) {
        Loja loja = lojaService.findById(id);
        if (loja != null) {
            return ResponseEntity.ok(loja);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    // Endpoint para criar uma nova loja
    @PostMapping
    public ResponseEntity<Loja> createLoja(@RequestBody Loja loja) {
        Loja createdLoja = lojaService.save(loja);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdLoja);
    }

    // Endpoint para atualizar uma loja existente
    @PutMapping("/{id}")
    public ResponseEntity<Loja> updateLoja(@PathVariable Long id, @RequestBody Loja lojaDetails) {
        Loja existingLoja = lojaService.findById(id);
        if (existingLoja != null) {
            lojaDetails.setLojaId(id);  // Garantir que o ID não seja modificado
            Loja updatedLoja = lojaService.update(lojaDetails);
            return ResponseEntity.ok(updatedLoja);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    // Endpoint para excluir uma loja pelo ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLoja(@PathVariable Long id) {
        Loja existingLoja = lojaService.findById(id);
        if (existingLoja != null) {
            lojaService.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // Endpoint para pesquisa com critérios, exemplo básico de uso
    @GetMapping("/pesquisa")
    public ResponseEntity<List<Loja>> findLojasByCriteria(
            @RequestParam String campo,
            @RequestParam String condicao,
            @RequestParam String valor,
            @RequestParam(defaultValue = "0") int first,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String sortField,
            @RequestParam(defaultValue = "true") boolean ascendente) {
        List<Loja> lojas = lojaService.findLojasByCriteria(campo, condicao, valor, first, pageSize, sortField, ascendente);
        return ResponseEntity.ok(lojas);
    }

    // Endpoint para contar lojas com critérios de pesquisa
    @GetMapping("/count")
    public ResponseEntity<Integer> countLojasByCriteria(
            @RequestParam String campo,
            @RequestParam String condicao,
            @RequestParam String valor) {
        int count = lojaService.countLojasByCriteria(campo, condicao, valor);
        return ResponseEntity.ok(count);
    }
}