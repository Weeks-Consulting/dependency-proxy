package us.weeksconsulting.dependencyproxy.config.model;

import java.util.UUID;

public class RepositoryCacheEntry {
    private final String repositoryType;
    private final String repositoryName;
    private final String urlPath;
    private final String urlParams;
    private final String mimeType;
    private final String cacheObjectPath;
    private final UUID cacheObjectId;

    public RepositoryCacheEntry(
            String repositoryType,
            String repositoryName,
            String urlPath,
            String urlParams,
            String mimeType,
            String cacheObjectPath,
            UUID cacheObjectId) {
        this.repositoryType = repositoryType;
        this.repositoryName = repositoryName;
        this.urlPath = urlPath;
        this.urlParams = urlParams;
        this.mimeType = mimeType;
        this.cacheObjectPath = cacheObjectPath;
        this.cacheObjectId = cacheObjectId;
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

    public String getObjectPath() {
        return cacheObjectPath;
    }

    public UUID getObjectId() {
        return cacheObjectId;
    }

    @Override
    public String toString() {
        return "RepositoryCacheEntry [repositoryType=" + repositoryType + ", repositoryName=" + repositoryName
                + ", urlPath=" + urlPath + ", urlParams=" + urlParams + ", mimeType=" + mimeType + ", cacheObjectPath="
                + cacheObjectPath + ", cacheObjectId=" + cacheObjectId + "]";
    }

}
