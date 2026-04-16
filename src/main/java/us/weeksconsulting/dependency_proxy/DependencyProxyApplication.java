package us.weeksconsulting.dependency_proxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("us.weeksconsulting.dependency_proxy.config")
public class DependencyProxyApplication {

	public static void main(String[] args) {
		SpringApplication.run(DependencyProxyApplication.class, args);
	}

}
