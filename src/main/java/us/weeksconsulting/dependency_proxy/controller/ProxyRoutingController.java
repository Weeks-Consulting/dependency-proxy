package us.weeksconsulting.dependency_proxy.controller;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import us.weeksconsulting.dependency_proxy.manager.CacheManager;

@Controller
public class ProxyRoutingController {
  private static final Logger LOGGER = LoggerFactory.getLogger(ProxyRoutingController.class);

  private final CacheManager cacheManager;

  public ProxyRoutingController(CacheManager cacheManager) {
    this.cacheManager = cacheManager;
  }

  @GetMapping("/raw/{repositoryName}/{*urlPath}")
  public ResponseEntity<StreamingResponseBody> getRawRequest(
      @PathVariable String repositoryName,
      @PathVariable String urlPath,
      @RequestParam(required = false) MultiValueMap<String, String> urlParams) throws IOException {
    LOGGER.trace("getRawRequest -> repositoryName: {}, urlPath: {}, urlParams: {}", repositoryName, urlPath, urlParams);

    return cacheManager.get("raw", repositoryName, urlPath, urlParams);
  }
}
