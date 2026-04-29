package us.weeksconsulting.dependency_proxy.manager;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.input.TeeInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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

  @Transactional
  public ResponseEntity<StreamingResponseBody> get(
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

    // Acquires a shared lock on cache entry.
    RepositoryCacheEntry repositoryCacheEntry = repoDao.insertGetCacheEntry(
        repositoryType,
        repositoryName,
        urlPath,
        urlParams);

    LOGGER.trace("repositoryCacheEntry: {}", repositoryCacheEntry);

    String url = repo.getBaseUrl() + urlPath;
    LOGGER.trace("url: {}", url);

    if (isExcluded(urlPath, repo)) {
      LOGGER.info("Bypassing Cache for Excluded URL - {}", url);
      return getBypassCache(url, repositoryCacheEntry);
    } else {
      return getFromCache(url, repositoryCacheEntry);
    }

  }

  private ResponseEntity<StreamingResponseBody> getFromCache(
      String url,
      RepositoryCacheEntry repositoryCacheEntry)
      throws IOException {
    boolean isCached = repositoryCacheEntry.isCached();
    InputStream inputStream;
    HttpHeaders headers = new HttpHeaders();
    HttpStatusCode statusCode;

    if (isCached) {
      LOGGER.debug("Cache Entry Found - {}", url);
      inputStream = streamFromCache(repositoryCacheEntry);
      headers.setContentType(repositoryCacheEntry.getMimeType());
      statusCode = HttpStatus.OK;
    } else {
      LOGGER.debug("Cache Entry Not Found - {}", url);
      ClientHttpResponse response = getFromUrl(url);
      statusCode = response.getStatusCode();
      MediaType contentType = response.getHeaders().getContentType();
      headers.setContentType(contentType);
      InputStream responseBody = response.getBody();
      if (statusCode.isSameCodeAs(HttpStatus.OK)) {
        inputStream = streamThroughCache(responseBody, repositoryCacheEntry, contentType);
      } else {
        LOGGER.warn("Skip Caching for HTTP Status Code {} - {}", statusCode.value(), url);
        inputStream = responseBody;
      }

    }

    return ResponseEntity.status(statusCode)
        .headers(headers)
        .body(outputStream -> inputStream.transferTo(outputStream));

  }

  private ResponseEntity<StreamingResponseBody> getBypassCache(
      String url,
      RepositoryCacheEntry repositoryCacheEntry)
      throws IOException {

    ClientHttpResponse response = getFromUrl(url);
    HttpStatusCode statusCode = response.getStatusCode();
    MediaType contentType = response.getHeaders().getContentType();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(contentType);
    InputStream inputStream = response.getBody();

    return ResponseEntity.status(statusCode)
        .headers(headers)
        .body(outputStream -> inputStream.transferTo(outputStream));
  }

  private ClientHttpResponse getFromUrl(String url) {
    return RestClient.create().get().uri(url).exchange((request, response) -> response, false);

  }

  private InputStream streamThroughCache(
      InputStream inputStream,
      RepositoryCacheEntry repositoryCacheEntry,
      MediaType mimeType)
      throws IOException {
    String cacheObjectPath = repositoryCacheEntry.getCacheObjectPath();
    UUID cacheObjectId = repositoryCacheEntry.getCacheObjectId();

    PipedInputStream pipedInputStream = new PipedInputStream(1048576);
    PipedOutputStream pipedOutputStream = new PipedOutputStream(pipedInputStream);
    TeeInputStream teeInputStream = new TeeInputStream(inputStream, pipedOutputStream);
    fileService.writeFileAsync(teeInputStream, pipedOutputStream, cacheObjectPath, cacheObjectId, mimeType);
    LOGGER.trace("Returning cacheFile from pipedInputStream");
    return pipedInputStream;
  }

  private InputStream streamFromCache(RepositoryCacheEntry repositoryCacheEntry) throws IOException {
    String cacheObjectPath = repositoryCacheEntry.getCacheObjectPath();
    UUID cacheObjectId = repositoryCacheEntry.getCacheObjectId();
    return fileService.readFileFromCache(cacheObjectPath, cacheObjectId);
  }

  private boolean isExcluded(String urlPath, Repository repository) {
    return repository.getExcludes().stream().anyMatch(excludePath -> urlPath.startsWith(excludePath));
  }
}
