package com.bachratus.demo.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class DemoEventController {

    private final DemoEventProducer producer;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public DemoEvent publish(@RequestBody PublishDemoEventRequest request) {
        return producer.publish(request.message());
    }

    public record PublishDemoEventRequest(String message) {
    }
}
