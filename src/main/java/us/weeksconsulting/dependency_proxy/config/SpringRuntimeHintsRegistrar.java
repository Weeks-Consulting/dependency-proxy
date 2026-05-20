package us.weeksconsulting.dependency_proxy.config;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

import io.awspring.cloud.s3.S3Template;
import us.weeksconsulting.dependency_proxy.dao.RepositoryCacheDao;
import us.weeksconsulting.dependency_proxy.service.FileCacheService;
import us.weeksconsulting.dependency_proxy.service.S3CacheService;

public class SpringRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

  private static final Logger LOGGER = LoggerFactory.getLogger(SpringRuntimeHintsRegistrar.class);

  @Override
  public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
    try {
      ProxyFactory fileCacheServiceFactory = new ProxyFactory();
      fileCacheServiceFactory.setTargetClass(FileCacheService.class);
      fileCacheServiceFactory.setProxyTargetClass(true);

      hints.reflection()
          .registerConstructor(
              FileCacheService.class.getConstructor(ApplicationConfig.class, RepositoryCacheDao.class),
              ExecutableMode.INVOKE)
          .registerType(FileCacheService.class,
              MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
              MemberCategory.INVOKE_PUBLIC_METHODS,
              MemberCategory.INVOKE_DECLARED_METHODS)
          .registerType(fileCacheServiceFactory.getProxyClass(classLoader),
              MemberCategory.ACCESS_DECLARED_FIELDS,
              MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
              MemberCategory.INVOKE_DECLARED_METHODS);
    } catch (NoSuchMethodException ex) {
      LOGGER.error("Error Registering Hints for FileCacheService", ex);
    }

    try {
      ProxyFactory s3CacheServiceFactory = new ProxyFactory();
      s3CacheServiceFactory.setTargetClass(S3CacheService.class);
      s3CacheServiceFactory.setProxyTargetClass(true);

      hints.reflection()
          .registerConstructor(
              S3CacheService.class.getConstructor(ApplicationConfig.class, RepositoryCacheDao.class, S3Template.class),
              ExecutableMode.INVOKE)
          .registerType(S3CacheService.class,
              MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
              MemberCategory.INVOKE_PUBLIC_METHODS,
              MemberCategory.INVOKE_DECLARED_METHODS)
          .registerType(s3CacheServiceFactory.getProxyClass(classLoader),
              MemberCategory.ACCESS_DECLARED_FIELDS,
              MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
              MemberCategory.INVOKE_DECLARED_METHODS);
    } catch (NoSuchMethodException ex) {
      LOGGER.error("Error Registering Hints for S3CacheService", ex);
    }
  }
}
