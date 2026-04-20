package us.weeksconsulting.dependencyproxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("us.weeksconsulting.dependencyproxy.config")
public class DependencyProxyApplication {

	public static void main(String[] args) {
		SpringApplication.run(DependencyProxyApplication.class, args);
	}

}
