package us.weeksconsulting.dependency_proxy.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.input.TeeInputStream;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import io.awspring.cloud.s3.S3Exception;
import us.weeksconsulting.dependency_proxy.config.ApplicationConfig;
import us.weeksconsulting.dependency_proxy.config.model.Repository;
import us.weeksconsulting.dependency_proxy.config.model.Storage;
import us.weeksconsulting.dependency_proxy.dao.RepositoryCacheDao;
import us.weeksconsulting.dependency_proxy.exception.CachingException;

public abstract class CacheService {
  protected static final Logger LOGGER = LoggerFactory.getLogger(CacheService.class);

  protected final Storage storageConfig;
  private final Map<String, Map<String, Repository>> repositories;
  protected final RepositoryCacheDao repoDao;

  protected CacheService(ApplicationConfig appConfig, RepositoryCacheDao repoDao) {
    this.storageConfig = appConfig.storage();
    this.repositories = appConfig.repositories();
    this.repoDao = repoDao;
  }

  @Transactional
  public void readFromCache(
      UUID cacheObjectId,
      String cacheObjectPath,
      UUID lockId,
      OutputStream outputStream) throws IOException {
    LOGGER.trace("readFromCache -> cacheObjectId: {}, cacheObjectPath: {}, lockId: {}",
        cacheObjectId,
        cacheObjectPath,
        lockId);

    try {
      repoDao.lockCacheEntryForRead(cacheObjectId, lockId);
      streamFromCache(cacheObjectId, cacheObjectPath).transferTo(outputStream);
    } catch (Exception exception) {
      handleCachingException(exception);
    } finally {
      repoDao.unlockCacheEntryForRead(cacheObjectId, lockId);
      outputStream.close();
    }
  }

  @Transactional
  public void readThroughCache(
      UUID cacheObjectId,
      String cacheObjectPath,
      MediaType cacheObjectMimeType,
      InputStream inputStream,
      OutputStream outputStream) throws IOException {

    LOGGER.trace("readThroughCache -> cacheObjectId: {}, cacheObjectPath: {}, cacheObjectMimeType: {}",
        cacheObjectId,
        cacheObjectPath,
        cacheObjectMimeType);

    boolean acquiredLock = repoDao.lockCacheEntryForUpdate(cacheObjectId);
    try {
      if (acquiredLock) {
        LOGGER.trace("Successfully Acquired Lock For - {}", cacheObjectId);
        MessageDigest cacheFileDigest;
        cacheFileDigest = MessageDigest.getInstance("SHA-256");
        long cacheFileLength = -1;
        try (
            TeeInputStream teeInputStream = new TeeInputStream(inputStream, outputStream);
            DigestInputStream digestInputStream = new DigestInputStream(teeInputStream, cacheFileDigest)) {

          LOGGER.trace("Started Streaming Through Cache");
          cacheFileLength = streamThroughCache(cacheObjectId, cacheObjectPath, digestInputStream);
          LOGGER.trace("Finished Streaming Through Cache");
        }

        LOGGER.trace("cacheFile length: {}", cacheFileLength);
        String cacheFileHash = Hex.encodeHexString(cacheFileDigest.digest());
        LOGGER.trace("cacheFileHash: {}", cacheFileHash);

        repoDao.updateCacheEntry(cacheObjectId, cacheFileLength, cacheFileHash, cacheObjectMimeType, Boolean.TRUE);
      } else {
        LOGGER.trace("Failed Acquiring Lock For - {}", cacheObjectId);
        LOGGER.trace("Bypassing Cache");
        inputStream.transferTo(outputStream);
      }
    } catch (Exception exception) {
      handleCachingException(exception);
    } finally {
      inputStream.close();
    }

  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void cleanupCache() {
    LOGGER.trace("cleanupCache");

    repositories.forEach((repoType, repoMap) -> repoMap.forEach((repoName, repo) ->
    // This acquires an exclusive lock on these rows preventing any cache reads or
    // writes.
    repoDao.getExpiredCacheEntries(repoType, repoName, repo.cacheTTL()).forEach(repoCacheEntry -> {
      UUID cacheObjectId = repoCacheEntry.cacheObjectId();
      String cacheObjectPath = repoCacheEntry.cacheObjectPath();

      URI url = UriComponentsBuilder.fromUriString(repo.baseUrl() + repoCacheEntry.urlPath()).build(true).toUri();
      LOGGER.debug("Cleaning up expired cache for {}", url);
      try {
        deleteFromCache(cacheObjectId, cacheObjectPath);
        repoDao.deleteCacheEntry(cacheObjectId);
      } catch (Exception exception) {
        LOGGER.error("Error cleaning up cache for {}", repoCacheEntry, exception);
        // Mark file as no longer cached.
        repoDao.updateCacheEntry(cacheObjectId, null, cacheObjectPath, null, false);
        throw new CachingException(exception);
      }
    })));
  }

  protected abstract InputStream streamFromCache(
      UUID cacheObjectId,
      String cacheObjectPath) throws IOException, S3Exception;

  protected abstract long streamThroughCache(
      UUID cacheObjectId,
      String cacheObjectPath,
      InputStream inputStream) throws IOException, S3Exception;

  protected abstract void deleteFromCache(
      UUID cacheObjectId,
      String cacheObjectPath) throws IOException, S3Exception;

  private void handleCachingException(Exception exception) {
    Throwable cause = ExceptionUtils.getRootCause(exception);
    String message = cause.getMessage();

    LOGGER.trace("handleCachingException -> message: {}", message);

    // If the incoming piped input stream get's closed prematurely
    // assume the client disconnected mid download and abandon caching
    switch (message) {
      case "Broken pipe", "Connection reset by peer" -> LOGGER.warn("Client Download Interrupted");
      default -> {
        LOGGER.error("Failed to Cache File", exception);
        throw new CachingException(exception);
      }
    }
  }
}
