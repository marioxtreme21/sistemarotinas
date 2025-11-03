package sistema.rotinas.primefaces.bean;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.model.SelectItem;
import jakarta.inject.Named;
import sistema.rotinas.primefaces.model.Loja;

@Component
@Named
@SessionScoped
public class PesquisaBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private String campoSelecionado;
    private String condicaoSelecionada;
    private String valorPesquisa;

    private List<SelectItem> camposDisponiveis;
    private List<Item> listaOriginal;
    private List<Item> listaFiltrada;

    @PostConstruct
    public void init() {
        carregarCamposDaEntidade(Loja.class); // Carrega os campos da classe Loja
        carregarDados();
    }

    private void carregarCamposDaEntidade(Class<?> entidadeClass) {
        camposDisponiveis = new ArrayList<>();
        for (Field field : entidadeClass.getDeclaredFields()) {
            camposDisponiveis.add(new SelectItem(field.getName(), field.getName()));
        }
    }

    private void carregarDados() {
        // Simulação de dados para a lista original
        this.listaOriginal = List.of(
            new Item("Item 1", "Descrição 1"),
            new Item("Item 2", "Descrição 2"),
            new Item("Item 3", "Descrição 3")
        );
        this.listaFiltrada = listaOriginal;
    }

    public void pesquisar() {
        if (campoSelecionado == null || condicaoSelecionada == null || valorPesquisa == null || valorPesquisa.isEmpty()) {
            listaFiltrada = listaOriginal;
            return;
        }

        listaFiltrada = listaOriginal.stream()
            .filter(item -> {
                String valorCampo;

                switch (campoSelecionado) {
                    case "nome":
                        valorCampo = item.getNome();
                        break;
                    case "descricao":
                        valorCampo = item.getDescricao();
                        break;
                    default:
                        return true;
                }

                if ("equal".equals(condicaoSelecionada)) {
                    return valorCampo.equalsIgnoreCase(valorPesquisa);
                } else if ("contains".equals(condicaoSelecionada)) {
                    return valorCampo.toLowerCase().contains(valorPesquisa.toLowerCase());
                }

                return false;
            })
            .collect(Collectors.toList());
    }

    // Getters e Setters

    public String getCampoSelecionado() {
        return campoSelecionado;
    }

    public void setCampoSelecionado(String campoSelecionado) {
        this.campoSelecionado = campoSelecionado;
    }

    public String getCondicaoSelecionada() {
        return condicaoSelecionada;
    }

    public void setCondicaoSelecionada(String condicaoSelecionada) {
        this.condicaoSelecionada = condicaoSelecionada;
    }

    public String getValorPesquisa() {
        return valorPesquisa;
    }

    public void setValorPesquisa(String valorPesquisa) {
        this.valorPesquisa = valorPesquisa;
    }

    public List<SelectItem> getCamposDisponiveis() {
        return camposDisponiveis;
    }

    public List<Item> getListaFiltrada() {
        return listaFiltrada;
    }

    public void setListaFiltrada(List<Item> listaFiltrada) {
        this.listaFiltrada = listaFiltrada;
    }

    // Classe interna para simular dados (substituir por `Loja` no uso real)
    public static class Item {
        private String nome;
        private String descricao;

        public Item(String nome, String descricao) {
            this.nome = nome;
            this.descricao = descricao;
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }

        public String getDescricao() {
            return descricao;
        }

        public void setDescricao(String descricao) {
            this.descricao = descricao;
        }
    }
}