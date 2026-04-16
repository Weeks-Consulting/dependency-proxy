package us.weeksconsulting.dependency_proxy.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("application.repositories")
public class RespositoryConfigs {
    final List<RepositoryConfig> repositoryConfigs;

    public RespositoryConfigs(List<RepositoryConfig> repositoryConfigs) {
        this.repositoryConfigs = repositoryConfigs;
    }

    public List<RepositoryConfig> getRepositories() {
        return repositoryConfigs;
    }

    @Override
    public String toString() {
        return "RespositoryConfigs [repositoryConfigs=" + repositoryConfigs + "]";
    }

}
