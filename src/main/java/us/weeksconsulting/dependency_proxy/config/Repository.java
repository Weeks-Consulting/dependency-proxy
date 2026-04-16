package us.weeksconsulting.dependency_proxy.config;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Repository {
    private static final Logger LOGGER = LoggerFactory.getLogger(Repository.class);
    private final String baseUrl;
    private final Duration cacheTTL;

    public Repository(String baseUrl, Duration cacheTTL) {        
        this.baseUrl = baseUrl;
        this.cacheTTL = cacheTTL;
        LOGGER.trace("Constructed Repository: {} {} {}", baseUrl, cacheTTL);
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public Duration getCacheTTL() {
        return cacheTTL;
    }

    @Override
    public String toString() {
        return "Repository [baseUrl=" + baseUrl + ", cacheTTL=" + cacheTTL + "]";
    }

}
