package us.weeksconsulting.dependency_proxy.config.model;

import java.util.Map;

import org.springframework.boot.context.properties.bind.Name;

public class Repositories {
  private final Map<String, Repository> rawRepositories;

  public Repositories(@Name("raw") Map<String, Repository> rawRepositories) {
    this.rawRepositories = rawRepositories;
  }

  public Map<String, Repository> getRawRepositories() {
    return rawRepositories;
  }

  @Override
  public String toString() {
    return "Repositories [rawRepositories=" + rawRepositories + "]";
  }

}
