package tools.vitruv.framework.remote.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.propagation")
public record PropagationProperties(
    int corePoolSize,
    int maxPoolSize,
    int queueCapacity,
    int taskTtlHours
) {
  public PropagationProperties {
    if (corePoolSize <= 0) {
      corePoolSize = 2;
    }
    if (maxPoolSize <= 0) {
      maxPoolSize = 4;
    }
    if (queueCapacity <= 0) {
      queueCapacity = 50;
    }
    if (taskTtlHours <= 0) {
      taskTtlHours = 24;
    }
  }
}
