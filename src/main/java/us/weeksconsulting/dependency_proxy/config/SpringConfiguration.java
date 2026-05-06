package us.weeksconsulting.dependency_proxy.config;

import org.eclipse.jetty.http.UriCompliance;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.ServerConnector;
// import org.apache.tomcat.util.buf.EncodedSolidusHandling;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.jetty.servlet.JettyServletWebServerFactory;
// import org.springframework.boot.tomcat.TomcatConnectorCustomizer;
// import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@ConfigurationPropertiesScan("us.weeksconsulting.dependency_proxy.config")

@EnableAsync
@EnableScheduling
public class SpringConfiguration {

  @Bean
  public JettyServletWebServerFactory jettyFactory() {
    JettyServletWebServerFactory factory = new JettyServletWebServerFactory();
    factory.addServerCustomizers(server -> {
      for (Connector connector : server.getConnectors()) {
        if (connector instanceof org.eclipse.jetty.server.ServerConnector) {
          HttpConnectionFactory httpConnectionFactory = ((ServerConnector) connector)
              .getConnectionFactory(HttpConnectionFactory.class);

          // Allow encoded slashes in the URI compliance settings
          httpConnectionFactory.getHttpConfiguration()
              .setUriCompliance(UriCompliance.from("RFC3986,AMBIGUOUS_PATH_SEPARATOR"));
        }
      }
    });
    return factory;
  }

  // @Bean
  // public TomcatConnectorCustomizer connectorCustomizer(
  // @Value("${spring.mvc.async.request-timeout}") int asyncTimeout) {
  // return (connector) -> {
  // connector.setEncodedSolidusHandling(EncodedSolidusHandling.DECODE.getValue());
  // connector.setAsyncTimeout(asyncTimeout);
  // };
  // }

}
