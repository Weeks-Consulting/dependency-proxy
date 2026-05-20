package us.weeksconsulting.dependency_proxy.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import us.weeksconsulting.dependency_proxy.config.ApplicationConfig;
import us.weeksconsulting.dependency_proxy.dao.RepositoryCacheDao;

public class FileCacheService extends CacheService {

  private final String storagePath;

  public FileCacheService(ApplicationConfig appConfig, RepositoryCacheDao repoDao) {
    LOGGER.trace("FileCacheService -> appConfig: {}, repoDao: {}", appConfig, repoDao);
    super(appConfig, repoDao);
    this.storagePath = storageConfig.path();
  }

  @Override
  protected InputStream streamFromCache(
      UUID cacheObjectId,
      String cacheObjectPath) throws IOException {

    LOGGER.trace("streamFromCache -> cacheObjectId: {}", cacheObjectId);
    LOGGER.trace("streamFromCache -> cacheObjectPath: {}", cacheObjectPath);
    LOGGER.trace("streamFromCache -> storagePath: {}", storagePath);

    Path cacheObjectFile = Path.of(storagePath, cacheObjectPath, cacheObjectId.toString());
    return Files.newInputStream(cacheObjectFile);
  }

  @Override
  protected long streamThroughCache(
      UUID cacheObjectId,
      String cacheObjectPath,
      InputStream inputStream) throws IOException {

    LOGGER.trace("streamThroughCache -> cacheObjectId: {}", cacheObjectId);
    LOGGER.trace("streamThroughCache -> cacheObjectPath: {}", cacheObjectPath);
    LOGGER.trace("streamThroughCache -> storagePath: {}", storagePath);

    Path cacheObjectFilePath = Files.createDirectories(Path.of(storagePath, cacheObjectPath));
    Path cacheObjectFile = cacheObjectFilePath.resolve(cacheObjectId.toString());

    return Files.copy(inputStream, cacheObjectFile, StandardCopyOption.REPLACE_EXISTING);
  }

  @Override
  protected void deleteFromCache(
      UUID cacheObjectId,
      String cacheObjectPath) throws IOException {

    LOGGER.trace("deleteFromCache -> cacheObjectId: {}", cacheObjectId);
    LOGGER.trace("deleteFromCache -> cacheObjectPath: {}", cacheObjectPath);
    LOGGER.trace("deleteFromCache -> storagePath: {}", storagePath);

    Path file = Path.of(storagePath, cacheObjectPath, cacheObjectId.toString());
    LOGGER.debug("Deleting {} from cache", file);
    Files.delete(file);
  }

}
