package us.weeksconsulting.dependency_proxy.config.model;

import java.time.Duration;
import java.util.Set;

import org.springframework.boot.context.properties.bind.DefaultValue;

public class Repository {
  private final String baseUrl;
  private final Duration cacheTTL;
  private final Set<String> excludes;

  public Repository(String baseUrl, Duration cacheTTL, @DefaultValue Set<String> excludes) {
    this.baseUrl = baseUrl;
    this.cacheTTL = cacheTTL;
    this.excludes = excludes;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public Duration getCacheTTL() {
    return cacheTTL;
  }

  public Set<String> getExcludes() {
    return excludes;
  }

  @Override
  public String toString() {
    return "Repository [baseUrl=" + baseUrl + ", cacheTTL=" + cacheTTL + ", excludes=" + excludes + "]";
  }

}
