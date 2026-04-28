package us.weeksconsulting.dependency_proxy.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import us.weeksconsulting.dependency_proxy.config.model.Repositories;
import us.weeksconsulting.dependency_proxy.config.model.Storage;

@ConfigurationProperties(prefix = "application")
public class ApplicationConfig {
  private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfig.class);

  @NestedConfigurationProperty
  private final Storage storage;

  @NestedConfigurationProperty
  private final Repositories repositories;

  public ApplicationConfig(Storage storage, Repositories repositories) {
    LOGGER.debug("ApplicationConfig: storage: {}, repositories: {}", storage, repositories);
    this.storage = storage;
    this.repositories = repositories;
  }

  public Storage getStorage() {
    return storage;
  }

  public Repositories getRepositories() {
    return repositories;
  }

  @Override
  public String toString() {
    return "ApplicationConfig [storage=" + storage + ", repositories=" + repositories + "]";
  }

}
