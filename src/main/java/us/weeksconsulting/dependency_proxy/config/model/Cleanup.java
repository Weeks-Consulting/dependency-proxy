package us.weeksconsulting.dependency_proxy.config.model;

import java.time.Duration;

public record Cleanup(Duration schedule, Duration lockTimeout) {
}