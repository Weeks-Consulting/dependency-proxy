package us.weeksconsulting.dependencyproxy.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import us.weeksconsulting.dependencyproxy.config.ApplicationConfig;
import us.weeksconsulting.dependencyproxy.config.model.Storage;
import us.weeksconsulting.dependencyproxy.dao.RepositoryCacheEntryDao;
import us.weeksconsulting.dependencyproxy.model.RepositoryCacheEntry;

@Service
public class FileService {
  private static final Logger LOGGER = LoggerFactory.getLogger(FileService.class);

  private final RepositoryCacheEntryDao repoDao;
  private final Storage storage;

  public FileService(RepositoryCacheEntryDao repoDao, ApplicationConfig appConfig) {
    this.repoDao = repoDao;
    this.storage = appConfig.getStorage();
  }

  @Async
  @Transactional
  public void writeFileAsync(
      InputStream inputStream,
      OutputStream outputStream,
      UUID cacheObjectId,
      File cacheDirectory,
      File cacheFile) throws IOException {
    LOGGER.trace("writeFileAsync started");
    LOGGER.trace("cacheObjectId: {}", cacheObjectId);
    LOGGER.trace("cacheDirectory: {}", cacheDirectory);
    LOGGER.trace("cacheFile: {}", cacheFile);

    String storageLocation = storage.getLocation();
    LOGGER.trace("storageLocation: {}", storageLocation);

    RepositoryCacheEntry repoEntry = repoDao.getCacheEntryForUpdate(cacheObjectId);
    LOGGER.trace("repoEntry: {}", repoEntry);

    if (repoEntry == null) {
      LOGGER.warn("Unable to get row lock. Assuming another thread is already caching this data");
      IOUtils.consume(inputStream);
      outputStream.close();
    } else {
      cacheDirectory.mkdirs();
      LOGGER.trace("cacheFile length: {}", cacheFile.length());

      try {
        MessageDigest fileHash;
        fileHash = MessageDigest.getInstance("SHA-256");
        DigestInputStream digestInputStream = new DigestInputStream(inputStream, fileHash);

        cacheFile.createNewFile();
        Files.copy(digestInputStream, cacheFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        LOGGER.trace("cacheFile length: {}", cacheFile.length());

        LOGGER.trace("cacheFileHash: {}", Hex.encodeHexString(fileHash.digest()));

        repoDao.updateCacheEntry(cacheObjectId, true);
      } catch (NoSuchAlgorithmException e) {
        LOGGER.error("SHA-256 Hash Algorithm Not Available", e);
        throw new RuntimeException(e);
      } finally {
        outputStream.close();
      }

    }

    LOGGER.trace("writeFileAsync finished");
  }
}
