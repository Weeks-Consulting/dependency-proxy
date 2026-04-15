package us.weeksconsulting.dependency_proxy.controllers;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
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

        RestClient defaultClient = RestClient.create();

        return defaultClient.get().uri(url).exchange((request, response) -> {
            LOGGER.info("HTTP Status {}", response.getStatusCode());
            LOGGER.info("HTTP Headers -> {}", response.getHeaders());
            try (InputStream inputStream = response.getBody();) {

                StreamingResponseBody stream = outputStream -> {
                    StreamUtils.copy(inputStream, outputStream);
                };
                
                return ResponseEntity.ok()
                        .headers(response.getHeaders())
                        .body(stream);
            }
        });
    }
}
