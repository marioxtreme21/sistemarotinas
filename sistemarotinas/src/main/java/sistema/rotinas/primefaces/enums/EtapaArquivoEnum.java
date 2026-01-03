package sistema.rotinas.primefaces.enums;

public enum EtapaArquivoEnum {
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
    VALIDACAO_ARQUIVOS
}
