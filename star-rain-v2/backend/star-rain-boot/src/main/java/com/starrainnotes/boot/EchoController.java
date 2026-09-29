package com.starrainnotes.boot;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2")
public class EchoController {

    @PostMapping("/echo")
    public EchoResponse echo(@RequestBody EchoRequest request) {
        return new EchoResponse(request.message(), "2.0.0");
    }

    public record EchoRequest(String message) {
    }

    public record EchoResponse(String message, String version) {
    }
}

