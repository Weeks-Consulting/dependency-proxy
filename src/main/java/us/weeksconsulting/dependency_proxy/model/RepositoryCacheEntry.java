package us.weeksconsulting.dependency_proxy.model;

import java.sql.Timestamp;
import java.util.UUID;

import org.springframework.http.MediaType;

// Ignore rule about having too many parameters since this represents a database row.
@SuppressWarnings("java:S107")
public class RepositoryCacheEntry {
  public static final String REPOSITORY_TYPE = "repository_type";
  public static final String REPOSITORY_NAME = "repository_name";
  public static final String URL_PATH = "url_path";
  public static final String MIME_TYPE = "mime_type";
  public static final String CACHE_OBJECT_PATH = "cache_object_path";
  public static final String CACHE_OBJECT_ID = "cache_object_id";
  public static final String CACHE_OBJECT_SIZE = "cache_object_size";
  public static final String CACHE_OBJECT_HASH = "cache_object_hash";
  public static final String IS_CACHED = "is_cached";
  public static final String CACHED_AT = "cached_at";

  private final String repositoryType;
  private final String repositoryName;
  private final String urlPath;
  private final MediaType mimeType;
  private final String cacheObjectPath;
  private final UUID cacheObjectId;
  private final Long cacheObjectSize;
  private final String cacheObjectHash;
  private final boolean isCached;
  private final Timestamp cachedAt;

  public RepositoryCacheEntry(
      String repositoryType,
      String repositoryName,
      String urlPath,
      MediaType mimeType,
      String cacheObjectPath,
      UUID cacheObjectId,
      Long cacheObjectSize,
      String cacheObjectHash,
      boolean isCached,
      Timestamp cachedAt) {
    this.repositoryType = repositoryType;
    this.repositoryName = repositoryName;
    this.urlPath = urlPath;
    this.mimeType = mimeType;
    this.cacheObjectPath = cacheObjectPath;
    this.cacheObjectId = cacheObjectId;
    this.cacheObjectSize = cacheObjectSize;
    this.cacheObjectHash = cacheObjectHash;
    this.isCached = isCached;
    this.cachedAt = cachedAt;
  }

  public String getRepositoryType() {
    return repositoryType;
  }

  public String getRepositoryName() {
    return repositoryName;
  }

  public String getUrlPath() {
    return urlPath;
  }

  public MediaType getMimeType() {
    return mimeType;
  }

  public String getCacheObjectPath() {
    return cacheObjectPath;
  }

  public UUID getCacheObjectId() {
    return cacheObjectId;
  }

  public Long getCacheObjectSize() {
    return cacheObjectSize;
  }

  public String getCacheObjectHash() {
    return cacheObjectHash;
  }

  public boolean isCached() {
    return isCached;
  }

  public Timestamp getCachedAt() {
    return cachedAt;
  }

  @Override
  public String toString() {
    return "RepositoryCacheEntry [repositoryType=" + repositoryType + ", repositoryName=" + repositoryName
        + ", urlPath="
        + urlPath + ", mimeType=" + mimeType + ", cacheObjectPath=" + cacheObjectPath + ", cacheObjectId="
        + cacheObjectId
        + ", cacheObjectSize=" + cacheObjectSize + ", cacheObjectHash=" + cacheObjectHash + ", isCached=" + isCached
        + ", cachedAt=" + cachedAt + "]";
  }

}
