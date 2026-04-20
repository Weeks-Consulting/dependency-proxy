package us.weeksconsulting.dependencyproxy.controller;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import us.weeksconsulting.dependencyproxy.manager.CacheManager;

@Controller
public class ProxyRoutingController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyRoutingController.class);

    private final CacheManager cacheManager;

    public ProxyRoutingController(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @GetMapping("/{repositoryType}/{repositoryName}/{*urlPath}")
    public ResponseEntity<StreamingResponseBody> getRequest(
            @PathVariable String repositoryType,
            @PathVariable String repositoryName,
            @PathVariable String urlPath,
            @RequestParam(required = false) Map<String, String> urlParams) throws IOException {
        LOGGER.trace("repositoryType: {}", repositoryType);
        LOGGER.trace("repositoryName: {}", repositoryName);
        LOGGER.trace("urlPath: {}", urlPath);
        LOGGER.trace("urlParams: {}", urlParams);

        return cacheManager.getOrCache(repositoryType, repositoryName, urlPath, urlParams);
    }
}
