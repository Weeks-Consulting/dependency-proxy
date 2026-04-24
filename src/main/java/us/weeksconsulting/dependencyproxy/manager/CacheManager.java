package us.weeksconsulting.dependencyproxy.manager;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Path;
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

import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import us.weeksconsulting.dependencyproxy.config.ApplicationConfig;
import us.weeksconsulting.dependencyproxy.config.model.Repository;
import us.weeksconsulting.dependencyproxy.config.model.Storage;
import us.weeksconsulting.dependencyproxy.dao.RepositoryCacheEntryDao;
import us.weeksconsulting.dependencyproxy.model.RepositoryCacheEntry;
import us.weeksconsulting.dependencyproxy.service.FileService;

@Component
public class CacheManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(CacheManager.class);

  // private final ApplicationConfig appConfig;

  private final S3Template s3Template;

  private final RepositoryCacheEntryDao repoDao;
  private final FileService fileService;

  private final Map<String, Map<String, Repository>> repositories;
  private final Storage storage;

  public CacheManager(
      ApplicationConfig appConfig,
      S3Template s3Template,
      RepositoryCacheEntryDao repoDao,
      FileService fileService) {
    this.s3Template = s3Template;
    this.repositories = appConfig.getRepositories();
    this.storage = appConfig.getStorage();

    this.repoDao = repoDao;
    this.fileService = fileService;
  }

  public ResponseEntity<StreamingResponseBody> getOrCache(
      String repositoryType,
      String repositoryName,
      String urlPath,
      Map<String, String> urlParams)
      throws IOException {

    Repository repo = repositories.get(repositoryType).get(repositoryName);

    LOGGER.trace("repo: {}", repo);

    final RepositoryCacheEntry existingRepositoryCacheEntry = repoDao.getCacheEntry(
        repositoryType,
        repositoryName,
        urlPath,
        urlParams);

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

        LOGGER.trace("Calling getOrCache");
        InputStream inputStream = getOrCache(
            response.getBody(),
            repositoryCacheEntry.getCacheObjectPath(),
            repositoryCacheEntry.getCacheObjectId(),
            false);
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

      LOGGER.trace("Calling getOrCache");
      InputStream inputStream = getOrCache(
          null,
          existingRepositoryCacheEntry.getCacheObjectPath(),
          existingRepositoryCacheEntry.getCacheObjectId(),
          true);
      LOGGER.trace("Returning ResponseEntity");
      return ResponseEntity.ok()
          .headers(responseHeaders)
          .body(outputStream -> inputStream.transferTo(outputStream));
    }
  }

  private InputStream getOrCache(
      InputStream inputStream,
      String cacheObjectPath,
      UUID cacheObjectId,
      boolean isCached) throws IOException {

    String cacheType = storage.getType();

    switch (cacheType) {
      case "local":
        return getOrCacheLocal(inputStream, cacheObjectPath, cacheObjectId, isCached);
      case "s3":
        return getOrCacheS3(inputStream, cacheObjectPath, cacheObjectId, isCached);
      default:
        return null;
    }
  }

  private InputStream getOrCacheLocal(
      InputStream inputStream,
      String cacheObjectPath,
      UUID cacheObjectId,
      boolean isCached) throws IOException {

    Path cacheDirectory = Path.of(storage.getLocation() + cacheObjectPath);
    File cacheFile = new File(cacheDirectory + File.separator + cacheObjectId);

    LOGGER.trace("cacheDirectory: {}", cacheDirectory);
    LOGGER.trace("cacheFile: {}", cacheFile.getPath());

    LOGGER.trace("isCached: {}", isCached);
    if (!isCached) {
      PipedInputStream pipedInputStream = new PipedInputStream(1048576);
      PipedOutputStream pipedOutputStream = new PipedOutputStream(pipedInputStream);
      TeeInputStream teeInputStream = new TeeInputStream(inputStream, pipedOutputStream);
      fileService.writeFileAsync(teeInputStream, pipedOutputStream, cacheObjectId, cacheDirectory, cacheFile);

      LOGGER.trace("Returning cacheFile from pipedInputStream");
      return pipedInputStream;
    } else {
      LOGGER.trace("cacheFile length: {}", cacheFile.length());
      LOGGER.trace("Returning cacheFile from {}", cacheFile.getAbsolutePath());
      return new FileInputStream(cacheFile);
    }
  }

  private InputStream getOrCacheS3(
      InputStream inputStream,
      String cacheObjectPath,
      UUID cacheObjectId,
      boolean isCached) throws IOException {

    LOGGER.trace(
        "getOrCacheS3 - cacheObjectPath: {}, cacheObjectId: {}, isCached: {}",
        cacheObjectPath,
        cacheObjectId,
        isCached);

    String bucket = storage.getLocation();
    String s3ObjectKey = cacheObjectPath.substring(1) + "/" + cacheObjectId;
    LOGGER.trace("s3ObjectKey: {}", s3ObjectKey);

    LOGGER.trace("isCached: {}", isCached);
    if (!isCached) {
      PipedInputStream pipedInputStream = new PipedInputStream(1048576);
      PipedOutputStream pipedOutputStream = new PipedOutputStream(pipedInputStream);
      TeeInputStream teeInputStream = new TeeInputStream(inputStream, pipedOutputStream);
      fileService.writeS3Async(teeInputStream, pipedOutputStream, cacheObjectId, s3ObjectKey);

      LOGGER.trace("Returning cacheFile from pipedInputStream");
      return pipedInputStream;
    } else {
      S3Resource s3Resource = s3Template.download(bucket, s3ObjectKey);
      LOGGER.trace("cacheFile length: {}", s3Resource.contentLength());

      String s3Path = "s3://" + s3Resource.getLocation().getBucket() + "/" + s3Resource.getLocation().getObject();
      LOGGER.trace("Returning cacheFile from {}", s3Path);

      LOGGER.trace("s3Resource: {}", s3Resource);
      return s3Resource.getInputStream();
    }
  }
}
