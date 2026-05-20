package us.weeksconsulting.dependency_proxy.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import us.weeksconsulting.dependency_proxy.config.ApplicationConfig;
import us.weeksconsulting.dependency_proxy.config.model.Cleanup;
import us.weeksconsulting.dependency_proxy.dao.RepositoryCacheDao;

@Service
public class CleanupService {
  private static final Logger LOGGER = LoggerFactory.getLogger(CleanupService.class);

  private final RepositoryCacheDao repoDao;
  private final CacheService cacheService;
  private final Cleanup cleanup;

  public CleanupService(ApplicationConfig appConfig, RepositoryCacheDao repoDao, CacheService cachService) {
    this.repoDao = repoDao;
    this.cacheService = cachService;
    this.cleanup = appConfig.cleanup();
  }

  @Scheduled(fixedDelayString = "${application.cleanup.schedule}", initialDelay = 30000)
  @Transactional
  public void cleanupTask() {
    LOGGER.trace("Cleanup Task Started");

    cleanupCacheLocks();
    cleanupCacheStorage();

    LOGGER.trace("Cleanup Task finished");
  }

  private void cleanupCacheLocks() {
    LOGGER.trace("cleanupCacheLocks started");
    repoDao.cleanupExpiredLocks(cleanup.lockTimeout());
    LOGGER.trace("cleanupCacheLocks finished");
  }

  private void cleanupCacheStorage() {
    LOGGER.trace("cleanupCacheStorage started");
    cacheService.cleanupCache();
    LOGGER.trace("cleanupCacheStorage finished");
  }

}
