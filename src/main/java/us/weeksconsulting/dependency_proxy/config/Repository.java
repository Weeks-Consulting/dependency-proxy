package us.weeksconsulting.dependency_proxy.config;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Repository {
    private static final Logger LOGGER = LoggerFactory.getLogger(Repository.class);
    private final String name;
    private final String type;
    private final String baseUrl;
    private final Duration cacheTTL;

    public Repository(String name, String type, String baseUrl, Duration cacheTTL) {
        this.name = name;
        this.type = type;
        this.baseUrl = baseUrl;
        this.cacheTTL = cacheTTL;
        LOGGER.error("Constructed Repository: {} {} {}", name, type, baseUrl, cacheTTL);
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public Duration getCacheTTL() {
        return cacheTTL;
    }

}
