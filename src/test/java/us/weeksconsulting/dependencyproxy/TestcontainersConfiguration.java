package us.weeksconsulting.dependencyproxy;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.adobe.testing.s3mock.testcontainers.S3MockContainer;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

  @Bean
  PostgreSQLContainer postgresContainer() {
    PostgreSQLContainer postgresContainer = new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));
    postgresContainer.withCreateContainerCmdModifier(cmd -> cmd.withHostConfig(
        new HostConfig()
            .withPortBindings(new PortBinding(Ports.Binding.bindPort(55432), new ExposedPort(5432)))));

    return postgresContainer;
  }

  @Bean
  DynamicPropertyRegistrar postgresPropertiesRegistrar(PostgreSQLContainer postgresContainer) {
    return registry -> {
      registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
      registry.add("spring.datasource.username", postgresContainer::getUsername);
      registry.add("spring.datasource.password", postgresContainer::getPassword);
      registry.add("spring.datasource.configuration.maximum-pool-size", () -> 5);
    };
  }

  @Bean
  @Profile("s3")
  S3MockContainer s3MockContainer() {
    S3MockContainer s3Container = new S3MockContainer("5.0.0");
    // For some reason this doesn't work
    // s3Container.withInitialBuckets("test-bucket");
    s3Container.withEnv("COM_ADOBE_TESTING_S3MOCK_STORE_INITIAL_BUCKETS", "test-bucket");
    return s3Container;
  }

  @Bean
  @Profile("s3")
  DynamicPropertyRegistrar s3PropertiesRegistrar(S3MockContainer s3MockContainer) {
    return registry -> {
      registry.add("spring.cloud.aws.s3.endpoint", s3MockContainer::getHttpEndpoint);
      registry.add("spring.cloud.aws.s3.path-style-access-enabled", () -> "true");
      registry.add("spring.cloud.aws.credentials.access-key", () -> "foo");
      registry.add("spring.cloud.aws.credentials.secret-key", () -> "bar");
    };
  }

}
