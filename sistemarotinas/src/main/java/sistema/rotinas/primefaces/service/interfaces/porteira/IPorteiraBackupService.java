// FILE: src/main/java/sistema/rotinas/primefaces/service/interfaces/porteira/IPorteiraBackupService.java
package sistema.rotinas.primefaces.service.interfaces.porteira;

import java.util.List;

import sistema.rotinas.primefaces.model.porteira.PorteiraBackup;
import sistema.rotinas.primefaces.model.porteira.PorteiraBackupUsuario;

public interface IPorteiraBackupService {

    List<PorteiraBackup> listar();

    PorteiraBackup obterPorPorteira(Long porteiraId);

    List<PorteiraBackupUsuario> listarUsuariosDoBackup(Long backupId);

    // ✅ compatibilidade
    PorteiraBackup executarBackup(Long porteiraId);

    // ✅ novo: usado pela task automática
    PorteiraBackup executarBackup(Long porteiraId, String origemExecucao); // "MANUAL" | "AUTO"

    // ✅ compatibilidade
    RestoreResult restaurarBackupParaPorteira(Long backupId, Long porteiraDestinoId, boolean dryRun);

    // ✅ novo: usado pela task automática (se vocês quiserem restore automático um dia)
    RestoreResult restaurarBackupParaPorteira(Long backupId, Long porteiraDestinoId, boolean dryRun, String origemExecucao);

    // ✅ NOVO: Excluir backup (remove usuários do backup + backup)
    void excluirBackup(Long backupId);

    class RestoreResult {
        public int total;
        public int ok;
        public int falha;
        public String log;
    }
}