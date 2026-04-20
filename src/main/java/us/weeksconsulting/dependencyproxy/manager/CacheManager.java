package us.weeksconsulting.dependencyproxy.manager;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import us.weeksconsulting.dependencyproxy.config.ApplicationConfig;
import us.weeksconsulting.dependencyproxy.config.dao.RepositoryCacheEntryDao;
import us.weeksconsulting.dependencyproxy.config.model.Repository;
import us.weeksconsulting.dependencyproxy.config.model.RepositoryCacheEntry;

@Component
public class CacheManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheManager.class);
    private final ApplicationConfig applicationConfig;

    private final RepositoryCacheEntryDao repoDao;

    public CacheManager(ApplicationConfig applicationConfig, RepositoryCacheEntryDao repoDao) {
        this.applicationConfig = applicationConfig;
        this.repoDao = repoDao;
    }

    @Transactional
    public ResponseEntity<StreamingResponseBody> getOrCache(String repositoryType, String repositoryName,
            String urlPath,
            Map<String, String> urlParams) throws IOException {

        Repository repo = applicationConfig.getRepositories().get(repositoryType).get(repositoryName);

        LOGGER.trace("repo: {}", repo);

        RepositoryCacheEntry existingRepositoryCacheEntry = repoDao.getRepositoryCacheEntry(
                repositoryType,
                repositoryName,
                urlPath,
                urlParams);

        LOGGER.trace("repositoryCacheEntry: {}", existingRepositoryCacheEntry);

        if (existingRepositoryCacheEntry == null) {
            LOGGER.trace("Cache Entry Not Found - repositoryType: {}, repositoryName: {}, urlPath: {}, urlParams: {}",
                    repositoryType, repositoryName, urlPath, urlParams);
            String url = repo.getBaseUrl() + urlPath;

            LOGGER.trace("url: {}", url);
            return RestClient.create().get().uri(url).exchange((request, response) -> {

                LOGGER.trace("responseHeaders: {}", response.getHeaders());
                List<String> contentHeaders = response.getHeaders().get(CONTENT_TYPE);
                String mimeType = null;
                if (contentHeaders != null && !contentHeaders.isEmpty()) {
                    mimeType = response.getHeaders().get(CONTENT_TYPE).getFirst();
                }

                RepositoryCacheEntry newRepositoryCacheEntry = repoDao.putRepositoryCacheEntry(repositoryType,
                        repositoryName, urlPath,
                        urlParams, mimeType);

                HttpHeaders responseHeaders = new HttpHeaders();

                if (newRepositoryCacheEntry.getMimeType() != null) {
                    responseHeaders.add(CONTENT_TYPE, mimeType);
                }

                InputStream inputStream = getOrCache(
                        response.getBody(),
                        newRepositoryCacheEntry.getObjectPath(),
                        newRepositoryCacheEntry.getObjectId());
                return ResponseEntity.ok()
                        .headers(responseHeaders)
                        .body(outputStream -> inputStream.transferTo(outputStream));
            });
        } else {
            LOGGER.trace("Cache Entry Found - repositoryType: {}, repositoryName: {}, urlPath: {}, urlParams: {}",
                    repositoryType, repositoryName, urlPath, urlParams);
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.add(CONTENT_TYPE, existingRepositoryCacheEntry.getMimeType());
            InputStream inputStream = getOrCache(
                    null,
                    existingRepositoryCacheEntry.getObjectPath(),
                    existingRepositoryCacheEntry.getObjectId());
            return ResponseEntity.ok()
                    .headers(responseHeaders)
                    .body(outputStream -> inputStream.transferTo(outputStream));
        }
    }

    private InputStream getOrCache(InputStream inputStream, String objectPath, UUID objectId) throws IOException {
        String cacheType = applicationConfig.getStorage().getType();

        switch (cacheType) {
            case "local":
                return getOrCacheLocal(inputStream, objectPath, objectId);
            case "s3":
                return getOrCacheS3(inputStream, objectPath, objectId);
            default:
                return null;
        }
    }

    private InputStream getOrCacheLocal(InputStream inputStream, String objectPath, UUID objectId) throws IOException {
        String storageLocation = applicationConfig.getStorage().getLocation();

        LOGGER.trace("storageLocation: {}", storageLocation);

        File cacheDirectory = new File(applicationConfig.getStorage().getLocation() + objectPath);

        LOGGER.trace("cacheDirectory: {}", cacheDirectory.getPath());

        if (!cacheDirectory.exists()) {
            LOGGER.trace("cacheDirectory does not exist creating ...");
            cacheDirectory.mkdirs();
        }

        File cacheFile = new File(cacheDirectory.getPath() + File.separator + String.valueOf(objectId));

        LOGGER.trace("cacheFile: {}", cacheFile.getPath());

        LOGGER.trace("cacheFile length: {}", cacheFile.length());

        if (!cacheFile.exists()) {
            LOGGER.trace("cacheFile does not exist creating ...");
            cacheFile.createNewFile();
            try (InputStream is = inputStream) {
                Files.copy(is, cacheFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }

        LOGGER.trace("cacheFile length: {}", cacheFile.length());

        LOGGER.trace("Returning cacheFile: {}", cacheFile.getAbsolutePath());

        return new FileInputStream(cacheFile);
    }

    private InputStream getOrCacheS3(InputStream inputStream, String objectPath, UUID objectId) {
        return null;
    }
}
