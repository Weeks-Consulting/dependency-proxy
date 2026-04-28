package us.weeksconsulting.dependency_proxy.model;

import java.sql.Timestamp;
import java.util.UUID;

public class RepositoryCacheEntry {
  private final String repositoryType;
  private final String repositoryName;
  private final String url;
  private final String mimeType;
  private final String cacheObjectPath;
  private final UUID cacheObjectId;
  private final Long cacheObjectSize;
  private final String cacheObjectHash;
  private final boolean isCached;
  private final Timestamp cachedAt;

  public RepositoryCacheEntry(
      String repositoryType,
      String repositoryName,
      String url,
      String mimeType,
      String cacheObjectPath,
      UUID cacheObjectId,
      Long cacheObjectSize,
      String cacheObjectHash,
      boolean isCached,
      Timestamp cachedAt) {
    this.repositoryType = repositoryType;
    this.repositoryName = repositoryName;
    this.url = url;
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

  public String getUrl() {
    return url;
  }

  public String getMimeType() {
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
    return "RepositoryCacheEntry [repositoryType=" + repositoryType + ", repositoryName=" + repositoryName + ", url="
        + url + ", mimeType=" + mimeType + ", cacheObjectPath=" + cacheObjectPath + ", cacheObjectId=" + cacheObjectId
        + ", cacheObjectSize=" + cacheObjectSize + ", cacheObjectHash=" + cacheObjectHash + ", isCached=" + isCached
        + ", cachedAt=" + cachedAt + "]";
  }

}
