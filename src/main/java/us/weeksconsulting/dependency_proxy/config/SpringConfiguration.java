package us.weeksconsulting.dependency_proxy.config;

import org.apache.tomcat.util.buf.EncodedSolidusHandling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@ConfigurationPropertiesScan("us.weeksconsulting.dependency_proxy.config")

@ImportRuntimeHints(SpringRuntimeHintsRegistrar.class)
@Import(SpringBeanRegistrar.class)

@EnableAsync
@EnableScheduling
public class SpringConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(SpringConfiguration.class);

  @Bean
  public WebServerFactoryCustomizer<TomcatServletWebServerFactory> serverCustomizer() {
    LOGGER.trace("Returning Custom WebServerFactoryCustomizer");
    return factory -> factory
        .addConnectorCustomizers(connector -> {
          connector.setEncodedSolidusHandling(EncodedSolidusHandling.PASS_THROUGH.getValue());
          connector.setAsyncTimeout(300000);
        });
  }

}
