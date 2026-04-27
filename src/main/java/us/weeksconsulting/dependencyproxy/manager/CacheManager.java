package us.weeksconsulting.dependencyproxy.manager;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.input.TeeInputStream;
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

@Component
public class CacheManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(CacheManager.class);

  private final RepositoryCacheEntryDao repoDao;
  private final FileService fileService;

  private final Map<String, Map<String, Repository>> repositories;

  public CacheManager(
      ApplicationConfig appConfig,
      RepositoryCacheEntryDao repoDao,
      FileService fileService) {
    this.repositories = appConfig.getRepositories();

    this.repoDao = repoDao;
    this.fileService = fileService;
  }

  public ResponseEntity<StreamingResponseBody> getThroughCache(
      String repositoryType,
      String repositoryName,
      String urlPath,
      Map<String, String> urlParams)
      throws IOException {

    Repository repo = repositories.get(repositoryType).get(repositoryName);

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

        LOGGER.trace("responseHeaders: {}", response.getHeaders());
        List<String> contentHeaders = response.getHeaders().get(CONTENT_TYPE);
        HttpHeaders responseHeaders = new HttpHeaders();

        String mimeType = null;
        if (contentHeaders != null && !contentHeaders.isEmpty()) {
          mimeType = contentHeaders.getFirst();
          responseHeaders.add(CONTENT_TYPE, mimeType);
        }

        RepositoryCacheEntry repositoryCacheEntry;

        if (existingRepositoryCacheEntry == null) {
          repositoryCacheEntry = repoDao.insertGetCacheEntry(
              repositoryType,
              repositoryName,
              urlPath,
              urlParams,
              mimeType);
        } else {
          repositoryCacheEntry = existingRepositoryCacheEntry;
        }

        LOGGER.trace("repositoryCacheEntry: {}", repositoryCacheEntry);

        String cacheObjectPath = repositoryCacheEntry.getCacheObjectPath();
        UUID cacheObjectId = repositoryCacheEntry.getCacheObjectId();

        LOGGER.trace("Calling getOrCache for cacheObjectPath: {}, cacheObjectId: {}", cacheObjectPath, cacheObjectId);
        InputStream inputStream = getAndCache(response.getBody(), cacheObjectPath, cacheObjectId);
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
      LOGGER.trace("Calling getOrCache for cacheObjectPath: {}, cacheObjectId: {}", cacheObjectPath, cacheObjectId);
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
