package sistema.rotinas.primefaces.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import sistema.rotinas.primefaces.dto.CtrIntVdaDTO;
import sistema.rotinas.primefaces.repository.CtrIntVdaRepository;
import sistema.rotinas.primefaces.service.interfaces.ICtrIntVdaService;

@Service
public class CtrIntVdaService implements ICtrIntVdaService {

    private final CtrIntVdaRepository repository;
    private final ObjectMapper mapper = new ObjectMapper();

    public CtrIntVdaService(CtrIntVdaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<CtrIntVdaDTO> listarComTransactionId() {
        List<LinkedHashMap<String, Object>> base = repository.listarTipInt16();
        List<CtrIntVdaDTO> out = new ArrayList<>(base.size());

        for (LinkedHashMap<String, Object> row : base) {
            String txId = null;

            Object jsoEnvObj = row.get("JSO_ENV"); // coluna normalizada pra UPPER no repo
            if (jsoEnvObj != null) {
                try {
                    // Aceita String ou byte[]; tenta parsear
                    String json = (jsoEnvObj instanceof byte[])
                            ? new String((byte[]) jsoEnvObj)
                            : String.valueOf(jsoEnvObj);

                    JsonNode root = mapper.readTree(json);
                    // extrai "transactionId" do nível raiz; ajuste aqui se estiver aninhado
                    JsonNode txNode = root.path("transactionId");
                    if (!txNode.isMissingNode() && !txNode.isNull()) {
                        txId = txNode.asText();
                    }
                } catch (Exception ignore) {
                    // JSON inválido ou formato inesperado -> txId permanece null
                }
            }

            out.add(new CtrIntVdaDTO(row, txId));
        }

        return out;
    }
}
