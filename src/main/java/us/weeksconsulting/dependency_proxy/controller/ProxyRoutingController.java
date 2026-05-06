package us.weeksconsulting.dependency_proxy.controller;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import jakarta.servlet.http.HttpServletRequest;
import us.weeksconsulting.dependency_proxy.manager.CacheManager;

@Controller
@RequestMapping("/proxy")
public class ProxyRoutingController {
  private static final Logger LOGGER = LoggerFactory.getLogger(ProxyRoutingController.class);

  private final CacheManager cacheManager;

  public ProxyRoutingController(CacheManager cacheManager) {
    this.cacheManager = cacheManager;
  }

  @GetMapping("/{repositoryType}/{repositoryName}/**")
  public ResponseEntity<StreamingResponseBody> getProxyRequest(
      @PathVariable String repositoryType,
      @PathVariable String repositoryName,
      HttpServletRequest proxyRequest) throws IOException {

    String requestUrl = getRequestUrl("raw", repositoryName, proxyRequest);

    LOGGER.trace(
        "getProxyRequest -> repositoryType: {}, repositoryName: {}, requestUrl: {}",
        repositoryType,
        repositoryName,
        requestUrl);

    return cacheManager.get(repositoryType, repositoryName, requestUrl);
  }

  private String getRequestUrl(
      String repositoryType,
      String repositoryName,
      HttpServletRequest request) {

    String requestUri = request.getRequestURI();
    String queryString = request.getQueryString();
    String baseMapping = "/" + repositoryType + "/" + repositoryName;
    return requestUri.substring(requestUri.indexOf(baseMapping) + baseMapping.length())
        + (queryString != null ? "?" + queryString : "");
  }
}
