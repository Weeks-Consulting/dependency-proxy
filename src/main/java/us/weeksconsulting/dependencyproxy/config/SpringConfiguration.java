package us.weeksconsulting.dependencyproxy.config;

import java.util.concurrent.Executor;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@ConfigurationPropertiesScan("us.weeksconsulting.dependencyproxy.config")
@EnableAsync
public class SpringConfiguration {

  @Bean
  public Executor cacheBackgroundTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(2);
    executor.setQueueCapacity(500);
    executor.setThreadNamePrefix("cacheBackgroundTask-");
    executor.initialize();
    return executor;
  }

  @Bean
  public TomcatServletWebServerFactory tomcatServletWebServerFactory() {
    TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
    factory.addConnectorCustomizers(connector -> connector.setAsyncTimeout(3600000)); // Set async timeout to 1 hour
    return factory;
  }

}
