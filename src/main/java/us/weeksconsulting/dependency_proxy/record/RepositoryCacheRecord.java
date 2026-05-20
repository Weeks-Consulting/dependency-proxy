package us.weeksconsulting.dependency_proxy.record;

import java.sql.Timestamp;
import java.util.UUID;

import org.springframework.http.MediaType;

@SuppressWarnings("java:S1845")
public record RepositoryCacheRecord(
    UUID cacheObjectId,
    String cacheObjectPath,
    Long cacheObjectSize,
    String cacheObjectHash,
    String cacheObjectMimeType,
    String repositoryType,
    String repositoryName,
    String urlPath,
    boolean cached,
    Timestamp cachedAt) {

  public static final String CACHE_OBJECT_ID = "cache_object_id";
  public static final String CACHE_OBJECT_PATH = "cache_object_path";
  public static final String CACHE_OBJECT_SIZE = "cache_object_size";
  public static final String CACHE_OBJECT_HASH = "cache_object_hash";
  public static final String CACHE_OBJECT_MIME_TYPE = "cache_object_mime_type";
  public static final String REPOSITORY_TYPE = "repository_type";
  public static final String REPOSITORY_NAME = "repository_name";
  public static final String URL_PATH = "url_path";
  public static final String CACHED = "cached";
  public static final String CACHED_AT = "cached_at";

  public MediaType cacheObjectMimeTypeAsMediaType() {
    return cacheObjectMimeType == null ? null : MediaType.valueOf(cacheObjectMimeType);
  }

}
