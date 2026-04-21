package us.weeksconsulting.dependencyproxy.config.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import us.weeksconsulting.dependencyproxy.config.ApplicationConfig;
import us.weeksconsulting.dependencyproxy.config.dao.RepositoryCacheEntryDao;
import us.weeksconsulting.dependencyproxy.config.model.RepositoryCacheEntry;

@Service
public class FileService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileService.class);

    private final ApplicationConfig appConfig;
    private final RepositoryCacheEntryDao repoDao;

    public FileService(RepositoryCacheEntryDao repoDao, ApplicationConfig appConfig) {
        this.appConfig = appConfig;
        this.repoDao = repoDao;
    }

    @Async
    @Transactional
    public void writeFileAsync(
            InputStream inputStream,
            OutputStream outputStream,
            UUID cacheObjectId,
            File cacheDirectory,
            File cacheFile) throws IOException {
        LOGGER.trace("writeFileAsync started");

        String storageLocation = appConfig.getStorage().getLocation();
        LOGGER.trace("storageLocation: {}", storageLocation);

        RepositoryCacheEntry repoEntry = repoDao.getCacheEntryForUpdate(cacheObjectId);

        if (repoEntry == null) {
            LOGGER.warn("Unable to get row lock. Assuming another thread is already caching this data");
            IOUtils.consume(inputStream);
            inputStream.close();
            outputStream.close();
        } else {
            if (!cacheDirectory.exists()) {
                LOGGER.trace("cacheDirectory does not exist creating ...");
                cacheDirectory.mkdirs();
            }            
            LOGGER.trace("cacheFile length: {}", cacheFile.length());

            try {
                cacheFile.createNewFile();
                Files.copy(inputStream, cacheFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } finally {
                inputStream.close();
                outputStream.close();
            }

            LOGGER.trace("cacheFile length: {}", cacheFile.length());

            repoDao.updateCacheEntry(cacheObjectId, true);
        }

        LOGGER.trace("writeFileAsync finished");
    }
}
