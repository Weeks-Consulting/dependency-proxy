package us.weeksconsulting.dependencyproxy.manager;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import us.weeksconsulting.dependencyproxy.config.ApplicationConfig;
import us.weeksconsulting.dependencyproxy.config.model.Repository;
import us.weeksconsulting.dependencyproxy.dao.RepositoryCacheEntryDao;
import us.weeksconsulting.dependencyproxy.model.RepositoryCacheEntry;
import us.weeksconsulting.dependencyproxy.service.FileService;
import us.weeksconsulting.dependencyproxy.util.BetterTeeInputStream;

@Component
public class CacheManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheManager.class);
    private final ApplicationConfig appConfig;

    private final RepositoryCacheEntryDao repoDao;
    private final FileService fileService;

    public CacheManager(ApplicationConfig appConfig, RepositoryCacheEntryDao repoDao,
            FileService fileService) {
        this.appConfig = appConfig;
        this.repoDao = repoDao;
        this.fileService = fileService;
    }

    public ResponseEntity<StreamingResponseBody> getOrCache(String repositoryType,
            String repositoryName, String urlPath, Map<String, String> urlParams)
            throws IOException {

        Repository repo = appConfig.getRepositories().get(repositoryType).get(repositoryName);

        LOGGER.trace("repo: {}", repo);

        final RepositoryCacheEntry existingRepositoryCacheEntry = repoDao.getCacheEntry(repositoryType, repositoryName,
                urlPath, urlParams);

        LOGGER.trace("repositoryCacheEntry: {}", existingRepositoryCacheEntry);

        if (existingRepositoryCacheEntry == null || !existingRepositoryCacheEntry.isCached()) {
            LOGGER.trace(
                    "Cache Entry Not Found - repositoryType: {}, repositoryName: {}, urlPath: {}, urlParams: {}",
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

                RepositoryCacheEntry repositoryCacheEntry;

                if (existingRepositoryCacheEntry == null) {
                    repositoryCacheEntry = repoDao.insertGetCacheEntry(repositoryType,
                            repositoryName, urlPath, urlParams, mimeType);
                    LOGGER.trace("repositoryCacheEntry: {}", repositoryCacheEntry);
                } else {
                    repositoryCacheEntry = existingRepositoryCacheEntry;
                    LOGGER.trace("repositoryCacheEntry: {}", repositoryCacheEntry);
                }

                HttpHeaders responseHeaders = new HttpHeaders();

                if (repositoryCacheEntry.getMimeType() != null) {
                    responseHeaders.add(CONTENT_TYPE, mimeType);
                }

                LOGGER.trace("Calling getOrCache");
                LOGGER.trace("Returning ResponseEntity");
                InputStream inputStream = response.getBody();
                return ResponseEntity.ok()
                        .headers(responseHeaders).body(outputStream -> {
                            BetterTeeInputStream teeInputStream = new BetterTeeInputStream(inputStream, outputStream);
                            LOGGER.trace("Calling getOrCache");
                            getOrCache(
                                    teeInputStream,
                                    repositoryCacheEntry.getCacheObjectPath(),
                                    repositoryCacheEntry.getCacheObjectId(),
                                    false);

                        });
            }, false);
        } else {
            LOGGER.trace(
                    "Cache Entry Found - repositoryType: {}, repositoryName: {}, urlPath: {}, urlParams: {}",
                    repositoryType, repositoryName, urlPath, urlParams);
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.add(CONTENT_TYPE, existingRepositoryCacheEntry.getMimeType());
            InputStream inputStream = getOrCache(null, existingRepositoryCacheEntry.getCacheObjectPath(),
                    existingRepositoryCacheEntry.getCacheObjectId(), true);
            LOGGER.trace("Returning ResponseEntity");
            return ResponseEntity.ok().headers(responseHeaders)
                    .body(outputStream -> inputStream.transferTo(outputStream));
        }
    }

    private InputStream getOrCache(InputStream inputStream, String cacheObjectPath,
            UUID cacheObjectId, boolean isCached) throws IOException {
        String cacheType = appConfig.getStorage().getType();

        switch (cacheType) {
            case "local":
                return getOrCacheLocal(inputStream, cacheObjectPath, cacheObjectId, isCached);
            case "s3":
                return getOrCacheS3(inputStream, cacheObjectPath, cacheObjectId, isCached);
            default:
                return null;
        }
    }

    private InputStream getOrCacheLocal(InputStream inputStream, String cacheObjectPath,
            UUID cacheObjectId, boolean isCached) throws IOException {

        File cacheDirectory = new File(appConfig.getStorage().getLocation() + cacheObjectPath);
        File cacheFile = new File(cacheDirectory.getPath() + File.separator + cacheObjectId);

        LOGGER.trace("cacheDirectory: {}", cacheDirectory.getPath());
        LOGGER.trace("cacheFile: {}", cacheFile.getPath());

        if (!isCached) {
            LOGGER.trace("isCached: {}", isCached);
            fileService.writeFileAsync(inputStream, cacheObjectId, cacheDirectory, cacheFile);

            LOGGER.trace("Returning null InputStream");
            return null;
        } else {
            LOGGER.trace("cacheFile length: {}", cacheFile.length());
            LOGGER.trace("Returning cacheFile from {}", cacheFile.getAbsolutePath());
            return new FileInputStream(cacheFile);
        }
    }

    private InputStream getOrCacheS3(InputStream inputStream, String cacheObjectPath,
            UUID cacheObjectId, boolean isCached) {
        throw new UnsupportedOperationException("S3 Support Not Implemented");
    }
}
