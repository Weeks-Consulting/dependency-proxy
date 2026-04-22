package us.weeksconsulting.dependencyproxy.config;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

import us.weeksconsulting.dependencyproxy.config.model.Repository;
import us.weeksconsulting.dependencyproxy.config.model.Storage;

@ConfigurationProperties(prefix = "application")
public class ApplicationConfig {
  private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfig.class);

  private final Storage storage;
  private final Map<String, Map<String, Repository>> repositories;

  public ApplicationConfig(Storage storage, Map<String, Map<String, Repository>> repositories) {
    LOGGER.trace("Constructed RepositoryConfigs: storage: {}, repositories: {}", storage, repositories);
    this.storage = storage;
    this.repositories = repositories;
  }

  public Storage getStorage() {
    return storage;
  }

  public Map<String, Map<String, Repository>> getRepositories() {
    return repositories;
  }

  @Override
  public String toString() {
    return "ApplicationConfig [storage=" + storage + ", repositories=" + repositories + "]";
  }

}
