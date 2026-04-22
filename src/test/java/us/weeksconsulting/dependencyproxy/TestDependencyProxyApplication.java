package us.weeksconsulting.dependencyproxy;

import org.springframework.boot.SpringApplication;

public class TestDependencyProxyApplication {

  public static void main(String[] args) {
    SpringApplication
        .from(DependencyProxyApplication::main)
        .with(TestcontainersConfiguration.class)
        .run(args);
  }

}
