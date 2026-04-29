package us.weeksconsulting.dependency_proxy.dao;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.annotation.RegisterReflection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import us.weeksconsulting.dependency_proxy.model.RepositoryCacheEntry;

@Component
@RegisterReflection(classes = RepositoryCacheEntry.class, memberCategories = {
    MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS })
public class RepositoryCacheEntryDao {
  private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryCacheEntryDao.class);

  private final JdbcClient jdbcClient;

  public RepositoryCacheEntryDao(DataSource dataSource) {
    this.jdbcClient = JdbcClient.create(dataSource);
  }

  public Stream<RepositoryCacheEntry> getExpiredCacheEntries(
      String repositoryType,
      String repositoryName,
      Duration cacheTTL) {

    return this.jdbcClient
        .sql("""
            select *
              from repository_cache
              where is_cached
                and repository_type = :repository_type
                and repository_name = :repository_name
                and cached_at < current_timestamp - (:cache_ttl * interval '1 second')
              for key share skip locked
            """)
        .param("repository_type", repositoryType)
        .param("repository_name", repositoryName)
        .param("cache_ttl", cacheTTL.getSeconds())
        .query(RepositoryCacheEntry.class)
        .stream();
  }

  private RepositoryCacheEntry getCacheEntry(
      String repositoryType,
      String repositoryName,
      String urlPath,
      MultiValueMap<String, String> urlParams) {

    LOGGER.trace(
        "getCacheEntry - repositoryType: {}, repositoryName: {}, urlPath: {}, urlParams: {}",
        repositoryType,
        repositoryName,
        urlPath,
        urlParams);

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
        .param("repository_type", repositoryType)
        .param("repository_name", repositoryName)
        .param("url_path", serializeUrl(urlPath, urlParams))
        .query(RepositoryCacheEntry.class)
        .single();
  }

  public Boolean getCacheEntryForUpdate(UUID cacheObjectId) {
    LOGGER.trace("getCacheEntryForUpdate - cacheObjectId: {}", cacheObjectId);

    return this.jdbcClient
        .sql("""
            select true
              from repository_cache
              where cache_object_id = :cache_object_id
              for no key update skip locked
            """)
        .param("cache_object_id", cacheObjectId)
        .query(Boolean.class)
        .optional().orElse(Boolean.FALSE);
  }

  public Boolean lockCacheEntryForDelete(UUID cacheObjectId) {
    LOGGER.trace("lockCacheEntryForDelete - cacheObjectId: {}", cacheObjectId);

    return this.jdbcClient
        .sql("""
            select true
              from repository_cache
              where cache_object_id = :cache_object_id
              for update
            """)
        .param("cache_object_id", cacheObjectId)
        .query(Boolean.class)
        .optional().orElse(Boolean.FALSE);
  }

  public RepositoryCacheEntry insertGetCacheEntry(
      String repositoryType,
      String repositoryName,
      String urlPath,
      MultiValueMap<String, String> urlParams) {

    LOGGER.trace(
        "insertGetCacheEntry - repositoryType: {}, repositoryName: {}, urlPath: {}, urlParams: {}",
        repositoryType,
        repositoryName,
        urlPath,
        urlParams);

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
        .param("repository_type", repositoryType)
        .param("repository_name", repositoryName)
        .param("url_path", serializeUrl(urlPath, urlParams))
        .param("cache_object_path", getObjectFilePath(serializeUrl(urlPath, urlParams)))
        .param("cache_object_id", UUID.randomUUID())
        .query(RepositoryCacheEntry.class)
        .optional()
        .orElseGet(() -> getCacheEntry(repositoryType, repositoryName, urlPath, urlParams));

  }

  public void updateCacheEntry(
      UUID cacheObjectId,
      Long cacheObjectSize,
      String cacheObjectHash,
      MediaType mimeType,
      Boolean isCached) {

    LOGGER.trace(
        "updateCacheEntry - cacheObjectId: {}, cacheObjectHash: {}, cacheObjectHash: {}, mimeType: {}, isCached: {}",
        cacheObjectId,
        cacheObjectHash,
        cacheObjectHash,
        mimeType,
        isCached);

    if (Boolean.TRUE.equals(isCached)) {
      this.jdbcClient
          .sql("""
              update repository_cache
                set cache_object_size = :cache_object_size,
                    cache_object_hash = :cache_object_hash,
                    is_cached = :is_cached,
                    cached_at = :cached_at
                where cache_object_id = :cache_object_id
              """)
          .param("cache_object_id", cacheObjectId)
          .param("cache_object_size", cacheObjectSize)
          .param("cache_object_hash", cacheObjectHash)
          .param("mime_type", mimeType)
          .param("is_cached", isCached)
          .param("cached_at", Timestamp.from(Instant.now()))
          .update();
    } else {
      this.jdbcClient
          .sql("""
              update repository_cache
                set cache_object_size = null,
                    cache_object_hash = null,
                    is_cached = :is_cached,
                    cached_at = null
                where cache_object_id = :cache_object_id
              """)
          .param("cache_object_id", cacheObjectId)
          .update();
    }

  }

  public void deleteCacheEntry(UUID cacheObjectId) {

    LOGGER.trace("deleteCacheEntry - cacheObjectId: {}", cacheObjectId);

    this.jdbcClient
        .sql("""
            delete
              from repository_cache
              where cache_object_id = :cache_object_id
            """)
        .param("cache_object_id", cacheObjectId)
        .update();
  }

  private String serializeUrl(String urlPath, MultiValueMap<String, String> urlParams) {
    return UriComponentsBuilder
        .fromPath(urlPath)
        .queryParams(urlParams)
        .toUriString();
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
      LOGGER.error("Error generation object file path.", exception);
      throw new RuntimeException(exception);
    }

  }

}
