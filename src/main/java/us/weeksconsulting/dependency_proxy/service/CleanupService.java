package us.weeksconsulting.dependency_proxy.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.awspring.cloud.s3.S3Template;
import software.amazon.awssdk.services.s3.model.S3Exception;
import us.weeksconsulting.dependency_proxy.config.ApplicationConfig;
import us.weeksconsulting.dependency_proxy.config.model.Repository;
import us.weeksconsulting.dependency_proxy.config.model.Storage;
import us.weeksconsulting.dependency_proxy.dao.RepositoryCacheEntryDao;
import us.weeksconsulting.dependency_proxy.model.RepositoryCacheEntry;

@Service
public class CleanupService {
  private static final Logger LOGGER = LoggerFactory.getLogger(CleanupService.class);

  private final S3Template s3Template;
  private final RepositoryCacheEntryDao repoDao;
  private final Map<String, Repository> rawRepos;
  private final Storage storage;

  public CleanupService(RepositoryCacheEntryDao repoDao, ApplicationConfig appConfig, S3Template s3Template) {
    LOGGER.trace("CleanupService Constructed {}", this.hashCode());
    this.repoDao = repoDao;
    this.storage = appConfig.getStorage();
    this.rawRepos = appConfig.getRepositories().getRawRepositories();
    this.s3Template = s3Template;
  }

  @Scheduled(fixedDelayString = "${application.cleanup.schedule}", initialDelay = 30000)
  @Transactional
  public void cleanupTask() {
    LOGGER.trace("cleanupTask started");
    String cacheType = storage.getType();
    String cacheLocation = storage.getLocation();

    rawRepos.forEach((repoName, repoConfig) -> {
      String repoType = "raw";
      try (Stream<RepositoryCacheEntry> repositoryCacheEntryStream = repoDao.getExpiredCacheEntries(
          repoType,
          repoName,
          repoConfig.getCacheTTL())) {
        AtomicReference<Boolean> hasRows = new AtomicReference<>();
        hasRows.set(false);
        repositoryCacheEntryStream.forEach(repoCacheEntry -> {
          if (Boolean.FALSE.equals(hasRows.get())) {
            LOGGER.debug("Cleaning up repo: {}.{}", repoType, repoName);
            hasRows.set(true);
          }
          LOGGER.debug("Cleaning up url: {}", repoCacheEntry.getUrl());

          // Acquire an exclusive local on the row to be deleted before removing the file.
          LOGGER.trace("Waiting for lock on {}", repoCacheEntry.getCacheObjectId());
          repoDao.lockCacheEntryForDelete(repoCacheEntry.getCacheObjectId());
          LOGGER.trace("Acquired lock on {}", repoCacheEntry.getCacheObjectId());

          try {
            String cacheObjectPath = repoCacheEntry.getCacheObjectPath();
            UUID cacheObjectId = repoCacheEntry.getCacheObjectId();
            switch (cacheType) {
              case "local":
                Path file = Paths.get(cacheLocation, cacheObjectPath, cacheObjectId.toString());
                LOGGER.debug("Deleting {}", file);
                Files.delete(file);
                break;
              case "s3":
                String s3ObjectKey = cacheObjectPath + "/" + cacheObjectId;
                LOGGER.debug("Deleting s3://{}/{}", cacheLocation, s3ObjectKey);
                s3Template.deleteObject(cacheLocation, s3ObjectKey);
                break;
              default:
                LOGGER.error("Unknown storage type.");
                break;
            }
          } catch (IOException | S3Exception exception) {
            LOGGER.error("Failed to cleanup file, marking as not cached", exception);
          }

          // Delete cache entry once file has been removed and release the lock
          repoDao.deleteCacheEntry(repoCacheEntry.getCacheObjectId());

        });

      }

    });

    LOGGER.trace("cleanupTask finished");
  }
}
