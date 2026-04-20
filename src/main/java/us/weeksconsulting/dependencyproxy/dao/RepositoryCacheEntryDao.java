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

    public RepositoryCacheEntry getRepositoryCacheEntry(String repositoryType, String repositoryName,
            String urlPath,
            Map<String, String> urlParams) {

        return this.jdbcClient
                .sql("""
                        select *
                            from repository_cache
                            where repository_type = ? and repository_name = ? and url_path = ? and url_params = ?
                        """)
                .params(repositoryType, repositoryName, urlPath, serializeUrlParams(urlParams))
                .query(RepositoryCacheEntry.class)
                .optional().orElse(null);
    }

    public RepositoryCacheEntry putRepositoryCacheEntry(
            String repositoryType,
            String repositoryName,
            String urlPath,
            Map<String, String> urlParams,
            String mime_type) {

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
                                inserted_at
                                )
                        values (?, ?, ?, ?, ?, ?, ?, ?)
                        returning
                            repository_type,
                            repository_name,
                            url_path,
                            url_params,
                            mime_type,
                            cache_object_path,
                            cache_object_id,
                            inserted_at
                        """)
                .params(
                        repositoryType,
                        repositoryName,
                        urlPath,
                        serializeUrlParams(urlParams),
                        mime_type,
                        getObjectFilePath(urlPath, serializeUrlParams(urlParams)),
                        UUID.randomUUID(),
                        Timestamp.from(Instant.now()))
                .query(RepositoryCacheEntry.class)
                .single();

    }

    private String serializeUrlParams(Map<String, String> urlParams) {
        return String.valueOf(urlParams);
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
