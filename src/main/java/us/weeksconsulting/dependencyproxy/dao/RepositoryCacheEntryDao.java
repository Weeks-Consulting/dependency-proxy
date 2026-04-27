package us.weeksconsulting.dependencyproxy.dao;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.annotation.RegisterReflection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import us.weeksconsulting.dependencyproxy.model.RepositoryCacheEntry;
import us.weeksconsulting.util.CacheUtils;

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
                and inserted_at < current_timestamp - (:cache_ttl * interval '1 second')
              for key share skip locked
            """)
        .param("repository_type", repositoryType)
        .param("repository_name", repositoryName)
        .param("cache_ttl", cacheTTL.getSeconds())
        .query(RepositoryCacheEntry.class)
        .stream();
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

    return this.jdbcClient
        .sql("""
            select *
              from repository_cache
              where 1=1
                and repository_type = :repository_type
                and repository_name = :repository_name
                and url_path = :url_path
                and url_params = :url_params
              for key share
            """)
        .param("repository_type", repositoryType)
        .param("repository_name", repositoryName)
        .param("url_path", urlPath)
        .param("url_params", CacheUtils.serializeUrlParams(urlParams))
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
              for key share
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
              for no key update skip locked
            """)
        .param("cache_object_id", cacheObjectId)
        .query(RepositoryCacheEntry.class)
        .optional().orElse(null);
  }

  public void lockCacheEntryForDelete(UUID cacheObjectId) {
    LOGGER.trace("getCacheEntryForUpdate - cacheObjectId: {}", cacheObjectId);

    this.jdbcClient
        .sql("""
            select *
              from repository_cache
              where cache_object_id = :cache_object_id
              for update
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
                cache_object_id                )
            values (
              :repository_type,
              :repository_name,
              :url_path,
              :url_params,
              :mime_type,
              :cache_object_path,
              :cache_object_id
              )
            on conflict do nothing
            returning *
            """)
        .param("repository_type", repositoryType)
        .param("repository_name", repositoryName)
        .param("url_path", urlPath)
        .param("url_params", CacheUtils.serializeUrlParams(urlParams))
        .param("mime_type", mime_type)
        .param("cache_object_path", CacheUtils.getObjectFilePath(urlPath, urlParams))
        .param("cache_object_id", UUID.randomUUID())
        .query(RepositoryCacheEntry.class)
        .optional()
        .orElseGet(() -> getCacheEntry(repositoryType, repositoryName, urlPath, urlParams));

  }

  public void updateCacheEntry(
      UUID cacheObjectId,
      Long cacheObjectSize,
      String cacheObjectHash,
      Boolean isCached) {

    LOGGER.trace(
        "updateCacheEntry - cacheObjectId: {}, cacheObjectHash: {}, cacheObjectHash: {}, isCached: {}",
        cacheObjectId,
        cacheObjectHash,
        cacheObjectHash,
        isCached);

    this.jdbcClient
        .sql("""
            update repository_cache
              set cache_object_size = :cache_object_size,
                  cache_object_hash = :cache_object_hash,
                  is_cached = :is_cached,
                  updated_at = :updated_at
              where cache_object_id = :cache_object_id
            """)
        .param("cache_object_id", cacheObjectId)
        .param("cache_object_size", cacheObjectSize)
        .param("cache_object_hash", cacheObjectHash)
        .param("is_cached", isCached)
        .param("updated_at", Timestamp.from(Instant.now()))
        .update();
  }

  public void deleteCacheEntry(UUID cacheObjectId) {

    LOGGER.trace("getCacheEntryForUpdate - cacheObjectId: {}", cacheObjectId);

    this.jdbcClient
        .sql("""
            delete
              from repository_cache
              where cache_object_id = :cache_object_id
            """)
        .param("cache_object_id", cacheObjectId)
        .update();
  }

}
