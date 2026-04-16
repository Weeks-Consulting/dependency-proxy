package us.weeksconsulting.dependency_proxy.config;

import java.util.List;

public class RepositoryConfig {
    private final String repositoryType;
    private final List<Repository> repositories;

    public RepositoryConfig(String repositoryType, List<Repository> repositories) {
        this.repositoryType = repositoryType;
        this.repositories = repositories;
    }

    public String getRepositoryType() {
        return repositoryType;
    }

    public List<Repository> getRepositories() {
        return repositories;
    }

    @Override
    public String toString() {
        return "RepositoryConfig [repositoryType=" + repositoryType + ", repositories=" + repositories + "]";
    }

}
