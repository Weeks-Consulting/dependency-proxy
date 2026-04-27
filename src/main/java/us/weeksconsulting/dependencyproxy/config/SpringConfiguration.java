package us.weeksconsulting.dependencyproxy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@ConfigurationPropertiesScan("us.weeksconsulting.dependencyproxy.config")

@EnableAsync
@EnableScheduling
public class SpringConfiguration {

  @Bean
  public TomcatServletWebServerFactory tomcatServletWebServerFactory(
      @Value("${spring.mvc.async.request-timeout}") int asyncTimeout) {
    TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
    factory.addConnectorCustomizers(connector -> connector.setAsyncTimeout(asyncTimeout));
    return factory;
  }

}
