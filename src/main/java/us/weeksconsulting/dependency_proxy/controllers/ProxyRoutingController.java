package us.weeksconsulting.dependency_proxy.controllers;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

import java.io.BufferedInputStream;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Controller
public class ProxyRoutingController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyRoutingController.class);

    @GetMapping("/{repository}/**")
    public ResponseEntity<StreamingResponseBody> getRequest(@PathVariable String repository) {
        String url = "https://archive.apache.org/dist/nifi/2.9.0/minifi-toolkit-2.9.0-bin.zip";

        LOGGER.info("Returning RestClient");
        return RestClient.create().get().uri(url).exchange((request, response) -> {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.addAll(CONTENT_TYPE, response.getHeaders().get(CONTENT_TYPE));

            LOGGER.info("Returning ResponseEntity");
            return ResponseEntity.ok()
                    .headers(responseHeaders)
                    .body(outputStream -> response.getBody().transferTo(outputStream));
        }, false);

    }
}
