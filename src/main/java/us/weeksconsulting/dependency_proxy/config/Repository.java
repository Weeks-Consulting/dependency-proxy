package us.weeksconsulting.dependency_proxy.config;

import java.time.Duration;

public class Repository {

    private final String name;
    private final String baseUrl;
    private final Duration cacheTTL;

    public Repository(String name, String baseUrl, Duration cacheTTL) {
        this.name = name;
        this.baseUrl = baseUrl;
        this.cacheTTL = cacheTTL;
    }

    public String getName() {
        return name;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public Duration getCacheTTL() {
        return cacheTTL;
    }

    @Override
    public String toString() {
        return "Repository [name=" + name + ", baseUrl=" + baseUrl + ", cacheTTL=" + cacheTTL + "]";
    }

}
