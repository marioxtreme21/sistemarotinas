package sistema.rotinas.primefaces.adapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.primefaces.model.file.UploadedFile;
import org.springframework.web.multipart.MultipartFile;

/**
 * Classe adaptadora para converter UploadedFile do PrimeFaces em MultipartFile do Spring.
 */
public class MultipartFileAdapter implements MultipartFile {

    private final UploadedFile uploadedFile;

    public MultipartFileAdapter(UploadedFile uploadedFile) {
        this.uploadedFile = uploadedFile;
    }

    @Override
    public String getName() {
        return uploadedFile.getFileName();
    }

    @Override
    public String getOriginalFilename() {
        return uploadedFile.getFileName();
    }

    @Override
    public String getContentType() {
        return uploadedFile.getContentType();
    }

    @Override
    public boolean isEmpty() {
        return uploadedFile.getSize() == 0;
    }

    @Override
    public long getSize() {
        return uploadedFile.getSize();
    }

    @Override
    public byte[] getBytes() throws IOException {
        return uploadedFile.getContent();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return uploadedFile.getInputStream();
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        try (OutputStream out = new FileOutputStream(dest)) {
            out.write(getBytes());
        }
    }
}