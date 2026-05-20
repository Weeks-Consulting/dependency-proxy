package us.weeksconsulting.dependency_proxy.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanRegistrar;
import org.springframework.beans.factory.BeanRegistry;
import org.springframework.core.env.Environment;

import io.awspring.cloud.s3.S3Template;
import us.weeksconsulting.dependency_proxy.dao.RepositoryCacheDao;
import us.weeksconsulting.dependency_proxy.exception.UnknownStorageTypeException;
import us.weeksconsulting.dependency_proxy.service.CacheService;
import us.weeksconsulting.dependency_proxy.service.FileCacheService;
import us.weeksconsulting.dependency_proxy.service.S3CacheService;

public class SpringBeanRegistrar implements BeanRegistrar {
  private static final Logger LOGGER = LoggerFactory.getLogger(SpringBeanRegistrar.class);

  @Override
  public void register(BeanRegistry registry, Environment env) {
    String storageType = env.getProperty("application.storage.type");

    LOGGER.trace("Registering Cache Service Beans for storageType: {}", storageType);
    switch (storageType) {
      case "local" ->
        registry.registerBean("bar", CacheService.class, spec -> spec
            .prototype()
            .lazyInit()
            .supplier(context -> new FileCacheService(context.bean(ApplicationConfig.class),
                context.bean(RepositoryCacheDao.class))));
      case "s3" ->
        registry.registerBean("bar", CacheService.class, spec -> spec
            .prototype()
            .lazyInit()
            .supplier(context -> new S3CacheService(context.bean(ApplicationConfig.class),
                context.bean(RepositoryCacheDao.class),
                context.bean(S3Template.class))));
      default -> throw new UnknownStorageTypeException(storageType);
    }
  }

}
