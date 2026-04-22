package us.weeksconsulting.dependencyproxy.dao;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import us.weeksconsulting.dependencyproxy.model.RepositoryCacheEntry;

@Component
public class RepositoryCacheEntryDao {
  private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryCacheEntryDao.class);

  private final JdbcClient jdbcClient;

  public RepositoryCacheEntryDao(DataSource dataSource) {
    this.jdbcClient = JdbcClient.create(dataSource);
  }

  public RepositoryCacheEntry getCacheEntry(
      String repositoryType,
      String repositoryName,
      String urlPath,
      Map<String, String> urlParams) {

    LOGGER.trace(
        "getCacheEntry - repositoryType: {}, repositoryName: {}, urlPath: {}, urlParams: {}",
        repositoryType,
        repositoryName,
        urlPath,
        urlParams);

    String serializedUrlParams = serializeUrlParams(urlParams);

    return this.jdbcClient
        .sql("""
            select *
              from repository_cache
              where 1=1
                and repository_type = :repository_type
                and repository_name = :repository_name
                and url_path = :url_path
                and url_params = :url_params
            """)
        .param("repository_type", repositoryType)
        .param("repository_name", repositoryName)
        .param("url_path", urlPath)
        .param("url_params", serializedUrlParams)
        .query(RepositoryCacheEntry.class)
        .optional().orElse(null);
  }

  public RepositoryCacheEntry getCacheEntry(UUID cacheObjectId) {

    LOGGER.trace("getCacheEntry - cacheObjectId: {}", cacheObjectId);

    return this.jdbcClient
        .sql("""
            select *
              from repository_cache
              where cache_object_id = :cache_object_id
            """)
        .param("cache_object_id", cacheObjectId)
        .query(RepositoryCacheEntry.class)
        .optional().orElse(null);
  }

  public RepositoryCacheEntry getCacheEntryForUpdate(UUID cacheObjectId) {

    LOGGER.trace("getCacheEntryForUpdate - cacheObjectId: {}", cacheObjectId);

    return this.jdbcClient
        .sql("""
            select *
              from repository_cache
              where cache_object_id = :cache_object_id
              for update skip locked
            """)
        .param("cache_object_id", cacheObjectId)
        .query(RepositoryCacheEntry.class)
        .optional().orElse(null);
  }

  public RepositoryCacheEntry insertGetCacheEntry(
      String repositoryType,
      String repositoryName,
      String urlPath,
      Map<String, String> urlParams,
      String mime_type) {

    LOGGER.trace(
        "insertGetCacheEntry - repositoryType: {}, repositoryName: {}, urlPath: {}, urlParams: {}, mime_type: {}",
        repositoryType,
        repositoryName,
        urlPath,
        urlParams,
        mime_type);

    String serializedUrlParams = serializeUrlParams(urlParams);
    String objectFilepath = getObjectFilePath(urlPath, serializedUrlParams);
    UUID cacheObjectId = UUID.randomUUID();
    Timestamp now = Timestamp.from(Instant.now());

    return this.jdbcClient
        .sql("""
            insert
              into repository_cache (
                repository_type,
                repository_name,
                url_path,
                url_params,
                mime_type,
                cache_object_path,
                cache_object_id,
                cache_object_hash,
                is_cached,
                inserted_at,
                updated_at
                )
            values (
              :repository_type,
              :repository_name,
              :url_path,
              :url_params,
              :mime_type,
              :cache_object_path,
              :cache_object_id,
              :cache_object_hash,
              :is_cached,
              :inserted_at,
              :updated_at
              )
            on conflict do nothing
            returning *
            """)
        .param("repository_type", repositoryType)
        .param("repository_name", repositoryName)
        .param("url_path", urlPath)
        .param("url_params", serializedUrlParams)
        .param("mime_type", mime_type)
        .param("cache_object_path", objectFilepath)
        .param("cache_object_id", cacheObjectId)
        .param("cache_object_hash", null)
        .param("is_cached", false)
        .param("inserted_at", now)
        .param("updated_at", now)
        .query(RepositoryCacheEntry.class)
        .optional()
        .orElseGet(() -> getCacheEntry(repositoryType, repositoryName, urlPath, urlParams));

  }

  public void updateCacheEntry(
      UUID cacheObjectId,
      String cacheObjectHash,
      boolean isCached) {

    LOGGER.trace(
        "updateCacheEntry - cacheObjectId: {}, cacheObjectHash: {}, isCached: {}",
        cacheObjectId,
        cacheObjectHash,
        isCached);

    Timestamp now = Timestamp.from(Instant.now());

    this.jdbcClient
        .sql("""
            update repository_cache
              set cache_object_hash = :cache_object_hash,
                  is_cached = :is_cached,
                  updated_at = updated_at
              where cache_object_id = :cache_object_id
            """)
        .param("cache_object_id", cacheObjectId)
        .param("cache_object_hash", cacheObjectHash)
        .param("is_cached", isCached)
        .param("updated_at", now)
        .update();
  }

  private String serializeUrlParams(Map<String, String> urlParams) {
    return String.valueOf(urlParams);
  }

  private String getObjectFilePath(String urlPath, String serializedUrlParams) {

    try {
      MessageDigest digest;
      digest = MessageDigest.getInstance("SHA-256");
      byte[] encodedHash = digest.digest((urlPath + serializedUrlParams).getBytes());
      String sha256Hex = Hex.encodeHexString(encodedHash);

      String level1Dir = sha256Hex.substring(0, 1);
      String level2Dir = sha256Hex.substring(1, 2);

      return "/" + level1Dir + "/" + level2Dir;
    } catch (NoSuchAlgorithmException exception) {
      LOGGER.error("Error generation object file path.", exception);
      throw new RuntimeException(exception);
    }

  }
}
