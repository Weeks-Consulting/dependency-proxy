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

import io.awspring.cloud.s3.S3Exception;
import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import us.weeksconsulting.dependencyproxy.config.ApplicationConfig;
import us.weeksconsulting.dependencyproxy.config.model.Storage;
import us.weeksconsulting.dependencyproxy.dao.RepositoryCacheEntryDao;
import us.weeksconsulting.dependencyproxy.model.RepositoryCacheEntry;

@Service
public class FileService {
  private static final Logger LOGGER = LoggerFactory.getLogger(FileService.class);

  private final S3Template s3Template;
  private final RepositoryCacheEntryDao repoDao;
  private final Storage storage;

  public FileService(RepositoryCacheEntryDao repoDao, ApplicationConfig appConfig, S3Template s3Template) {
    this.repoDao = repoDao;
    this.storage = appConfig.getStorage();
    this.s3Template = s3Template;
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

    try {

      if (repoEntry == null) {
        LOGGER.debug("Unable to get row lock. Assuming another thread is already caching this data");
        IOUtils.consume(inputStream);
        outputStream.close();
      } else {
        LOGGER.trace("Acquired row lock. Caching Data to file storage");
        cacheDirectory.mkdirs();
        LOGGER.trace("cacheFile length: {}", cacheFile.length());

        try {

          MessageDigest fileHash;
          fileHash = MessageDigest.getInstance("SHA-256");
          DigestInputStream digestInputStream = new DigestInputStream(inputStream, fileHash);

          cacheFile.createNewFile();
          Files.copy(digestInputStream, cacheFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

          LOGGER.trace("cacheFile length: {}", cacheFile.length());

          String cacheFileHash = Hex.encodeHexString(fileHash.digest());

          LOGGER.trace("cacheFileHash: {}", cacheFileHash);

          repoDao.updateCacheEntry(cacheObjectId, cacheFile.length(), cacheFileHash, true);
        } catch (NoSuchAlgorithmException e) {
          LOGGER.error("SHA-256 Hash Algorithm Not Available", e);
          throw new RuntimeException(e);
        } finally {
          outputStream.close();
        }

      }
    } catch (IOException ex) {
      // If the incoming piped input stream get's closed prematurely assume the
      // client disconnected mid download and abandon the caching operation
      // and attempt to cleanup
      if (ex.getMessage().contains("Read end dead")) {
        LOGGER.warn("Client Download Interrupted - Skipping Cache Download and Attempting Cleanup");
        cacheFile.delete();
      } else {
        LOGGER.error("{}", ex);
        throw ex;
      }
    }

    LOGGER.trace("writeFileAsync finished");
  }

  @Async
  public void writeS3Async(InputStream inputStream,
      OutputStream outputStream,
      UUID cacheObjectId,
      String s3ObjectKey) throws IOException {

    LOGGER.trace("writeS3Async started");
    LOGGER.trace("cacheObjectId: {}", cacheObjectId);
    LOGGER.trace("s3ObjectKey: {}", s3ObjectKey);

    String bucket = storage.getLocation();
    LOGGER.trace("bucket: {}", bucket);

    RepositoryCacheEntry repoEntry = repoDao.getCacheEntryForUpdate(cacheObjectId);
    LOGGER.trace("repoEntry: {}", repoEntry);

    if (repoEntry == null) {
      LOGGER.debug("Unable to get row lock. Assuming another thread is already caching this data");
      IOUtils.consume(inputStream);
      outputStream.close();
    } else {
      LOGGER.trace("Acquired row lock. Caching Data to file storage");
      // LOGGER.trace("cacheFile length: {}", cacheFile.length());

      try {
        MessageDigest fileHash;
        fileHash = MessageDigest.getInstance("SHA-256");
        DigestInputStream digestInputStream = new DigestInputStream(inputStream, fileHash);

        S3Resource s3Resource = s3Template.upload(bucket, s3ObjectKey, digestInputStream);

        LOGGER.trace("cacheFile length: {}", s3Resource.contentLength());

        String cacheFileHash = Hex.encodeHexString(fileHash.digest());

        LOGGER.trace("cacheFileHash: {}", cacheFileHash);

        repoDao.updateCacheEntry(cacheObjectId, s3Resource.contentLength(), cacheFileHash, true);
      } catch (NoSuchAlgorithmException e) {
        LOGGER.error("SHA-256 Hash Algorithm Not Available", e);
        throw new RuntimeException(e);
      } catch (S3Exception ex) {
        // If the incoming piped input stream get's closed prematurely assume the
        // client disconnected mid download and abandon the caching operation
        // and attempt to cleanup
        if (ex.getCause().getMessage().contains("Read end dead")) {
          LOGGER.warn("Client Download Interrupted - Skipping Cache Download and Attempting Cleanup");
          s3Template.deleteObject(bucket, s3ObjectKey);
        } else {
          LOGGER.error("{}", ex);
          throw ex;
        }
      } finally {
        outputStream.close();
      }
    }

    LOGGER.trace("writeS3Async finished");
  }
}
