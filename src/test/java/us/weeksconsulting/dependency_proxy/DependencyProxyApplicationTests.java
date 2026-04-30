package us.weeksconsulting.dependency_proxy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles({ "test", "local" })
@Import(TestcontainersConfiguration.class)
class DependencyProxyApplicationTests {

  @Test
  void contextLoads() {
    // Not implemented yet
  }

}
