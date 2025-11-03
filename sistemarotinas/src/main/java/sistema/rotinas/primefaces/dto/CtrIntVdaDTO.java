package sistema.rotinas.primefaces.dto;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Representa uma linha da tabela ctr_int_vda com todas as colunas originais (em 'row')
 * e mais a coluna derivada 'transactionId' extraída do JSON contido em jso_env.
 */
public class CtrIntVdaDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Colunas originais do SELECT, preservando a ordem. */
    private LinkedHashMap<String, Object> row = new LinkedHashMap<>();

    /** Coluna derivada, extraída de 'jso_env' (JSON). */
    private String transactionId;

    public CtrIntVdaDTO() {}

    public CtrIntVdaDTO(LinkedHashMap<String, Object> row, String transactionId) {
        this.row = row;
        this.transactionId = transactionId;
    }

    public LinkedHashMap<String, Object> getRow() { return row; }
    public void setRow(LinkedHashMap<String, Object> row) { this.row = row; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    /** Acesso conveniente às colunas originais. */
    public Object get(String column) {
        if (row == null) return null;
        return row.get(column);
    }

    /** Adiciona/atualiza uma coluna. */
    public void put(String column, Object value) {
        if (row == null) row = new LinkedHashMap<>();
        row.put(column, value);
    }

    /** Retorna um mapa imutável para leitura em tabelas/export. */
    public Map<String, Object> asMapWithDerived() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(row);
        out.put("TRANSACTION_ID", transactionId);
        return out;
    }
}
