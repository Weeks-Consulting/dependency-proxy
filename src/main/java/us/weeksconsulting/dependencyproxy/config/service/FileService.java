package us.weeksconsulting.dependencyproxy.config.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class FileService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileService.class);

    @Async
    public void writeFileAsync(InputStream fileInputStream, OutputStream outputStream, File file) throws IOException {
        LOGGER.trace("writeFileAsync started");
        try (InputStream inputStream = fileInputStream) {
            file.createNewFile();
            Files.copy(inputStream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        LOGGER.trace("writeFileAsync finished");
    }
}
