package sistema.rotinas.primefaces.bean;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import sistema.rotinas.primefaces.model.Loja;
import sistema.rotinas.primefaces.service.NotificacaoService;
import sistema.rotinas.primefaces.service.RelatorioPrecosAlteradosService;
import sistema.rotinas.primefaces.service.interfaces.ILojaService;

@Component
@Named
@SessionScoped
public class RelatorioPrecosAlteradosBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final SimpleDateFormat FMT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    @Autowired private ILojaService lojaService;
    @Autowired private RelatorioPrecosAlteradosService relatorioService;
    @Autowired private NotificacaoService notificacaoService;

    private List<Loja> lojas;
    private List<Long> lojasSelecionadas;

    private Date dataInicialDate;
    private Date dataFinalDate;

    private List<String> arquivosGerados;

    @PostConstruct
    public void init() {
        lojas = lojaService.getAllLojas();
        lojasSelecionadas = new ArrayList<>();
        arquivosGerados = new ArrayList<>();

        LocalDate hoje = LocalDate.now(ZoneId.systemDefault());
        this.dataInicialDate = Date.from(hoje.atTime(0, 0, 0).atZone(ZoneId.systemDefault()).toInstant());
        this.dataFinalDate   = Date.from(hoje.atTime(7, 0, 0).atZone(ZoneId.systemDefault()).toInstant());
    }

    private boolean validarParametros() {
        if (lojasSelecionadas == null || lojasSelecionadas.isEmpty()) {
            addMsg(FacesMessage.SEVERITY_WARN, "Atenção", "Selecione ao menos uma loja.");
            return false;
        }
        if (dataInicialDate == null || dataFinalDate == null) {
            addMsg(FacesMessage.SEVERITY_WARN, "Atenção", "Informe Data Inicial e Data Final.");
            return false;
        }
        if (dataInicialDate.after(dataFinalDate)) {
            addMsg(FacesMessage.SEVERITY_WARN, "Atenção", "Data Inicial não pode ser maior que a Final.");
            return false;
        }
        return true;
    }

    public void gerarSomente() {
        if (!validarParametros()) return;

        String dtIni = FMT.format(dataInicialDate);
        String dtFim = FMT.format(dataFinalDate);

        try {
            arquivosGerados = relatorioService.gerarPdfParaLojas(new ArrayList<>(lojasSelecionadas), dtIni, dtFim);
            addMsg(FacesMessage.SEVERITY_INFO, "OK",
                    "Relatórios gerados: " + (arquivosGerados == null ? 0 : arquivosGerados.size()));
        } catch (Exception e) {
            addMsg(FacesMessage.SEVERITY_ERROR, "Erro", e.getMessage());
        }
    }

    public void gerarEEnviar() {
        if (!validarParametros()) return;

        String dtIni = FMT.format(dataInicialDate);
        String dtFim = FMT.format(dataFinalDate);

        try {
            arquivosGerados = relatorioService.gerarPdfParaLojas(new ArrayList<>(lojasSelecionadas), dtIni, dtFim);

            if (arquivosGerados == null || arquivosGerados.isEmpty()) {
                addMsg(FacesMessage.SEVERITY_WARN, "Atenção", "Nenhum PDF foi gerado para o período.");
                return;
            }
            notificacaoService.notificarRelatorioPrecosAlteradosComAnexos(arquivosGerados, dtIni, dtFim);
            addMsg(FacesMessage.SEVERITY_INFO, "OK", "Relatórios gerados e e-mail enviado com anexos.");
        } catch (Exception e) {
            addMsg(FacesMessage.SEVERITY_ERROR, "Erro", e.getMessage());
        }
    }

    /**
     * Gera e já faz o download no MESMO clique (stream direto no HttpServletResponse).
     * - Se 1 arquivo: baixa o PDF.
     * - Se >1 arquivo: compacta e baixa um ZIP.
     */
    public String gerarEDownload() {
        if (!validarParametros()) return null;

        String dtIni = FMT.format(dataInicialDate);
        String dtFim = FMT.format(dataFinalDate);

        try {
            arquivosGerados = relatorioService.gerarPdfParaLojas(new ArrayList<>(lojasSelecionadas), dtIni, dtFim);

            if (arquivosGerados == null || arquivosGerados.isEmpty()) {
                addMsg(FacesMessage.SEVERITY_WARN, "Atenção", "Nenhum PDF foi gerado para o período.");
                return null;
            }

            File arquivoParaBaixar;
            String contentType;
            String downloadName;

            if (arquivosGerados.size() == 1) {
                arquivoParaBaixar = new File(arquivosGerados.get(0));
                contentType = "application/pdf";
                downloadName = arquivoParaBaixar.getName();
            } else {
                File base = new File(arquivosGerados.get(0)).getParentFile();
                String nomeZip = "relatorios_precos_alterados_" +
                        new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".zip";
                File zip = new File(base, nomeZip);
                zipFiles(arquivosGerados, zip);

                arquivoParaBaixar = zip;
                contentType = "application/zip";
                downloadName = zip.getName();
            }

            streamFileToResponse(arquivoParaBaixar, contentType, downloadName);
            FacesContext.getCurrentInstance().responseComplete(); // muito importante!
            return null; // permanece na mesma view
        } catch (Exception e) {
            addMsg(FacesMessage.SEVERITY_ERROR, "Erro", e.getMessage());
            return null;
        }
    }

    private void streamFileToResponse(File file, String contentType, String downloadName) throws Exception {
        ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
        HttpServletResponse response = (HttpServletResponse) ec.getResponse();

        response.reset();
        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + downloadName + "\"");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setContentLengthLong(file.length());

        try (FileInputStream in = new FileInputStream(file);
             OutputStream out = response.getOutputStream()) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) {
                out.write(buf, 0, r);
            }
            out.flush();
        }
    }

    private void addMsg(FacesMessage.Severity sev, String sum, String det) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(sev, sum, det));
    }

    // Getters/Setters
    public List<Loja> getLojas() { return lojas; }
    public List<Long> getLojasSelecionadas() { return lojasSelecionadas; }
    public void setLojasSelecionadas(List<Long> lojasSelecionadas) { this.lojasSelecionadas = lojasSelecionadas; }
    public Date getDataInicialDate() { return dataInicialDate; }
    public void setDataInicialDate(Date dataInicialDate) { this.dataInicialDate = dataInicialDate; }
    public Date getDataFinalDate() { return dataFinalDate; }
    public void setDataFinalDate(Date dataFinalDate) { this.dataFinalDate = dataFinalDate; }
    public List<String> getArquivosGerados() { return arquivosGerados; }

    // ZIP util
    private static void zipFiles(List<String> paths, File outZip) throws Exception {
        try (java.util.zip.ZipOutputStream zos =
                     new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(outZip))) {
            byte[] buffer = new byte[8192];
            for (String p : paths) {
                File f = new File(p);
                if (!f.exists() || !f.isFile()) continue;
                try (java.io.FileInputStream fis = new java.io.FileInputStream(f)) {
                    java.util.zip.ZipEntry ze = new java.util.zip.ZipEntry(f.getName());
                    zos.putNextEntry(ze);
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                    zos.closeEntry();
                }
            }
        }
    }
}
