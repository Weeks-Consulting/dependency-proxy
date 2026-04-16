package us.weeksconsulting.dependency_proxy.controller;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import us.weeksconsulting.dependency_proxy.config.RespositoryConfigs;

@Controller
public class ProxyRoutingController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyRoutingController.class);

    final RespositoryConfigs repositoryConfigs;

    public ProxyRoutingController(RespositoryConfigs repositoryConfigs) {
        this.repositoryConfigs = repositoryConfigs;
    }

    @GetMapping("/{repository}/{*path}")
    public ResponseEntity<StreamingResponseBody> getRequest(@PathVariable String repository,
            @PathVariable String path) {
        LOGGER.info("repository: {}", repository);
        LOGGER.info("path: {}", path);

        LOGGER.info("repositoryConfigs: {}", repositoryConfigs);

        String baseUrl = "https://archive.apache.org";

        LOGGER.info("Returning RestClient");
        return RestClient.create().get().uri(baseUrl + path).exchange((request, response) -> {
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
