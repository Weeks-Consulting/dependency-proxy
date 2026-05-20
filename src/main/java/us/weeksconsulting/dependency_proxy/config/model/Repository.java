package us.weeksconsulting.dependency_proxy.config.model;

import java.time.Duration;
import java.util.Set;

import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.AntPathMatcher;

public record Repository(
    String baseUrl,
    Duration cacheTTL,
    @DefaultValue Set<String> excludes) {

  private static final AntPathMatcher antPatternMatcher = new AntPathMatcher();

  public boolean isExcluded(String urlPath) {
    return excludes.stream().anyMatch(excludePattern -> antPatternMatcher.match(excludePattern, urlPath));
  }
}
