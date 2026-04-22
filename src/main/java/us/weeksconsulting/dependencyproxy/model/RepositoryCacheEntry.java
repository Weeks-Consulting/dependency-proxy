package us.weeksconsulting.dependencyproxy.model;

import java.sql.Timestamp;
import java.util.UUID;

public class RepositoryCacheEntry {
  private final String repositoryType;
  private final String repositoryName;
  private final String urlPath;
  private final String urlParams;
  private final String mimeType;
  private final String cacheObjectPath;
  private final UUID cacheObjectId;
  private final String cacheObjectHash;
  private final boolean isCached;
  private final Timestamp insertedAt;
  private final Timestamp updatedAt;

  public RepositoryCacheEntry(
      String repositoryType,
      String repositoryName,
      String urlPath,
      String urlParams,
      String mimeType,
      String cacheObjectPath,
      UUID cacheObjectId,
      String cacheObjectHash,
      boolean isCached,
      Timestamp insertedAt,
      Timestamp updatedAt) {
    this.repositoryType = repositoryType;
    this.repositoryName = repositoryName;
    this.urlPath = urlPath;
    this.urlParams = urlParams;
    this.mimeType = mimeType;
    this.cacheObjectPath = cacheObjectPath;
    this.cacheObjectId = cacheObjectId;
    this.cacheObjectHash = cacheObjectHash;
    this.isCached = isCached;
    this.insertedAt = insertedAt;
    this.updatedAt = updatedAt;
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

  public String getUrlParams() {
    return urlParams;
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

  public String getCacheObjectHash() {
    return cacheObjectHash;
  }

  public boolean isCached() {
    return isCached;
  }

  public Timestamp getInsertedAt() {
    return insertedAt;
  }

  public Timestamp getUpdatedAt() {
    return updatedAt;
  }

  @Override
  public String toString() {
    return "RepositoryCacheEntry [repositoryType=" + repositoryType + ", repositoryName=" + repositoryName
        + ", urlPath=" + urlPath + ", urlParams=" + urlParams + ", mimeType=" + mimeType + ", cacheObjectPath="
        + cacheObjectPath + ", cacheObjectId=" + cacheObjectId + ", cacheObjectHash=" + cacheObjectHash + ", isCached="
        + isCached + ", insertedAt=" + insertedAt + ", updatedAt=" + updatedAt + "]";
  }

}
