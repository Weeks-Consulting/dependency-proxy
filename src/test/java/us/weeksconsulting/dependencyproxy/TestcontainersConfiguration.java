package us.weeksconsulting.dependencyproxy;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
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
  @ServiceConnection
  PostgreSQLContainer postgresContainer() {
    PostgreSQLContainer postgresContainer = new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));
    postgresContainer.withCreateContainerCmdModifier(cmd -> cmd.withHostConfig(
        new HostConfig()
            .withPortBindings(new PortBinding(Ports.Binding.bindPort(55432), new ExposedPort(5432)))));

    return postgresContainer;
  }

  @Bean
  S3MockContainer s3MockContainer() {
    S3MockContainer s3Container = new S3MockContainer("5.0.0");
    // s3Container.withInitialBuckets("test-bucket");
    s3Container.withEnv("COM_ADOBE_TESTING_S3MOCK_STORE_INITIAL_BUCKETS","test-bucket");
    s3Container.withEnv("SPRING_PROFILES_ACTIVE","debug");
    return s3Container;
  }

  @Bean
  DynamicPropertyRegistrar s3PropertiesRegistrar(S3MockContainer s3MockContainer) {
    return registry -> {
      // registry.add("spring.cloud.aws.s3.region", () -> "us-east-1");
      registry.add("spring.cloud.aws.s3.endpoint", s3MockContainer::getHttpEndpoint);
      registry.add("spring.cloud.aws.s3.path-style-access-enabled", () -> "true");
      registry.add("spring.cloud.aws.credentials.access-key", () -> "foo");
      registry.add("spring.cloud.aws.credentials.secret-key", () -> "bar");
    };
  }

}
