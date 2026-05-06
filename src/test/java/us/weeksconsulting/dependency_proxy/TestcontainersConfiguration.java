package us.weeksconsulting.dependency_proxy;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
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
@ConditionalOnExpression("'${CI:false}' == 'false'") // Check to see if we're running local or in CI/CD
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
      registry.add("PROXY_DB_HOST", postgresContainer::getHost);
      registry.add("PROXY_DB_PORT", postgresContainer::getFirstMappedPort);
      registry.add("PROXY_DB_DATABASE", postgresContainer::getDatabaseName);
      registry.add("PROXY_DB_USERNAME", postgresContainer::getUsername);
      registry.add("PROXY_DB_PASSWORD", postgresContainer::getPassword);
      registry.add("PROXY_DB_POOL_SIZE", () -> 5);
    };
  }

  @Bean
  @Profile("s3")
  S3MockContainer s3MockContainer() {
    S3MockContainer s3Container = new S3MockContainer("5.0.0");
    s3Container.withEnv("COM_ADOBE_TESTING_S3MOCK_STORE_INITIAL_BUCKETS", "test-bucket");
    s3Container.withEnv("SPRING_PROFILES_ACTIVE", "debug");
    return s3Container;
  }

  @Bean
  @Profile("s3")
  DynamicPropertyRegistrar s3PropertiesRegistrar(S3MockContainer s3MockContainer) {
    return registry -> {
      registry.add("PROXY_S3_ENDPOINT", s3MockContainer::getHttpEndpoint);
      registry.add("PROXY_S3_PATH_STYLE_ACCESS_ENABLED", () -> "true");
      registry.add("PROXY_S3_ACCESS_KEY", () -> "foo");
      registry.add("PROXY_S3_SECRET_KEY", () -> "bar");
    };
  }

}
