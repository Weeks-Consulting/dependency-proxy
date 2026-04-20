package us.weeksconsulting.dependencyproxy;

import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @RestartScope
    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        PostgreSQLContainer postgresContainer = new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));
        postgresContainer.withCreateContainerCmdModifier(cmd -> cmd.withHostConfig(
                new HostConfig()
                        .withPortBindings(new PortBinding(Ports.Binding.bindPort(55432), new ExposedPort(5432)))));

        return postgresContainer;
    }

}
