package us.weeksconsulting.dependencyproxy.config.model;

import java.time.Duration;

public class Repository {
  private final String baseUrl;
  private final Duration cacheTTL;

  public Repository(String baseUrl, Duration cacheTTL) {
    this.baseUrl = baseUrl;
    this.cacheTTL = cacheTTL;
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
