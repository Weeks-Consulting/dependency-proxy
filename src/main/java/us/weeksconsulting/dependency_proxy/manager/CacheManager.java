package us.weeksconsulting.dependency_proxy.manager;

import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import us.weeksconsulting.dependency_proxy.config.ApplicationConfig;
import us.weeksconsulting.dependency_proxy.model.RepositoryCacheEntry;

@Component
public class CacheManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheManager.class);
    private final ApplicationConfig applicationConfig;
    private final JdbcClient jdbcClient;

    public CacheManager(ApplicationConfig applicationConfig, DataSource dataSource) {
        LOGGER.trace("CacheManager Constructor Called applicationConfig: {} dataSource: {}", applicationConfig,
                dataSource);
        this.applicationConfig = applicationConfig;
        this.jdbcClient = JdbcClient.create(dataSource);
    }

    public InputStream getOrCache(String repositoryType, String repositoryName, String urlPath,
            Map<String, String> urlParams) {
        LOGGER.trace("repositoryType: {} repositoryName: {} urlPath: {} urlParams: {}", repositoryType, repositoryName,
                urlPath, urlParams);

        String seralizedUrlParams = String.valueOf(urlParams);

        Optional<RepositoryCacheEntry> repositoryCacheEntry = this.jdbcClient
                .sql("select * from repository_cache where repository_type = ? and repository_name = ? and url_path = ? and url_params = ?")
                .params(repositoryType, repositoryName, urlPath, seralizedUrlParams)
                .query(RepositoryCacheEntry.class)
                .optional();

        LOGGER.trace("repositoryCacheEntry: {}", repositoryCacheEntry);

        if (repositoryCacheEntry.isEmpty()) {
            LOGGER.trace("Cache Entry Not Found - repositoryType: {}, repositoryName: {}, urlPath: {}, urlParams: {}",
                    repositoryType, repositoryName, urlPath, urlParams);
            // repo_type, repo_name, url_path, url_params, mime_type, cache_object_path,
            // cache_object_id
            this.jdbcClient
                    .sql("""
                            insert into repository_cache (
                                repository_type,
                                repository_name,
                                url_path,
                                url_params,
                                mime_type,
                                cache_object_path,
                                cache_object_id,
                                inserted_at )
                            values (?, ?, ?, ?, ?, ?, ?, ?)
                            """)
                    .params(repositoryType, repositoryName, urlPath, seralizedUrlParams, null,
                            getObjectFilePath(urlPath, seralizedUrlParams),
                            UUID.randomUUID(), Instant.now())
                    .update();
        }

        return null;
    }

    private InputStream getOrCache(InputStream inputStream, String objectPath, UUID objectId) {
        String cacheType = applicationConfig.getStorage().getType();

        switch (cacheType) {
            case "local":
                return getOrCacheLocal(objectPath, objectId);
            case "s3":
                return getOrCacheS3(objectPath, objectId);
            default:
                return null;
        }
    }

    private InputStream getOrCacheLocal(String objectPath, UUID objectId) {
        return null;
    }

    private InputStream getOrCacheS3(String objectPath, UUID objectId) {
        return null;
    }

    private String getObjectFilePath(String urlPath, String serializedUrlParams) {

        try {
            // 1. Calculate the SHA-256 hash of the file
            MessageDigest digest;
            digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest((urlPath + serializedUrlParams).getBytes());
            String sha256Hex = Hex.encodeHexString(encodedhash);

            // 2. Determine the folder structure from the hash
            if (sha256Hex.length() < 4) {
                throw new RuntimeException("Hash is too short to create a directory structure.");
            }
            String level1Dir = sha256Hex.substring(0, 2);
            String level2Dir = sha256Hex.substring(2, 4);

            return "/" + level1Dir + "/" + level2Dir;
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("NoSuchAlgorithmException", e);
            throw new RuntimeException(e);
        }

    }
}
