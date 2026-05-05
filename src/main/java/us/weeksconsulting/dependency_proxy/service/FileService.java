package us.weeksconsulting.dependency_proxy.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import software.amazon.awssdk.services.s3.model.S3Exception;
import us.weeksconsulting.dependency_proxy.config.ApplicationConfig;
import us.weeksconsulting.dependency_proxy.dao.RepositoryCacheEntryDao;
import us.weeksconsulting.dependency_proxy.exception.CachingException;
import us.weeksconsulting.dependency_proxy.exception.MissingHashAlgorithmException;
import us.weeksconsulting.dependency_proxy.exception.UnknownStorageTypeException;

@Service
public class FileService {
  private static final Logger LOGGER = LoggerFactory.getLogger(FileService.class);

  private final S3Template s3Template;
  private final RepositoryCacheEntryDao repoDao;
  private final String storageType;
  private final String storagePath;
  private final String storageBucket;

  public FileService(RepositoryCacheEntryDao repoDao, ApplicationConfig appConfig, S3Template s3Template) {
    this.repoDao = repoDao;
    this.storageType = appConfig.getStorage().getType();
    this.storagePath = appConfig.getStorage().getPath();
    this.storageBucket = appConfig.getStorage().getBucket();
    this.s3Template = s3Template;
  }

  public InputStream readFileFromCache(
      String cacheObjectPath,
      UUID cacheObjectId) throws IOException {
    switch (storageType) {
      case "local":
        Path cacheDirectory = Path.of(storagePath + File.separator + cacheObjectPath);
        File cacheFile = new File(cacheDirectory + File.separator + cacheObjectId);

        LOGGER.trace("cacheFile length: {}", cacheFile.length());
        LOGGER.trace("Returning cacheFile from {}", cacheFile.getAbsolutePath());
        return new FileInputStream(cacheFile);
      case "s3":
        String s3ObjectKey = Optional.ofNullable(storagePath).orElse("") + cacheObjectPath.replace(File.separatorChar, '/') + '/' + cacheObjectId.toString();
        LOGGER.trace("s3ObjectKey: {}", s3ObjectKey);
        S3Resource s3Resource = s3Template.download(storageBucket, s3ObjectKey);
        LOGGER.trace("cacheFile length: {}", s3Resource.contentLength());

        // Ignore rule about hard coded URL's since you have to include the path
        // separator in an S3 URL.
        @SuppressWarnings("java:S1075")
        String s3Path = "s3://" + s3Resource.getLocation().getBucket() + "/" + s3Resource.getLocation().getObject();
        LOGGER.trace("Returning cacheFile from {}", s3Path);

        LOGGER.trace("s3Resource: {}", s3Resource);
        return s3Resource.getInputStream();
      default:
        throw new UnknownStorageTypeException(storageType);
    }
  }

  @Async
  @Transactional
  public void writeFileAsync(
      InputStream inputStream,
      OutputStream outputStream,
      String cacheObjectPath,
      UUID cacheObjectId,
      MediaType mimeType) throws IOException {
    LOGGER.trace("writeFileAsync started");
    LOGGER.trace("cacheObjectId: {}", cacheObjectId);
    LOGGER.trace("cacheObjectPath: {}", cacheObjectPath);

    LOGGER.trace("Acquiring Lock For - {}", cacheObjectId);
    Boolean acquiredLock = repoDao.getCacheEntryForUpdate(cacheObjectId);
    try {
      if (acquiredLock.equals(Boolean.TRUE)) {
        LOGGER.trace("Successfully Acquired Lock For - {}", cacheObjectId);
        writeCache(inputStream, cacheObjectPath, cacheObjectId, mimeType);
      } else {
        LOGGER.trace("Failed Acquiring Lock For - {}", cacheObjectId);
        IOUtils.consume(inputStream);
      }
    } catch (S3Exception | IOException exception) {
      // If the incoming piped input stream get's closed prematurely
      // assume the client disconnected mid download and abandon caching
      if (exception.getMessage().contains("Read end dead")
          || exception.getCause().getMessage().contains("Read end dead")) {
        LOGGER.warn("Client Download Interrupted");
      } else {
        LOGGER.error("Failed to Cache File", exception);
        throw new CachingException(exception);
      }
    } finally {
      outputStream.close();
    }

    LOGGER.trace("writeFileAsync finished");
  }

  private void writeCache(
      InputStream inputStream,
      String cacheObjectPath,
      UUID cacheObjectId,
      MediaType mimeType) throws IOException {
    try {
      MessageDigest cacheFileDigest;
      cacheFileDigest = MessageDigest.getInstance("SHA-256");
      DigestInputStream digestInputStream = new DigestInputStream(inputStream, cacheFileDigest);

      long cacheFileLength = -1;

      switch (storageType) {
        case "local":
          Path cacheObjectDirectoryPath = Path.of(storagePath, cacheObjectPath);
          Files.createDirectories(cacheObjectDirectoryPath);

          File cacheObjectFile = Path.of(storagePath, cacheObjectPath, cacheObjectId.toString()).toFile();
          boolean createdFile = cacheObjectFile.createNewFile();
          LOGGER.trace("createdFile: {}", createdFile);
          Files.copy(digestInputStream, cacheObjectFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
          cacheFileLength = cacheObjectFile.length();
          break;
        case "s3":
          String s3Bucket = storageBucket;
          LOGGER.trace("s3Bucket: {}", s3Bucket);
          String s3ObjectKey = Optional.ofNullable(storagePath).orElse("") + cacheObjectPath.replace(File.separatorChar, '/') + "/" + cacheObjectId.toString();
          LOGGER.trace("s3ObjectKey: {}", s3ObjectKey);

          S3Resource s3Resource = s3Template.upload(s3Bucket, s3ObjectKey, digestInputStream);
          cacheFileLength = s3Resource.contentLength();
          break;
        default:
          throw new UnknownStorageTypeException(storageType);
      }

      LOGGER.trace("cacheFile length: {}", cacheFileLength);

      String cacheFileHash = Hex.encodeHexString(cacheFileDigest.digest());
      LOGGER.trace("cacheFileHash: {}", cacheFileHash);

      repoDao.updateCacheEntry(cacheObjectId, cacheFileLength, cacheFileHash, mimeType, Boolean.TRUE);
    } catch (NoSuchAlgorithmException exception) {
      LOGGER.error("SHA-256 Hash Algorithm Not Available", exception);
      throw new MissingHashAlgorithmException(exception);
    }

  }

}
