package sistema.rotinas.primefaces.enums;

public enum EtapaArquivoEnum {

    // =========================
    // (LEGADO / PRICE) - manter para não quebrar histórico gravado
    // =========================

    // tentativa de conexão / leitura
    CONEXAO_REMOTA_SFTP_CONSINCO,

    // traz do remoto para local
    DOWNLOAD_REMOTO_SFTP_CONSINCO,

    // cópia local -> destino (FS/SMB/SFTP)
    COPIA_DESTINO_PRICE_LOJA,

    // cópias adicionais (ex.: arquivo .m1 para MessageFiles)
    COPIA_MESSAGEFILES_PRICE_LOJA,

    // pós-processamento (mover processed etc.)
    POS_PROCESSAMENTO,

    // validações (tamanho, hash, timestamp etc.)
    VALIDACAO_ARQUIVOS,

    // =========================
    // (NOVO / GENÉRICO) - use daqui pra frente (PRICE e MGV)
    // =========================

    // conexão ao remoto (FTP/FTPS/SFTP) - genérico
    CONEXAO_REMOTA,

    // download remoto -> local - genérico
    DOWNLOAD_REMOTO,

    // cópia local -> destino FS (path montado)
    COPIA_DESTINO_FS,

    // cópia local -> destino SMB (share)
    COPIA_DESTINO_MGV,

    // cópia local -> destino SFTP (quando existir)
    COPIA_DESTINO_SFTP,

    // mover/remover no remoto após cópia (processed)
    MOVER_REMOTO_PROCESSED,

    // =========================
    // (NOVO / MGV)
    // =========================

    // identifica arquivos/patterns MGV a copiar (ex.: 5 arquivos)
    SELECAO_ARQUIVOS_MGV,

    // cópia local -> destino MGV via FS montado
    COPIA_DESTINO_MGV_FS,

    // cópia local -> destino MGV via SMB direto
    COPIA_DESTINO_MGV_SMB,

    // validação específica MGV (ex.: arquivos esperados, quantidade, nomes)
    VALIDACAO_ARQUIVOS_MGV;
}