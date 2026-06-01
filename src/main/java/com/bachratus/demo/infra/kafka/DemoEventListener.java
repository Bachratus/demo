package com.bachratus.demo.infra.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.mapping.AbstractJavaTypeMapper;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DemoEventListener {

    @KafkaListener(topics = "${demo.kafka.topics.events}")
    public void listen(
            DemoEvent event,
            @Header(AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME) String eventType
    ) {
        log.info("Consumed Kafka event type: {}, aggregate: {}, event: {}", eventType, event.aggregateId(), event);
    }
}
