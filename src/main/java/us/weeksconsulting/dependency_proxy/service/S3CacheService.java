package us.weeksconsulting.dependency_proxy.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import io.awspring.cloud.s3.S3Exception;
import io.awspring.cloud.s3.S3Template;
import us.weeksconsulting.dependency_proxy.config.ApplicationConfig;
import us.weeksconsulting.dependency_proxy.dao.RepositoryCacheDao;

public class S3CacheService extends CacheService {

  private final S3Template s3Template;
  private final String s3Bucket;
  private final String s3Prefix;

  public S3CacheService(ApplicationConfig appConfig, RepositoryCacheDao repoDao, S3Template s3Template) {
    super(appConfig, repoDao);
    this.s3Template = s3Template;
    this.s3Bucket = storageConfig.bucket();
    this.s3Prefix = storageConfig.prefix();
  }

  @Override
  protected InputStream streamFromCache(
      UUID cacheObjectId,
      String cacheObjectPath) throws S3Exception, IOException {

    LOGGER.trace("streamFromCache -> cacheObjectId: {}", cacheObjectId);
    LOGGER.trace("streamFromCache -> cacheObjectPath: {}", cacheObjectPath);
    LOGGER.trace("streamFromCache -> s3Bucket: {}", s3Bucket);
    LOGGER.trace("streamFromCache -> s3Prefix: {}", s3Prefix);

    String s3Key = getS3Key(s3Prefix, cacheObjectPath, cacheObjectId);

    return s3Template.download(s3Bucket, s3Key).getInputStream();
  }

  @Override
  protected long streamThroughCache(
      UUID cacheObjectId,
      String cacheObjectPath,
      InputStream inputStream) throws S3Exception {

    LOGGER.trace("streamThroughCache -> cacheObjectId: {}", cacheObjectId);
    LOGGER.trace("streamThroughCache -> cacheObjectPath: {}", cacheObjectPath);
    LOGGER.trace("streamThroughCache -> s3Bucket: {}", s3Bucket);
    LOGGER.trace("streamThroughCache -> s3Prefix: {}", s3Prefix);

    String s3Key = getS3Key(s3Prefix, cacheObjectPath, cacheObjectId);

    return s3Template.upload(s3Bucket, s3Key, inputStream).contentLength();
  }

  @Override
  protected void deleteFromCache(UUID cacheObjectId, String cacheObjectPath) throws S3Exception {
    LOGGER.trace("deleteFromCache -> cacheObjectId: {}", cacheObjectId);
    LOGGER.trace("deleteFromCache -> cacheObjectPath: {}", cacheObjectPath);
    LOGGER.trace("deleteFromCache -> s3Bucket: {}", s3Bucket);
    LOGGER.trace("deleteFromCache -> s3Prefix: {}", s3Prefix);

    String s3Key = getS3Key(s3Prefix, cacheObjectPath, cacheObjectId);

    LOGGER.debug("Deleting s3://{}/{} from cache", s3Bucket, s3Key);
    s3Template.deleteObject(s3Bucket, s3Key);
  }

  private String getS3Key(String s3Prefix, String cacheObjectPath, UUID cacheObjectId) {
    String s3Key = (s3Prefix.isEmpty() ? "" : s3Prefix + "/") + cacheObjectPath + "/" + cacheObjectId.toString();
    LOGGER.trace("s3Key: {}", s3Key);
    return s3Key;
  }
}
