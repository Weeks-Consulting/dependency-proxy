package us.weeksconsulting.dependency_proxy.config.model;

import org.springframework.boot.context.properties.bind.DefaultValue;

public record Storage(
    String type,
    String path,
    String bucket,
    @DefaultValue String prefix) {
}
