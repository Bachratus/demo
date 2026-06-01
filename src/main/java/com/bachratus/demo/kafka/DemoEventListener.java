package com.bachratus.demo.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DemoEventListener {

    @KafkaListener(topics = "${demo.kafka.topics.events}")
    public void listen(DemoEvent event) {
        log.info("Consumed demo event: {}", event);
    }
}
