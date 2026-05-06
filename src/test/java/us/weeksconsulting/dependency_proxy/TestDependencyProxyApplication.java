package us.weeksconsulting.dependency_proxy;

import org.springframework.boot.SpringApplication;

public class TestDependencyProxyApplication {

  public static void main(String[] args) {
    SpringApplication
        .from(DependencyProxyApplication::main)
        .with(TestcontainersConfiguration.class)
        .run(args);
  }

}
