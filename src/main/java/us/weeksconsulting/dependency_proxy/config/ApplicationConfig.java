package us.weeksconsulting.dependency_proxy.config;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import us.weeksconsulting.dependency_proxy.config.model.Cleanup;
import us.weeksconsulting.dependency_proxy.config.model.Repository;
import us.weeksconsulting.dependency_proxy.config.model.Storage;

@ConfigurationProperties(prefix = "application")
public record ApplicationConfig(
    @NestedConfigurationProperty Cleanup cleanup,
    @NestedConfigurationProperty Storage storage,
    @NestedConfigurationProperty Map<String,Map<String,Repository>> repositories) {
}
