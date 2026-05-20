package us.weeksconsulting.dependency_proxy.dao;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.annotation.RegisterReflection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import us.weeksconsulting.dependency_proxy.exception.MissingHashAlgorithmException;
import us.weeksconsulting.dependency_proxy.record.RepositoryCacheLockRecord;
import us.weeksconsulting.dependency_proxy.record.RepositoryCacheRecord;

@Component
@RegisterReflection(classes = RepositoryCacheRecord.class, memberCategories = {
    MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS })
public class RepositoryCacheDao {
  private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryCacheDao.class);

  private final JdbcClient jdbcClient;

  public RepositoryCacheDao(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  public Stream<RepositoryCacheRecord> getExpiredCacheEntries(
      String repositoryType,
      String repositoryName,
      Duration cacheTTL) {

    LOGGER.trace(
        "getExpiredCacheEntries - repositoryType: {}, repositoryName: {}, cacheTTL: {}",
        repositoryType,
        repositoryName,
        cacheTTL);

    return this.jdbcClient
        .sql("""
            select *
              from repository_cache
              where cached
                and repository_type = :repository_type
                and repository_name = :repository_name
                and cached_at < current_timestamp - (:cache_ttl * interval '1 second')
                and not exists (
                  select 1
                    from repository_cache_lock
                    where repository_cache.cache_object_id =  repository_cache_lock.cache_object_id
                  )
              for update skip locked
            """)
        .param(RepositoryCacheRecord.REPOSITORY_TYPE, repositoryType)
        .param(RepositoryCacheRecord.REPOSITORY_NAME, repositoryName)
        .param("cache_ttl", cacheTTL.getSeconds())
        .query(RepositoryCacheRecord.class)
        .stream();
  }

  public void cleanupExpiredLocks(Duration lockTimeout) {
    LOGGER.trace("cleanupExpiredLocks -> lockTimeout: {}", lockTimeout);

    this.jdbcClient
        .sql("""
            delete
              from repository_cache_lock
              where locked_at < current_timestamp - (:lock_timeout * interval '1 second')
            """)
        .param("lock_timeout", lockTimeout.getSeconds())
        .update();
  }

  public RepositoryCacheRecord getCacheEntry(
      String repositoryType,
      String repositoryName,
      String urlPath) {

    LOGGER.trace(
        "getCacheEntry - repositoryType: {}, repositoryName: {}, urlPath: {}",
        repositoryType,
        repositoryName,
        urlPath);

    return this.jdbcClient
        .sql("""
            select *
              from repository_cache
              where 1=1
                and repository_type = :repository_type
                and repository_name = :repository_name
                and url_path = :url_path
              for key share
            """)
        .param(RepositoryCacheRecord.REPOSITORY_TYPE, repositoryType)
        .param(RepositoryCacheRecord.REPOSITORY_NAME, repositoryName)
        .param(RepositoryCacheRecord.URL_PATH, urlPath)
        .query(RepositoryCacheRecord.class)
        .single();
  }

  public boolean cached(UUID cacheObjectId) {
    LOGGER.trace(
        "cached - cacheObjectId: {}",
        cacheObjectId);

    return this.jdbcClient
        .sql("""
            select cached
              from repository_cache
              where cache_object_id = :cache_object_id
              for key share skip locked
            """)
        .param(RepositoryCacheRecord.CACHE_OBJECT_ID, cacheObjectId)
        .query(Boolean.class)
        .single()
        .booleanValue();
  }

  public void lockCacheEntryForRead(UUID cacheObjectId, UUID lockId) {
    LOGGER.trace("lockCacheEntryForRead - cacheObjectId: {}, lockId: {}", cacheObjectId, lockId);

    jdbcClient
        .sql("""
            insert
              into repository_cache_lock (
                cache_object_id,
                lock_id,
                lock_type,
                locked_at
              )
              values(
                :cache_object_id,
                :lock_id,
                :lock_type,
                current_timestamp
              )
              on conflict(cache_object_id,lock_id) do update set locked_at = current_timestamp
            """)
        .param(RepositoryCacheLockRecord.CACHE_OBJECT_ID, cacheObjectId)
        .param(RepositoryCacheLockRecord.LOCK_ID, lockId)
        .param(RepositoryCacheLockRecord.LOCK_TYPE, RepositoryCacheLockRecord.READ_LOCK)
        .update();
  }

  public void unlockCacheEntryForRead(UUID cacheObjectId, UUID lockId) {
    LOGGER.trace("unlockCacheEntryForRead - cacheObjectId: {}, lockId: {}", cacheObjectId, lockId);

    jdbcClient
        .sql("""
            delete
              from repository_cache_lock
              where cache_object_id = :cache_object_id and lock_id = :lock_id
            """)
        .param(RepositoryCacheLockRecord.CACHE_OBJECT_ID, cacheObjectId)
        .param(RepositoryCacheLockRecord.LOCK_ID, lockId)
        .update();
  }

  public boolean lockCacheEntryForUpdate(UUID cacheObjectId) {
    LOGGER.trace(
        "getCacheEntryForUpdate - cacheObjectId: {}",
        cacheObjectId);

    return this.jdbcClient
        .sql("""
            select true
              from repository_cache
              where cache_object_id = :cache_object_id
              for no key update skip locked
            """)
        .param(RepositoryCacheRecord.CACHE_OBJECT_ID, cacheObjectId)
        .query(Boolean.class)
        .optional().orElse(Boolean.FALSE).booleanValue();
  }

  public boolean lockCacheEntryForDelete(UUID cacheObjectId) {

    LOGGER.trace(
        "lockCacheEntryForDelete - cacheObjectId: {}",
        cacheObjectId);

    return this.jdbcClient
        .sql("""
            select true
              from repository_cache
              where 1=1
                and cache_object_id = :cache_object_id
              for update skip locked
            """)
        .param(RepositoryCacheRecord.CACHE_OBJECT_ID, cacheObjectId)
        .query(Boolean.class)
        .optional().orElse(Boolean.FALSE).booleanValue();
  }

  public RepositoryCacheRecord insertGetCacheEntry(
      String repositoryType,
      String repositoryName,
      String urlPath) {

    LOGGER.trace(
        "insertGetCacheEntry - repositoryType: {}, repositoryName: {}, urlPath: {}",
        repositoryType,
        repositoryName,
        urlPath);

    return this.jdbcClient
        .sql("""
            insert
              into repository_cache (
                repository_type,
                repository_name,
                url_path,
                cache_object_path,
                cache_object_id
                )
            values (
              :repository_type,
              :repository_name,
              :url_path,
              :cache_object_path,
              :cache_object_id
              )
            on conflict do nothing
            returning *
            """)
        .param(RepositoryCacheRecord.REPOSITORY_TYPE, repositoryType)
        .param(RepositoryCacheRecord.REPOSITORY_NAME, repositoryName)
        .param(RepositoryCacheRecord.URL_PATH, urlPath)
        .param(RepositoryCacheRecord.CACHE_OBJECT_PATH, getObjectFilePath(urlPath))
        .param(RepositoryCacheRecord.CACHE_OBJECT_ID, UUID.randomUUID())
        .query(RepositoryCacheRecord.class)
        .optional()
        .orElseGet(() -> getCacheEntry(repositoryType, repositoryName, urlPath));

  }

  public void updateCacheEntry(
      UUID cacheObjectId,
      Long cacheObjectSize,
      String cacheObjectHash,
      MediaType cacheObjectMimeType,
      boolean cached) {

    LOGGER.trace(
        "updateCacheEntry - cacheObjectId: {}, cacheObjectHash: {}, cacheObjectHash: {}, cacheObjectMimeType: {}, cached: {}",
        cacheObjectId,
        cacheObjectHash,
        cacheObjectHash,
        cacheObjectMimeType,
        cached);

    if (cached) {
      this.jdbcClient
          .sql("""
              update repository_cache
                set cache_object_size = :cache_object_size,
                    cache_object_hash = :cache_object_hash,
                    cache_object_mime_type = :cache_object_mime_type,
                    cached = true,
                    cached_at = current_timestamp
                where cache_object_id = :cache_object_id
              """)
          .param(RepositoryCacheRecord.CACHE_OBJECT_ID, cacheObjectId)
          .param(RepositoryCacheRecord.CACHE_OBJECT_SIZE, cacheObjectSize)
          .param(RepositoryCacheRecord.CACHE_OBJECT_HASH, cacheObjectHash)
          .param(RepositoryCacheRecord.CACHE_OBJECT_MIME_TYPE,
              cacheObjectMimeType == null ? null : cacheObjectMimeType.toString())
          .update();
    } else {
      this.jdbcClient
          .sql("""
              update repository_cache
                set cache_object_size = null,
                    cache_object_hash = null,
                    cache_object_mime_type = null,
                    cached = false,
                    cached_at = null
                where cache_object_id = :cache_object_id
              """)
          .param(RepositoryCacheRecord.CACHE_OBJECT_ID, cacheObjectId)
          .update();
    }

  }

  public void deleteCacheEntry(UUID cacheObjectId) {

    LOGGER.trace(
        "deleteCacheEntry - cacheObjectId: {}",
        cacheObjectId);

    this.jdbcClient
        .sql("""
            delete
              from repository_cache
              where cache_object_id = :cache_object_id
            """)
        .param(RepositoryCacheRecord.CACHE_OBJECT_ID, cacheObjectId)
        .update();
  }

  private String getObjectFilePath(String urlPath) {

    try {
      MessageDigest digest;
      digest = MessageDigest.getInstance("SHA-256");
      byte[] encodedHash = digest.digest((urlPath).getBytes());
      String sha256Hex = Hex.encodeHexString(encodedHash);

      String level1Dir = sha256Hex.substring(0, 1);
      String level2Dir = sha256Hex.substring(1, 2);

      return level1Dir + File.separator + level2Dir;
    } catch (NoSuchAlgorithmException exception) {
      LOGGER.error("SHA-256 Hash Algorithm Not Available", exception);
      throw new MissingHashAlgorithmException(exception);
    }

  }

}
