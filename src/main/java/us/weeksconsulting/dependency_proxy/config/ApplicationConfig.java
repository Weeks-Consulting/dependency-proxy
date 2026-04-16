package us.weeksconsulting.dependency_proxy.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application")
public class ApplicationConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfig.class);

    private final List<Repository> repositories;

    public ApplicationConfig(List<Repository> repositories) {
        LOGGER.error("Constructed RespositoryConfigs: {}", repositories);
        this.repositories = repositories;
    }

    public List<Repository> getRepositories() {
        return repositories;
    }

    @Override
    public String toString() {
        return "ApplicationConfig [repositories=" + repositories + "]";
    }

}
