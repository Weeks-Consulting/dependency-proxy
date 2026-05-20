package us.weeksconsulting.dependency_proxy.manager;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.UUID;

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
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.util.UriComponentsBuilder;

import us.weeksconsulting.dependency_proxy.config.ApplicationConfig;
import us.weeksconsulting.dependency_proxy.config.model.Repository;
import us.weeksconsulting.dependency_proxy.dao.RepositoryCacheDao;
import us.weeksconsulting.dependency_proxy.exception.UnknownRepositoryException;
import us.weeksconsulting.dependency_proxy.record.RepositoryCacheRecord;
import us.weeksconsulting.dependency_proxy.service.CacheService;

@Component
public class CacheManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(CacheManager.class);

  private final Map<String, Repository> rawRepositories;
  private final RepositoryCacheDao repoDao;
  private final CacheService cacheService;

  public CacheManager(
      ApplicationConfig appConfig,
      RepositoryCacheDao repoDao,
      CacheService cacheService) {
    this.rawRepositories = appConfig.repositories().get("raw");
    this.repoDao = repoDao;
    this.cacheService = cacheService;
  }

  @Transactional
  public ResponseEntity<StreamingResponseBody> get(
      String repositoryType,
      String repositoryName,
      String urlPath)
      throws IOException {

    Repository repo = rawRepositories.get(repositoryName);
    LOGGER.trace("repo: {}", repo);

    if (repo == null) {
      throw new UnknownRepositoryException(repositoryName);
    }

    // Add or fetch existing RepositoryCacheRecord
    // This establishes a shared read lock on the record and prevents deletion
    RepositoryCacheRecord repositoryCacheRecord = repoDao.insertGetCacheEntry(repositoryType, repositoryName, urlPath);
    LOGGER.trace("RepositoryCacheRecord: {}", repositoryCacheRecord);

    // Since we are proxying the request we do not encode the incoming URL
    URI url = UriComponentsBuilder.fromUriString(repo.baseUrl() + urlPath).build(true).toUri();
    LOGGER.trace("url: {}", url);

    if (repo.isExcluded(urlPath)) {
      LOGGER.info("Bypassing Cache for Excluded URL - {}", url);
      return getBypassCache(url);
    } else {
      return getThroughCache(url, repositoryCacheRecord);
    }

  }

  private ResponseEntity<StreamingResponseBody> getThroughCache(
      URI url,
      RepositoryCacheRecord repositoryCacheRecord) throws IOException {
    UUID cacheObjectId = repositoryCacheRecord.cacheObjectId();
    String cacheObjectPath = repositoryCacheRecord.cacheObjectPath();
    boolean cached = repositoryCacheRecord.cached();
    HttpHeaders headers = new HttpHeaders();

    if (cached) {
      LOGGER.debug("Cache Entry Found - {}", url);
      UUID lockId = UUID.randomUUID();
      repoDao.lockCacheEntryForRead(repositoryCacheRecord.cacheObjectId(), lockId);
      headers.setContentType(repositoryCacheRecord.cacheObjectMimeTypeAsMediaType());
      LOGGER.trace("Returning ResponseEntity From Cache");
      return ResponseEntity.ok()
          .headers(headers)
          .body(outputStream -> cacheService.readFromCache(cacheObjectId, cacheObjectPath, lockId, outputStream));
    } else {
      LOGGER.debug("Cache Entry Not Found - {}", url);
      ClientHttpResponse response = getFromUrl(url);
      HttpStatusCode statusCode = response.getStatusCode();
      MediaType cacheObjectMimeType = response.getHeaders().getContentType();
      headers.setContentType(cacheObjectMimeType);
      InputStream responseBody = response.getBody();
      if (statusCode.isSameCodeAs(HttpStatus.OK)) {
        LOGGER.trace("Returning ResponseEntity Through Cache");
        return ResponseEntity.ok()
            .headers(headers)
            .body(outputStream -> cacheService.readThroughCache(cacheObjectId, cacheObjectPath, cacheObjectMimeType,
                responseBody, outputStream));
      } else {
        LOGGER.warn("Skip Caching for HTTP Status Code {} - {}", statusCode.value(), url);
        return ResponseEntity.status(statusCode)
            .headers(headers)
            .body(responseBody::transferTo);
      }
    }
  }

  private ResponseEntity<StreamingResponseBody> getBypassCache(URI url) throws IOException {

    ClientHttpResponse response = getFromUrl(url);
    HttpHeaders headers = new HttpHeaders();
    HttpStatusCode statusCode = response.getStatusCode();
    MediaType cacheObjectMimeType = response.getHeaders().getContentType();
    headers.setContentType(cacheObjectMimeType);
    InputStream responseBody = response.getBody();

    LOGGER.trace("Returning ResponseEntity Bypassing Cache");
    return ResponseEntity.status(statusCode)
        .headers(headers)
        .body(responseBody::transferTo);
  }

  private ClientHttpResponse getFromUrl(URI url) {
    LOGGER.trace("getFromUrl -> url: {}", url);
    return RestClient.create().get().uri(url).exchange((request, response) -> response, false);
  }
}
