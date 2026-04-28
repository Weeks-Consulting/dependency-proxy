package us.weeksconsulting.dependency_proxy.manager;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.input.TeeInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import us.weeksconsulting.dependency_proxy.config.ApplicationConfig;
import us.weeksconsulting.dependency_proxy.config.model.Repository;
import us.weeksconsulting.dependency_proxy.dao.RepositoryCacheEntryDao;
import us.weeksconsulting.dependency_proxy.exception.UnknownRepositoryException;
import us.weeksconsulting.dependency_proxy.model.RepositoryCacheEntry;
import us.weeksconsulting.dependency_proxy.service.FileService;

@Component
public class CacheManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(CacheManager.class);

  private final Map<String, Repository> rawRepositories;
  private final RepositoryCacheEntryDao repoDao;
  private final FileService fileService;

  public CacheManager(
      ApplicationConfig appConfig,
      RepositoryCacheEntryDao repoDao,
      FileService fileService) {
    this.rawRepositories = appConfig.getRepositories().getRawRepositories();
    this.repoDao = repoDao;
    this.fileService = fileService;
  }

  public ResponseEntity<StreamingResponseBody> getThroughCache(
      String repositoryType,
      String repositoryName,
      String urlPath,
      MultiValueMap<String, String> urlParams)
      throws IOException {

    Repository repo = rawRepositories.get(repositoryName);

    if (repo == null) {
      throw new UnknownRepositoryException(repositoryName);
    }

    LOGGER.trace("repo: {}", repo);

    LOGGER.trace("Acquiring read lock on repo");
    final RepositoryCacheEntry existingRepositoryCacheEntry = repoDao.getCacheEntry(
        repositoryType,
        repositoryName,
        urlPath,
        urlParams);
    LOGGER.trace("Acquired read lock on repo");

    LOGGER.trace("repositoryCacheEntry: {}", existingRepositoryCacheEntry);

    if (existingRepositoryCacheEntry == null || !existingRepositoryCacheEntry.isCached()) {
      LOGGER.trace(
          "Cache Entry Not Found - repositoryType: {}, repositoryName: {}, urlPath: {}, urlParams: {}",
          repositoryType,
          repositoryName,
          urlPath,
          urlParams);
      String url = repo.getBaseUrl() + urlPath;
      LOGGER.trace("url: {}", url);
      return RestClient.create().get().uri(url).exchange((request, response) -> {
        LOGGER.trace("responseBody: {}", response.getBody());

        LOGGER.trace("responseHeaders: {}", response.getHeaders());
        MediaType contentType = response.getHeaders().getContentType();
        HttpHeaders responseHeaders = new HttpHeaders();

        responseHeaders.setContentType(contentType);

        RepositoryCacheEntry repositoryCacheEntry;

        if (existingRepositoryCacheEntry == null) {
          repositoryCacheEntry = repoDao.insertGetCacheEntry(
              repositoryType,
              repositoryName,
              urlPath,
              urlParams,
              contentType);
        } else {
          repositoryCacheEntry = existingRepositoryCacheEntry;
        }

        LOGGER.trace("repositoryCacheEntry: {}", repositoryCacheEntry);

        String cacheObjectPath = repositoryCacheEntry.getCacheObjectPath();
        UUID cacheObjectId = repositoryCacheEntry.getCacheObjectId();

        AtomicReference<Boolean> excludeFromCache = new AtomicReference<>();
        excludeFromCache.set(false);
        repo.getExcludes().forEach(excludePath -> {
          if (urlPath.startsWith(excludePath)) {
            excludeFromCache.set(true);
            LOGGER.info(
                "Skip Caching for Excluded Path - {} - {}",
                excludePath,
                urlPath);
          }
        });

        if (!response.getStatusCode().isSameCodeAs(HttpStatus.OK)) {
          LOGGER.warn(
              "Skip Caching for HTTP Status Code {} - {} - {}",
              response.getStatusCode().value(),
              response.getStatusText(),
              url);
          excludeFromCache.set(true);
        }

        InputStream inputStream;
        if (excludeFromCache.get()) {
          inputStream = response.getBody();
        } else {
          inputStream = getAndCache(response.getBody(), cacheObjectPath, cacheObjectId);
        }

        LOGGER.trace("Returning ResponseEntity");
        return ResponseEntity.ok()
            .headers(responseHeaders)
            .body(outputStream -> inputStream.transferTo(outputStream));
      }, false);
    } else {
      LOGGER.trace(
          "Cache Entry Found - repositoryType: {}, repositoryName: {}, urlPath: {}, urlParams: {}",
          repositoryType, repositoryName, urlPath, urlParams);

      HttpHeaders responseHeaders = new HttpHeaders();
      responseHeaders.add(CONTENT_TYPE, existingRepositoryCacheEntry.getMimeType());
      String cacheObjectPath = existingRepositoryCacheEntry.getCacheObjectPath();
      UUID cacheObjectId = existingRepositoryCacheEntry.getCacheObjectId();
      InputStream inputStream = fileService.readFileFromCache(cacheObjectPath, cacheObjectId);
      LOGGER.trace("Returning ResponseEntity");
      return ResponseEntity.ok()
          .headers(responseHeaders)
          .body(outputStream -> inputStream.transferTo(outputStream));
    }
  }

  private InputStream getAndCache(
      InputStream inputStream,
      String cacheObjectPath,
      UUID cacheObjectId) throws IOException {

    PipedInputStream pipedInputStream = new PipedInputStream(1048576);
    PipedOutputStream pipedOutputStream = new PipedOutputStream(pipedInputStream);
    TeeInputStream teeInputStream = new TeeInputStream(inputStream, pipedOutputStream);
    fileService.writeFileAsync(teeInputStream, pipedOutputStream, cacheObjectPath, cacheObjectId);
    LOGGER.trace("Returning cacheFile from pipedInputStream");
    return pipedInputStream;
  }
}
