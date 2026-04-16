package us.weeksconsulting.dependency_proxy.controller;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

import java.io.InputStream;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import us.weeksconsulting.dependency_proxy.config.ApplicationConfig;
import us.weeksconsulting.dependency_proxy.manager.CacheManager;
import us.weeksconsulting.dependency_proxy.config.Repository;

@Controller
public class ProxyRoutingController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyRoutingController.class);

    private final ApplicationConfig applicationConfig;
    private final CacheManager cacheManager;

    public ProxyRoutingController(ApplicationConfig applicationConfig, CacheManager cacheManager) {
        this.applicationConfig = applicationConfig;
        this.cacheManager = cacheManager;
    }

    @GetMapping("/{repositoryType}/{repositoryName}/{*urlPath}")
    public ResponseEntity<StreamingResponseBody> getRequest(
            @PathVariable String repositoryType,
            @PathVariable String repositoryName,
            @PathVariable String urlPath,
            @RequestParam(required = false) Map<String, String> urlParams) {
        LOGGER.trace("repositoryType: {}", repositoryType);
        LOGGER.trace("repositoryName: {}", repositoryName);
        LOGGER.trace("urlPath: {}", urlPath);
        LOGGER.trace("urlParams: {}", urlParams);

        LOGGER.info("applicationConfig: {}", applicationConfig);

        Repository repository = applicationConfig.getRepositories().get(repositoryType).get(repositoryName);

        String baseUrl = repository.getBaseUrl();

        InputStream cachedInputStream = cacheManager.getOrCache(repositoryType, repositoryName, urlPath, urlParams); 

        LOGGER.info("Returning RestClient");
        return RestClient.create().get().uri(baseUrl + urlPath).exchange((request, response) -> {
            LOGGER.info("Request URI: {}", request.getURI());
            LOGGER.info("Request Headers: {}", request.getHeaders());

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.addAll(CONTENT_TYPE, response.getHeaders().get(CONTENT_TYPE));

            LOGGER.info("Returning ResponseEntity");
            return ResponseEntity.ok()
                    .headers(responseHeaders)
                    .body(outputStream -> response.getBody().transferTo(outputStream));
        }, false);

    }
}
